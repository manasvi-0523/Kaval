const POLL_INTERVAL_MS = 10000;
const STALE_AFTER_MS = 120000;

let config;
let token;
let map;
let marker;
let accuracySourceReady = false;
let lastSession;

const elements = {
  personName: document.querySelector("#person-name"),
  liveState: document.querySelector("#live-state"),
  mapMessage: document.querySelector("#map-message"),
  updatedAge: document.querySelector("#updated-age"),
  accuracy: document.querySelector("#accuracy"),
  sessionStatus: document.querySelector("#session-status"),
  staleWarning: document.querySelector("#stale-warning"),
  fatalMessage: document.querySelector("#fatal-message"),
  refreshButton: document.querySelector("#refresh-button")
};

function sessionTokenFromPath() {
  const parts = window.location.pathname.split("/").filter(Boolean);
  return parts[0] === "track" && parts[1] ? parts[1] : "";
}

async function loadConfig() {
  const response = await fetch("/api/config", { cache: "no-store" });
  if (!response.ok) throw new Error("Tracking configuration is unavailable.");
  const loaded = await response.json();
  if (!loaded.supabaseUrl || !loaded.supabaseAnonKey || !loaded.mapTilerKey) {
    throw new Error("Tracking configuration is incomplete.");
  }
  return loaded;
}

async function callRpc(functionName, body) {
  const response = await fetch(
    `${config.supabaseUrl}/rest/v1/rpc/${functionName}`,
    {
      method: "POST",
      headers: {
        apikey: config.supabaseAnonKey,
        Authorization: `Bearer ${config.supabaseAnonKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(body),
      cache: "no-store"
    }
  );
  if (!response.ok) throw new Error("The live session could not be read.");
  return response.json();
}

function initMap() {
  map = new maplibregl.Map({
    container: "map",
    style: `https://api.maptiler.com/maps/streets-v2/style.json?key=${encodeURIComponent(config.mapTilerKey)}`,
    center: [77.5946, 12.9716],
    zoom: 11,
    attributionControl: true
  });

  map.on("load", () => {
    map.addSource("accuracy", {
      type: "geojson",
      data: emptyFeatureCollection()
    });
    map.addLayer({
      id: "accuracy-fill",
      type: "fill",
      source: "accuracy",
      paint: {
        "fill-color": "#318fce",
        "fill-opacity": 0.16
      }
    });
    accuracySourceReady = true;
    if (lastSession) updateMap(lastSession);
  });
}

function emptyFeatureCollection() {
  return { type: "FeatureCollection", features: [] };
}

function accuracyCircle(longitude, latitude, radiusMeters) {
  const points = 64;
  const coordinates = [];
  const earthRadius = 6378137;
  const latRadians = latitude * Math.PI / 180;

  for (let index = 0; index <= points; index += 1) {
    const angle = index * 2 * Math.PI / points;
    const dx = radiusMeters * Math.cos(angle);
    const dy = radiusMeters * Math.sin(angle);
    coordinates.push([
      longitude + dx / (earthRadius * Math.cos(latRadians)) * 180 / Math.PI,
      latitude + dy / earthRadius * 180 / Math.PI
    ]);
  }
  return {
    type: "FeatureCollection",
    features: [{
      type: "Feature",
      geometry: { type: "Polygon", coordinates: [coordinates] },
      properties: {}
    }]
  };
}

function updateMap(session) {
  if (!map || !Number.isFinite(session.last_lng) || !Number.isFinite(session.last_lat)) return;
  const position = [session.last_lng, session.last_lat];
  if (!marker) {
    marker = new maplibregl.Marker({ color: "#318fce" }).setLngLat(position).addTo(map);
  } else {
    marker.setLngLat(position);
  }
  map.easeTo({ center: position, zoom: 16, duration: 700 });
  if (accuracySourceReady) {
    map.getSource("accuracy").setData(
      accuracyCircle(
        session.last_lng,
        session.last_lat,
        Math.max(session.last_accuracy_meters || 10, 5)
      )
    );
  }
}

function formatAge(timestamp) {
  if (!timestamp) return "Not available";
  const seconds = Math.max(0, Math.floor((Date.now() - new Date(timestamp).getTime()) / 1000));
  if (seconds < 60) return `${seconds}s ago`;
  return `${Math.floor(seconds / 60)}m ago`;
}

function render(session) {
  lastSession = session;
  elements.personName.textContent = `${session.display_name}'s live location`;
  elements.sessionStatus.textContent = session.status;
  elements.updatedAge.textContent = formatAge(session.last_updated_at);
  elements.accuracy.textContent = session.last_accuracy_meters
    ? `Within ${session.last_accuracy_meters} m`
    : "Not available";

  const age = session.last_updated_at
    ? Date.now() - new Date(session.last_updated_at).getTime()
    : Number.POSITIVE_INFINITY;
  const stale = age > STALE_AFTER_MS;
  elements.liveState.textContent = stale ? "Stale" : "Live";
  elements.liveState.className = `state ${stale ? "state-stale" : "state-live"}`;
  elements.staleWarning.hidden = !stale;
  elements.staleWarning.textContent = stale
    ? `Location has not updated recently. Last known position shown (${formatAge(session.last_updated_at)}).`
    : "";
  elements.mapMessage.hidden = Number.isFinite(session.last_lat) && Number.isFinite(session.last_lng);
  updateMap(session);
}

async function refreshSession() {
  try {
    const rows = await callRpc("guardian_session_by_token", { input_token: token });
    if (!Array.isArray(rows) || rows.length === 0) {
      showFatal("This tracking link has expired or is no longer active.");
      return;
    }
    render(rows[0]);
  } catch (error) {
    showFatal(error.message);
  }
}

function showFatal(message) {
  elements.fatalMessage.hidden = false;
  elements.fatalMessage.textContent = message;
  elements.liveState.textContent = "Unavailable";
  elements.liveState.className = "state state-stale";
}

async function start() {
  token = sessionTokenFromPath();
  if (!token) {
    showFatal("This tracking link is invalid.");
    return;
  }
  try {
    config = await loadConfig();
    initMap();
    await callRpc("guardian_mark_viewed", { input_token: token });
    await refreshSession();
    window.setInterval(refreshSession, POLL_INTERVAL_MS);
    window.setInterval(
      () => callRpc("guardian_mark_viewed", { input_token: token }).catch(() => {}),
      300000
    );
  } catch (error) {
    showFatal(error.message);
  }
}

elements.refreshButton.addEventListener("click", refreshSession);
start();
