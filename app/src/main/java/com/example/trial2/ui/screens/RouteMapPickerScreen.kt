package com.trail2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.trail2.R
import com.trail2.ui.theme.ForestGreen
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.SuggestOptions
import com.yandex.mapkit.search.SuggestResponse
import com.yandex.mapkit.search.SuggestSession
import com.yandex.mapkit.search.SuggestType
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.PedestrianRouter
import com.yandex.mapkit.transport.masstransit.Route
import com.yandex.mapkit.transport.masstransit.FitnessOptions
import com.yandex.mapkit.transport.masstransit.RouteOptions
import com.yandex.mapkit.transport.masstransit.Session
import com.yandex.mapkit.transport.masstransit.TimeOptions
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import kotlin.math.*

data class WaypointEntry(
    val point: Point,
    val name: String = "",
    val description: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteMapPickerScreen(
    onBack: () -> Unit,
    onRouteSelected: (
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double,
        geometry: List<List<Double>>,
        distanceKm: Double,
        waypoints: List<WaypointEntry>
    ) -> Unit
) {
    val context = LocalContext.current
    var waypoints by remember { mutableStateOf(listOf<WaypointEntry>()) }
    val points by remember { derivedStateOf { waypoints.map { it.point } } }
    var distanceKm by remember { mutableDoubleStateOf(0.0) }
    var routeGeometry by remember { mutableStateOf<List<Point>>(emptyList()) }
    var isRoutingInProgress by remember { mutableStateOf(false) }
    val mapView = remember { MapView(context) }
    val routerSession = remember { mutableStateOf<Session?>(null) }
    val pedestrianRouter = remember { TransportFactory.getInstance().createPedestrianRouter() }

    // Search
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<SuggestItem>>(emptyList()) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    val searchManager = remember { SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED) }
    val suggestSession = remember { searchManager.createSuggestSession() }

    // Waypoint naming dialog
    var pendingPoint by remember { mutableStateOf<Point?>(null) }
    var pendingName by remember { mutableStateOf("") }
    var pendingDesc by remember { mutableStateOf("") }
    var pendingSearchTitle by remember { mutableStateOf<String?>(null) }

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

    // Suggest listener
    val suggestOptions = remember {
        SuggestOptions().setSuggestTypes(
            SuggestType.GEO.value or SuggestType.BIZ.value
        )
    }
    val boundingBox = remember {
        BoundingBox(Point(41.0, 19.0), Point(82.0, 180.0))
    }

    // Search when query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            suggestSession.suggest(
                searchQuery,
                boundingBox,
                suggestOptions,
                object : SuggestSession.SuggestListener {
                    override fun onResponse(response: SuggestResponse) {
                        suggestions = response.items.mapNotNull { item ->
                            val center = item.center
                            if (center != null) {
                                SuggestItem(
                                    title = item.title.text,
                                    subtitle = item.subtitle?.text ?: "",
                                    point = center
                                )
                            } else null
                        }
                    }

                    override fun onError(error: Error) {
                        suggestions = emptyList()
                    }
                }
            )
        } else {
            suggestions = emptyList()
        }
    }

    fun addWaypoint(point: Point, name: String, description: String) {
        val newWaypoints = waypoints + WaypointEntry(point, name, description)
        waypoints = newWaypoints
        val newPoints = newWaypoints.map { it.point }
        drawMarkers(mapView, newPoints)

        if (newPoints.size >= 2) {
            isRoutingInProgress = true
            requestPedestrianRoute(
                pedestrianRouter = pedestrianRouter,
                points = newPoints,
                mapView = mapView,
                routerSession = routerSession,
                onResult = { geometry, distance ->
                    routeGeometry = geometry
                    distanceKm = distance
                    isRoutingInProgress = false
                },
                onFallback = {
                    routeGeometry = newPoints
                    distanceKm = totalDistanceKm(newPoints)
                    drawFallbackPolyline(mapView, newPoints)
                    isRoutingInProgress = false
                }
            )
        } else {
            routeGeometry = emptyList()
            distanceKm = 0.0
        }

        // Move camera to the new point
        mapView.mapWindow.map.move(
            CameraPosition(point, 14f, 0f, 0f)
        )
    }

    // Map tap listener
    DisposableEffect(Unit) {
        val listener = object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                if (isRoutingInProgress) return
                pendingPoint = point
                pendingName = ""
                pendingDesc = ""
                pendingSearchTitle = null
            }

            override fun onMapLongTap(map: Map, point: Point) {}
        }
        mapView.mapWindow.map.addInputListener(listener)
        onDispose {
            mapView.mapWindow.map.removeInputListener(listener)
        }
    }

    val pointCount = points.size
    val canFinish = pointCount >= 2 && !isRoutingInProgress

    val bannerText = when {
        pointCount == 0 -> stringResource(R.string.map_picker_hint_first)
        pointCount == 1 -> stringResource(R.string.map_picker_hint_more)
        else -> stringResource(R.string.map_picker_info, pointCount, distanceKm)
    }

    val bannerColor = when {
        pointCount < 2 -> MaterialTheme.colorScheme.primaryContainer
        else -> Color(0xFFD8F3DC)
    }

    // Waypoint naming dialog
    if (pendingPoint != null) {
        AlertDialog(
            onDismissRequest = { pendingPoint = null },
            title = { Text(stringResource(R.string.map_picker_add_point_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pendingName,
                        onValueChange = { pendingName = it },
                        label = { Text(stringResource(R.string.map_picker_point_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pendingDesc,
                        onValueChange = { pendingDesc = it },
                        label = { Text(stringResource(R.string.map_picker_point_desc_hint)) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pt = pendingPoint!!
                    val name = pendingName.ifBlank { pendingSearchTitle ?: "Точка ${waypoints.size + 1}" }
                    addWaypoint(pt, name, pendingDesc)
                    pendingPoint = null
                    pendingSearchTitle = null
                }) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPoint = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
                    IconButton(
                        onClick = {
                            if (waypoints.isNotEmpty() && !isRoutingInProgress) {
                                val newWaypoints = waypoints.dropLast(1)
                                waypoints = newWaypoints
                                routeGeometry = emptyList()
                                val newPoints = newWaypoints.map { it.point }

                                if (newPoints.size >= 2) {
                                    isRoutingInProgress = true
                                    drawMarkers(mapView, newPoints)
                                    requestPedestrianRoute(
                                        pedestrianRouter = pedestrianRouter,
                                        points = newPoints,
                                        mapView = mapView,
                                        routerSession = routerSession,
                                        onResult = { geometry, distance ->
                                            routeGeometry = geometry
                                            distanceKm = distance
                                            isRoutingInProgress = false
                                        },
                                        onFallback = {
                                            routeGeometry = newPoints
                                            distanceKm = totalDistanceKm(newPoints)
                                            drawFallbackPolyline(mapView, newPoints)
                                            isRoutingInProgress = false
                                        }
                                    )
                                } else {
                                    distanceKm = 0.0
                                    val map = mapView.mapWindow.map
                                    map.mapObjects.clear()
                                    drawMarkers(mapView, newPoints)
                                }
                            }
                        },
                        enabled = waypoints.isNotEmpty() && !isRoutingInProgress
                    ) {
                        Icon(Icons.Filled.Undo, stringResource(R.string.map_picker_undo))
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
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
                    if (isRoutingInProgress) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                waypoints = emptyList()
                                distanceKm = 0.0
                                routeGeometry = emptyList()
                                mapView.mapWindow.map.mapObjects.clear()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = waypoints.isNotEmpty() && !isRoutingInProgress
                        ) {
                            Text(stringResource(R.string.clear))
                        }
                        Button(
                            onClick = {
                                val first = points.first()
                                val last = points.last()
                                val geometryToSend = if (routeGeometry.size >= 2) {
                                    routeGeometry.map { p -> listOf(p.longitude, p.latitude) }
                                } else {
                                    points.map { p -> listOf(p.longitude, p.latitude) }
                                }
                                onRouteSelected(
                                    first.latitude, first.longitude,
                                    last.latitude, last.longitude,
                                    geometryToSend, distanceKm,
                                    waypoints
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    isSearchExpanded = it.length >= 2
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.map_picker_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            isSearchExpanded = false
                            suggestions = emptyList()
                        }) {
                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                // Map
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )

                // Banner
                if (!isSearchExpanded) {
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

                // Suggestions overlay
                if (isSearchExpanded && suggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 250.dp)
                        ) {
                            itemsIndexed(suggestions) { _, item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            pendingPoint = item.point
                                            pendingName = item.title
                                            pendingDesc = ""
                                            pendingSearchTitle = item.title
                                            searchQuery = ""
                                            isSearchExpanded = false
                                            suggestions = emptyList()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    if (item.subtitle.isNotBlank()) {
                                        Text(item.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Waypoint list
            if (waypoints.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            stringResource(R.string.map_picker_waypoints),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                            itemsIndexed(waypoints) { idx, wp ->
                                WaypointListItem(
                                    waypoint = wp,
                                    index = idx,
                                    isFirst = idx == 0,
                                    isLast = idx == waypoints.lastIndex && waypoints.size > 1
                                )
                                if (idx < waypoints.lastIndex) {
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaypointListItem(
    waypoint: WaypointEntry,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean
) {
    val dotColor = when {
        isFirst -> Color(0xFF52B788)
        isLast -> Color(0xFFE63946)
        else -> Color(0xFF457B9D)
    }
    val icon = when {
        isFirst -> "S"
        isLast -> "F"
        else -> "${index + 1}"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(dotColor),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(waypoint.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            if (waypoint.description.isNotBlank()) {
                Text(
                    waypoint.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Text(
            "${"%.4f".format(waypoint.point.latitude)}, ${"%.4f".format(waypoint.point.longitude)}",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
        )
    }
}

private data class SuggestItem(
    val title: String,
    val subtitle: String,
    val point: Point
)

// ── Построение пешеходного маршрута ──────────────────────

private fun requestPedestrianRoute(
    pedestrianRouter: PedestrianRouter,
    points: List<Point>,
    mapView: MapView,
    routerSession: MutableState<Session?>,
    onResult: (geometry: List<Point>, distanceKm: Double) -> Unit,
    onFallback: () -> Unit
) {
    val requestPoints = points.mapIndexed { idx, point ->
        val type = if (idx == 0 || idx == points.lastIndex) RequestPointType.WAYPOINT
                   else RequestPointType.VIAPOINT
        RequestPoint(point, type, "", "")
    }

    routerSession.value = pedestrianRouter.requestRoutes(
        requestPoints,
        TimeOptions(),
        RouteOptions(FitnessOptions()),
        object : Session.RouteListener {
            override fun onMasstransitRoutes(routes: MutableList<Route>) {
                if (routes.isNotEmpty()) {
                    val bestRoute = routes[0]
                    val geometryPoints = bestRoute.geometry.points
                    val metadata = bestRoute.metadata
                    val weight = metadata.weight
                    val walkingDistance = weight.walkingDistance.value
                    val distKm = walkingDistance / 1000.0

                    val map = mapView.mapWindow.map
                    map.mapObjects.clear()
                    drawMarkers(mapView, points)
                    val polylineObj = map.mapObjects.addPolyline(Polyline(geometryPoints))
                    polylineObj.apply {
                        strokeWidth = 5f
                        setStrokeColor(android.graphics.Color.parseColor("#2D6A4F"))
                        outlineColor = android.graphics.Color.WHITE
                        outlineWidth = 2f
                    }

                    onResult(geometryPoints, distKm)
                } else {
                    onFallback()
                }
            }

            override fun onMasstransitRoutesError(error: Error) {
                onFallback()
            }
        }
    )
}

// ── Фолбэк — прямые линии ───────────────────────────────

private fun drawFallbackPolyline(mapView: MapView, points: List<Point>) {
    val map = mapView.mapWindow.map
    map.mapObjects.clear()
    drawMarkers(mapView, points)
    if (points.size >= 2) {
        val polylineObj = map.mapObjects.addPolyline(Polyline(points))
        polylineObj.apply {
            strokeWidth = 5f
            setStrokeColor(android.graphics.Color.parseColor("#2D6A4F"))
            outlineColor = android.graphics.Color.WHITE
            outlineWidth = 2f
        }
    }
}

// ── Маркеры ──────────────────────────────────────────────

private fun drawMarkers(mapView: MapView, points: List<Point>) {
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
        val placemark = mapView.mapWindow.map.mapObjects.addPlacemark(point)
        placemark.setIcon(ImageProvider.fromBitmap(bitmap))
    }
}

private fun createPickerMarkerBitmap(color: Int, size: Int, label: String): android.graphics.Bitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    paint.color = android.graphics.Color.parseColor("#33000000")
    canvas.drawCircle(size / 2f + 1, size / 2f + 1, size / 2f - 3, paint)

    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3, paint)

    paint.style = android.graphics.Paint.Style.STROKE
    paint.color = android.graphics.Color.WHITE
    paint.strokeWidth = 3f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3, paint)

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

// ── Haversine distance (фолбэк) ─────────────────────────

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
