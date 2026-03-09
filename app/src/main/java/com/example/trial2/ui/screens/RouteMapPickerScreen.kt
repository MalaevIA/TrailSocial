package com.trail2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import com.trail2.R
import com.trail2.ui.theme.ForestGreen
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteMapPickerScreen(
    onBack: () -> Unit,
    onRouteSelected: (
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double,
        geometry: List<List<Double>>,
        distanceKm: Double
    ) -> Unit
) {
    val context = LocalContext.current
    // Mutable list of route points (MapKit Point: lat, lng)
    var points by remember { mutableStateOf(listOf<Point>()) }
    var distanceKm by remember { mutableDoubleStateOf(0.0) }
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        mapView.mapWindow.map.move(
            CameraPosition(Point(55.751244, 37.618423), 10f, 0f, 0f)
        )
        onDispose {
            mapView.onStop()
            MapKitFactory.getInstance().onStop()
        }
    }

    // Map tap listener — adds a point on each tap
    DisposableEffect(Unit) {
        val listener = object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                val newPoints = points + point
                points = newPoints
                distanceKm = totalDistanceKm(newPoints)
                redrawMap(mapView, newPoints)
            }

            override fun onMapLongTap(map: Map, point: Point) {}
        }
        mapView.mapWindow.map.addInputListener(listener)
        onDispose {
            mapView.mapWindow.map.removeInputListener(listener)
        }
    }

    val pointCount = points.size
    val canFinish = pointCount >= 2

    val bannerText = when {
        pointCount == 0 -> stringResource(R.string.map_picker_hint_first)
        pointCount == 1 -> stringResource(R.string.map_picker_hint_more)
        else -> stringResource(R.string.map_picker_info, pointCount, distanceKm)
    }

    val bannerColor = when {
        pointCount < 2 -> MaterialTheme.colorScheme.primaryContainer
        else -> Color(0xFFD8F3DC)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_picker_title), fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    // Undo last point
                    IconButton(
                        onClick = {
                            if (points.isNotEmpty()) {
                                val newPoints = points.dropLast(1)
                                points = newPoints
                                distanceKm = totalDistanceKm(newPoints)
                                redrawMap(mapView, newPoints)
                            }
                        },
                        enabled = points.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Undo, stringResource(R.string.map_picker_undo))
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    // Distance info
                    if (canFinish) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.map_picker_distance), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${"%.2f".format(distanceKm)} км", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.map_picker_points), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$pointCount", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                points = emptyList()
                                distanceKm = 0.0
                                mapView.mapWindow.map.mapObjects.clear()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = points.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.clear))
                        }
                        Button(
                            onClick = {
                                val first = points.first()
                                val last = points.last()
                                val geometry = points.map { p -> listOf(p.longitude, p.latitude) }
                                onRouteSelected(
                                    first.latitude, first.longitude,
                                    last.latitude, last.longitude,
                                    geometry, distanceKm
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = canFinish,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                        ) {
                            Text(stringResource(R.string.done))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // Banner hint
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                color = bannerColor,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = bannerText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ── Map drawing ──────────────────────────────────────────

private fun redrawMap(mapView: MapView, points: List<Point>) {
    val map = mapView.mapWindow.map
    map.mapObjects.clear()

    if (points.isEmpty()) return

    // Polyline
    if (points.size >= 2) {
        val polylineObj = map.mapObjects.addPolyline(Polyline(points))
        polylineObj.apply {
            strokeWidth = 5f
            setStrokeColor(android.graphics.Color.parseColor("#2D6A4F"))
            outlineColor = android.graphics.Color.WHITE
            outlineWidth = 2f
        }
    }

    // Markers
    points.forEachIndexed { idx, point ->
        val isFirst = idx == 0
        val isLast = idx == points.lastIndex && points.size > 1

        val bitmap = when {
            isFirst -> createPickerMarkerBitmap(
                android.graphics.Color.parseColor("#52B788"), 64, "S"
            )
            isLast -> createPickerMarkerBitmap(
                android.graphics.Color.parseColor("#E63946"), 64, "F"
            )
            else -> createSmallDotBitmap(
                android.graphics.Color.parseColor("#457B9D")
            )
        }
        val placemark = map.mapObjects.addPlacemark(point)
        placemark.setIcon(ImageProvider.fromBitmap(bitmap))
    }
}

private fun createPickerMarkerBitmap(color: Int, size: Int, label: String): android.graphics.Bitmap {
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

    // Circle
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

    // White border
    paint.style = android.graphics.Paint.Style.STROKE
    paint.color = android.graphics.Color.WHITE
    paint.strokeWidth = 2f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

    return bitmap
}

// ── Haversine distance ───────────────────────────────────

private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return r * 2 * asin(sqrt(a))
}

private fun totalDistanceKm(points: List<Point>): Double {
    return points.zipWithNext().sumOf { (a, b) ->
        haversineKm(a.latitude, a.longitude, b.latitude, b.longitude)
    }
}
