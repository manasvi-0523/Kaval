package com.kaval.app.presentation.screens

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kaval.app.domain.model.KavalLocation
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

private const val USER_SOURCE_ID = "kaval-user-location-source"
private const val USER_LAYER_ID = "kaval-user-location-layer"

@Composable
internal fun MapLibreLocationMap(
    location: KavalLocation?,
    mapTilerKey: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember(context) { MapView(context).apply { onCreate(Bundle()) } }
    val center = location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(12.9716, 77.5946)
    val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=$mapTilerKey"

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.getMapAsync { map ->
                    map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                        if (location != null) {
                            val point = Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude))
                            val source = style.getSourceAs<GeoJsonSource>(USER_SOURCE_ID)
                            if (source == null) {
                                style.addSource(GeoJsonSource(USER_SOURCE_ID, point))
                                style.addLayer(
                                    CircleLayer(USER_LAYER_ID, USER_SOURCE_ID).withProperties(
                                        circleRadius(9f),
                                        circleColor(Color(0xFF1565C0).toArgb()),
                                        circleStrokeColor(Color.White.toArgb()),
                                        circleStrokeWidth(3f)
                                    )
                                )
                            } else {
                                source.setGeoJson(point)
                            }
                        }
                        map.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder().target(center).zoom(if (location == null) 11.0 else 15.5).build()
                            ),
                            700
                        )
                    }
                }
            }
        )
    }
}
