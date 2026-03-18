package com.trail2.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

/**
 * Reusable composable that displays a route on a Yandex Map with polyline,
 * start (green) / end (red) markers, and small dot markers for intermediate points.
 *
 * @param geometry GeoJSON coordinate pairs [lng, lat]
 */
@Composable
fun RouteMapView(
    geometry: List<List<Double>>,
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double,
    modifier: Modifier = Modifier,
    manageMapKitLifecycle: Boolean = true
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        if (manageMapKitLifecycle) MapKitFactory.getInstance().onStart()
        mapView.onStart()
        onDispose {
            mapView.onStop()
            if (manageMapKitLifecycle) MapKitFactory.getInstance().onStop()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            setupRouteMap(mv, geometry, startLat, startLng, endLat, endLng)
        }
    )
}

private fun setupRouteMap(
    mapView: MapView,
    geometry: List<List<Double>>,
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double
) {
    val map = mapView.mapWindow.map
    map.mapObjects.clear()

    // Convert GeoJSON [lng, lat] -> MapKit Point(lat, lng)
    val polylinePoints = if (geometry.size >= 2) {
        geometry.map { coord -> Point(coord[1], coord[0]) }
    } else {
        listOf(Point(startLat, startLng), Point(endLat, endLng))
    }

    // Draw polyline
    if (polylinePoints.size >= 2) {
        val polylineObj = map.mapObjects.addPolyline(Polyline(polylinePoints))
        polylineObj.apply {
            strokeWidth = 5f
            setStrokeColor(android.graphics.Color.parseColor("#2D6A4F"))
            outlineColor = android.graphics.Color.WHITE
            outlineWidth = 2f
        }
    }

    // Only Start and Finish markers (no intermediate dots)
    if (polylinePoints.isNotEmpty()) {
        val startBitmap = createCircleMarkerBitmap(
            android.graphics.Color.parseColor("#52B788"), 64, "S"
        )
        val startPlacemark = map.mapObjects.addPlacemark(polylinePoints.first())
        startPlacemark.setIcon(ImageProvider.fromBitmap(startBitmap))

        if (polylinePoints.size > 1) {
            val finishBitmap = createCircleMarkerBitmap(
                android.graphics.Color.parseColor("#E63946"), 64, "F"
            )
            val finishPlacemark = map.mapObjects.addPlacemark(polylinePoints.last())
            finishPlacemark.setIcon(ImageProvider.fromBitmap(finishBitmap))
        }
    }

    // Camera auto-fit to bounds
    val minLat = polylinePoints.minOf { it.latitude }
    val maxLat = polylinePoints.maxOf { it.latitude }
    val minLng = polylinePoints.minOf { it.longitude }
    val maxLng = polylinePoints.maxOf { it.longitude }

    if (minLat == maxLat && minLng == maxLng) {
        map.move(CameraPosition(Point(minLat, minLng), 14f, 0f, 0f))
    } else {
        val boundingBox = BoundingBox(Point(minLat, minLng), Point(maxLat, maxLng))
        val cameraPosition = map.cameraPosition(Geometry.fromBoundingBox(boundingBox))
        map.move(CameraPosition(cameraPosition.target, cameraPosition.zoom - 0.5f, 0f, 0f))
    }
}

private fun createCircleMarkerBitmap(color: Int, size: Int, label: String): android.graphics.Bitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    // Shadow
    paint.color = android.graphics.Color.parseColor("#33000000")
    canvas.drawCircle(size / 2f + 1, size / 2f + 1, size / 2f - 3, paint)

    // Circle
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3, paint)

    // White border
    paint.style = android.graphics.Paint.Style.STROKE
    paint.color = android.graphics.Color.WHITE
    paint.strokeWidth = 3f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3, paint)

    // Label
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 24f
    paint.textAlign = android.graphics.Paint.Align.CENTER
    val textY = size / 2f - (paint.descent() + paint.ascent()) / 2
    canvas.drawText(label, size / 2f, textY, paint)

    return bitmap
}

private fun createSmallDotBitmap(color: Int): android.graphics.Bitmap {
    val size = 28
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

    paint.style = android.graphics.Paint.Style.STROKE
    paint.color = android.graphics.Color.WHITE
    paint.strokeWidth = 2f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

    return bitmap
}
