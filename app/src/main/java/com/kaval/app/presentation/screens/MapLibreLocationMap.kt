package com.kaval.app.presentation.screens

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val USER_SOURCE_ID = "kaval-user-location-source"
private const val USER_LAYER_ID = "kaval-user-location-layer"
private const val ACCURACY_SOURCE_ID = "kaval-accuracy-source"
private const val ACCURACY_FILL_LAYER_ID = "kaval-accuracy-fill-layer"
private const val ACCURACY_STROKE_LAYER_ID = "kaval-accuracy-stroke-layer"

@Composable
internal fun MapLibreLocationMap(
    location: KavalLocation?,
    mapTilerKey: String,
    recenterSignal: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember(context) { MapView(context).apply { onCreate(Bundle()) } }
    val center = location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(20.5937, 78.9629)
    val recenterRequest = recenterSignal
    val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=$mapTilerKey"
    var styleLoaded by remember(mapView) { mutableStateOf(false) }

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
                    if (!styleLoaded) {
                        map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                            styleLoaded = true
                            renderLocation(style, location)
                            recenterMap(map, center, location, recenterRequest)
                        }
                    } else {
                        map.getStyle { style ->
                            renderLocation(style, location)
                            recenterMap(map, center, location, recenterRequest)
                        }
                    }
                }
            }
        )
    }
}

private fun renderLocation(style: Style, location: KavalLocation?) {
    if (location == null) return

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

    location.accuracyMeters?.takeIf { it > 0f }?.let { accuracyMeters ->
        val accuracyFeature = Feature.fromGeometry(location.toAccuracyPolygon(accuracyMeters))
        val accuracySource = style.getSourceAs<GeoJsonSource>(ACCURACY_SOURCE_ID)
        if (accuracySource == null) {
            style.addSource(GeoJsonSource(ACCURACY_SOURCE_ID, accuracyFeature))
            style.addLayerBelow(
                FillLayer(ACCURACY_FILL_LAYER_ID, ACCURACY_SOURCE_ID).withProperties(
                    fillColor(Color(0xFF1565C0).toArgb()),
                    fillOpacity(0.16f)
                ),
                USER_LAYER_ID
            )
            style.addLayerBelow(
                LineLayer(ACCURACY_STROKE_LAYER_ID, ACCURACY_SOURCE_ID).withProperties(
                    lineColor(Color(0xFF1565C0).toArgb()),
                    lineWidth(2f)
                ),
                USER_LAYER_ID
            )
        } else {
            accuracySource.setGeoJson(accuracyFeature)
        }
    }
}

private fun recenterMap(
    map: org.maplibre.android.maps.MapLibreMap,
    center: LatLng,
    location: KavalLocation?,
    recenterRequest: Int
) {
    map.animateCamera(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder().target(center).zoom(if (location == null) 4.0 else 16.0).build()
        ),
        if (recenterRequest == 0) 700 else 450
    )
}

private fun KavalLocation.toAccuracyPolygon(radiusMeters: Float): Polygon {
    val earthRadiusMeters = 6_371_000.0
    val latitudeRadians = latitude.toRadians()
    val longitudeRadians = longitude.toRadians()
    val angularDistance = radiusMeters / earthRadiusMeters
    val ring = (0..64).map { step ->
        val bearing = 2.0 * PI * step / 64.0
        val pointLatitude = kotlin.math.asin(
            sin(latitudeRadians) * cos(angularDistance) +
                cos(latitudeRadians) * sin(angularDistance) * cos(bearing)
        )
        val pointLongitude = longitudeRadians + kotlin.math.atan2(
            sin(bearing) * sin(angularDistance) * cos(latitudeRadians),
            cos(angularDistance) - sin(latitudeRadians) * sin(pointLatitude)
        )
        Point.fromLngLat(pointLongitude.toDegrees(), pointLatitude.toDegrees())
    }
    return Polygon.fromLngLats(listOf(ring))
}

private fun Double.toRadians(): Double = this * PI / 180.0

private fun Double.toDegrees(): Double = this * 180.0 / PI
