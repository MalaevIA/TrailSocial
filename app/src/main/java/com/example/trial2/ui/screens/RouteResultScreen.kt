package com.trail2.ui.screens

// ══════════════════════════════════════════════════════════════
// Файл: ui/screens/RouteResultScreen.kt
//
// Зависимости, добавить в build.gradle.kts:
//   implementation("com.yandex.android:maps.mobile:4.5.1-full")
//
// В AndroidManifest.xml внутри <application>:
//   <meta-data
//       android:name="com.yandex.android.maps.YANDEX_MAP_KIT_API_KEY"
//       android:value="ВАША_ЯНДЕКС_КАРТЫ_API_KEY" />
//
// В Application.onCreate() (или MainActivity.onCreate() до setContent):
//   MapKitFactory.initialize(this)
// ══════════════════════════════════════════════════════════════

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.trail2.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.ai_route.GeneratedRoute
import com.trail2.ai_route.RoutePoint
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.util.RoutePhotoPlaceholder
import com.trail2.ui.util.routePhotoUrl
import com.trail2.ui.viewmodels.RouteResultViewModel
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.transport.masstransit.RouteOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.PedestrianRouter
import com.yandex.mapkit.transport.masstransit.Route
import com.yandex.mapkit.transport.masstransit.Session
import com.yandex.mapkit.transport.masstransit.TimeOptions
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteResultScreen(
    route: GeneratedRoute,
    onBack: () -> Unit,
    onRebuild: () -> Unit,
    onSaved: (String) -> Unit = {},
    onEditRoute: (String) -> Unit = {},
    vm: RouteResultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var showMap by remember { mutableStateOf(true) }
    val shareChooserTitle = stringResource(R.string.result_share)

    LaunchedEffect(uiState.publishedRouteId) {
        uiState.publishedRouteId?.let { routeId ->
            onSaved(routeId)
        }
    }

    LaunchedEffect(uiState.draftRouteId) {
        uiState.draftRouteId?.let { routeId ->
            onEditRoute(routeId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onRebuild) {
                        Icon(Icons.Outlined.Refresh, stringResource(R.string.result_rebuild), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        val shareText = buildString {
                            append(route.title)
                            append("\n${route.distanceKm} км · ${route.durationMin} мин · ${route.points.size} точек")
                            if (route.description.isNotBlank()) append("\n\n${route.description}")
                            if (route.tags.isNotEmpty()) append("\n\n${route.tags.joinToString(" ") { "#$it" }}")
                            append("\n\n#Верста")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, shareChooserTitle))
                    }) {
                        Icon(Icons.Outlined.Share, stringResource(R.string.share))
                    }
                    IconButton(
                        onClick = { vm.toggleSave() },
                        enabled = uiState.publishedRouteId != null
                    ) {
                        Icon(
                            if (uiState.isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            stringResource(R.string.save),
                            tint = if (uiState.isSaved) ForestGreen
                                   else if (uiState.publishedRouteId != null) MaterialTheme.colorScheme.onSurface
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Фотографии маршрута ───────────────────────
            if (route.photos.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(route.photos) { photo ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(routePhotoUrl(photo))
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(180.dp)
                                .width(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            } else {
                RoutePhotoPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }

            // ── Переключатель Карта / Список ──────────────
            TabRow(
                selectedTabIndex = if (showMap) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(selected = showMap, onClick = { showMap = true }) {
                    Padding { Text(stringResource(R.string.result_tab_map), modifier = Modifier.padding(vertical = 12.dp)) }
                }
                Tab(selected = !showMap, onClick = { showMap = false }) {
                    Padding { Text(stringResource(R.string.result_tab_details), modifier = Modifier.padding(vertical = 12.dp)) }
                }
            }

            if (showMap) {
                // ── Карта с наложенными маркерами ─────────
                Box(modifier = Modifier.fillMaxSize()) {
                    YandexMapView(
                        route = route,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Карточка-поп-ап снизу
                    RouteQuickStats(
                        route = route,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            } else {
                // ── Детали маршрута ───────────────────────
                RouteDetailsPanel(
                    route = route,
                    isSaving = uiState.isPublishing,
                    saveError = uiState.publishError,
                    onPublish = { vm.publishRoute(route) },
                    onSaveDraft = { vm.saveDraft(route) }
                )
            }
        }
    }
}

// ── Яндекс Карта ─────────────────────────────────────────────

@Composable
fun YandexMapView(route: GeneratedRoute, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val mapView = remember { MapView(context) }
    // Храним сессию роутера, чтобы она не была GC'd до завершения
    val routerSession = remember { mutableStateOf<Session?>(null) }

    DisposableEffect(Unit) {
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        onDispose {
            mapView.onStop()
            MapKitFactory.getInstance().onStop()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            setupMap(mv, route, context, routerSession)
        }
    )
}

private fun setupMap(
    mapView: MapView,
    route: GeneratedRoute,
    context: Context,
    routerSession: MutableState<Session?>
) {
    val map = mapView.mapWindow.map
    map.mapObjects.clear()

    if (route.points.isEmpty()) return

    val waypoints = route.points.map { Point(it.lat, it.lon) }

    // Центрируем карту, чтобы все точки были видны
    fitMapToPoints(map, mapView, waypoints)

    // Добавляем маркеры
    addMarkers(map, route, context)

    // Приоритет отрисовки маршрута:
    // 1) geometry от бекенда (подробная линия вдоль дорог)
    // 2) PedestrianRouter (запрос реального маршрута через MapKit)
    // 3) Прямая полилиния (фолбэк)
    val geometry = route.geometry
    if (geometry != null && geometry.coordinates.size >= 2) {
        // Бекенд вернул подробную геометрию — рисуем её
        val polylinePoints = geometry.coordinates.map { coord ->
            Point(coord[1], coord[0]) // GeoJSON: [lon, lat]
        }
        drawPolyline(map, polylinePoints)
    } else if (waypoints.size >= 2) {
        // Строим пешеходный маршрут через MapKit
        requestPedestrianRoute(map, waypoints, routerSession)
    }
}

private fun fitMapToPoints(map: com.yandex.mapkit.map.Map, mapView: MapView, points: List<Point>) {
    if (points.size == 1) {
        map.move(CameraPosition(points[0], 15f, 0f, 0f))
        return
    }
    val polyline = Polyline(points)
    val boundingBox = com.yandex.mapkit.geometry.BoundingBoxHelper.getBounds(polyline)
    val geometry = com.yandex.mapkit.geometry.Geometry.fromBoundingBox(boundingBox)
    var cameraPosition = map.cameraPosition(geometry)
    cameraPosition = CameraPosition(
        cameraPosition.target,
        cameraPosition.zoom - 0.5f,
        cameraPosition.azimuth,
        cameraPosition.tilt
    )
    map.move(cameraPosition)
}

private fun drawPolyline(map: com.yandex.mapkit.map.Map, points: List<Point>) {
    val polylineObj = map.mapObjects.addPolyline(Polyline(points))
    polylineObj.apply {
        strokeWidth = 5f
        setStrokeColor(android.graphics.Color.parseColor("#2D6A4F"))
        outlineColor = android.graphics.Color.WHITE
        outlineWidth = 2f
    }
}

private fun requestPedestrianRoute(
    map: com.yandex.mapkit.map.Map,
    points: List<Point>,
    routerSession: MutableState<Session?>
) {
    val pedestrianRouter: PedestrianRouter = TransportFactory.getInstance().createPedestrianRouter()

    val requestPoints = points.mapIndexed { idx, point ->
        val type = if (idx == 0 || idx == points.lastIndex) RequestPointType.WAYPOINT
                   else RequestPointType.VIAPOINT
        RequestPoint(point, type, "", "")
    }

    routerSession.value = pedestrianRouter.requestRoutes(
        requestPoints,
        TimeOptions(),
        RouteOptions(),
        object : Session.RouteListener {
            override fun onMasstransitRoutes(routes: MutableList<Route>) {
                if (routes.isNotEmpty()) {
                    val bestRoute = routes[0]
                    val geometry = bestRoute.geometry
                    drawPolyline(map, geometry.points)
                } else {
                    // Фолбэк — прямые линии
                    drawPolyline(map, points)
                }
            }

            override fun onMasstransitRoutesError(error: Error) {
                // Фолбэк — прямые линии
                drawPolyline(map, points)
            }
        }
    )
}

private fun addMarkers(map: com.yandex.mapkit.map.Map, route: GeneratedRoute, context: Context) {
    route.points.forEachIndexed { idx, point ->
        val mapPoint = Point(point.lat, point.lon)
        val markerColor = when (point.type) {
            "start"  -> android.graphics.Color.parseColor("#52B788")
            "finish" -> android.graphics.Color.parseColor("#E63946")
            else     -> android.graphics.Color.parseColor("#457B9D")
        }
        val bitmap = createMarkerBitmap(context, point, idx + 1, markerColor)
        val placemark = map.mapObjects.addPlacemark(mapPoint)
        placemark.setIcon(ImageProvider.fromBitmap(bitmap))
        placemark.addTapListener { _, _ ->
            android.widget.Toast.makeText(context, point.title, android.widget.Toast.LENGTH_SHORT).show()
            true
        }
    }
}

/** Рисует круглый цветной маркер с номером */
private fun createMarkerBitmap(
    context: Context,
    point: RoutePoint,
    number: Int,
    color: Int
): android.graphics.Bitmap {
    val size = 80  // px
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    // Тень
    paint.color = android.graphics.Color.parseColor("#33000000")
    canvas.drawCircle(size / 2f + 2, size / 2f + 2, size / 2f - 4, paint)

    // Круг
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

    // Белая обводка
    paint.style = android.graphics.Paint.Style.STROKE
    paint.color = android.graphics.Color.WHITE
    paint.strokeWidth = 4f
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

    // Номер / иконка
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 28f
    paint.textAlign = android.graphics.Paint.Align.CENTER
    val label = when (point.type) {
        "start"  -> "▶"
        "finish" -> "🏁"
        else     -> number.toString()
    }
    val textY = size / 2f - (paint.descent() + paint.ascent()) / 2
    canvas.drawText(label, size / 2f, textY, paint)

    return bitmap
}

// ── Быстрая статистика поверх карты ──────────────────────────

@Composable
private fun RouteQuickStats(route: GeneratedRoute, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatBox("📏", "${route.distanceKm} км", stringResource(R.string.result_distance))
            VerticalDivider(modifier = Modifier.height(40.dp))
            StatBox("⏱️", "${route.durationMin} мин", stringResource(R.string.result_time))
            VerticalDivider(modifier = Modifier.height(40.dp))
            StatBox("📍", "${route.points.size}", stringResource(R.string.result_points))
            VerticalDivider(modifier = Modifier.height(40.dp))
            val diffLabel = when (route.difficulty.uppercase()) {
                "EASY" -> stringResource(R.string.difficulty_easy)
                "MODERATE" -> stringResource(R.string.difficulty_medium)
                "HARD" -> stringResource(R.string.difficulty_hard)
                "EXPERT" -> stringResource(R.string.difficulty_expert)
                else -> route.difficulty
            }
            val diffColor = when (route.difficulty.uppercase()) {
                "EASY" -> Color(0xFF52B788)
                "MODERATE" -> Color(0xFFE9C46A)
                "HARD" -> Color(0xFFE63946)
                "EXPERT" -> Color(0xFF9B2226)
                else -> Color(0xFFE63946)
            }
            StatBox("💪", diffLabel, stringResource(R.string.create_difficulty), diffColor)
        }
    }
}

@Composable
private fun StatBox(emoji: String, value: String, label: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 16.sp)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Панель с деталями маршрута ────────────────────────────────

@Composable
private fun RouteDetailsPanel(
    route: GeneratedRoute,
    isSaving: Boolean = false,
    saveError: String? = null,
    onPublish: () -> Unit = {},
    onSaveDraft: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Описание
        Text(route.description, fontSize = 14.sp, lineHeight = 21.sp,
             color = MaterialTheme.colorScheme.onSurface.copy(0.85f))

        // Теги
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            route.tags.forEach { tag ->
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("#$tag", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                         fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        HorizontalDivider()

        // Точки маршрута
        Text(stringResource(R.string.result_waypoints), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        route.points.forEachIndexed { idx, point ->
            RoutePointCard(point = point, number = idx + 1)
        }

        HorizontalDivider()

        // Советы
        if (route.tips.isNotEmpty()) {
            Text(stringResource(R.string.result_tips), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            route.tips.forEach { tip ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Text("💡", fontSize = 16.sp, modifier = Modifier.width(28.dp))
                    Text(tip, fontSize = 14.sp, lineHeight = 20.sp,
                         color = MaterialTheme.colorScheme.onSurface.copy(0.85f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        if (saveError != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    saveError,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = onPublish,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (isSaving) stringResource(R.string.result_publishing) else stringResource(R.string.result_publish),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onSaveDraft,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.result_edit_route),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RoutePointCard(point: RoutePoint, number: Int) {
    val (dotColor, icon) = when (point.type) {
        "start"  -> Color(0xFF52B788) to "▶"
        "finish" -> Color(0xFFE63946) to "🏁"
        else     -> Color(0xFF457B9D) to number.toString()
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Номерной кружок
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(dotColor),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(point.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (point.description.isNotBlank()) {
                Text(point.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                     lineHeight = 17.sp)
            }
            Text("${point.lat}, ${point.lon}", fontSize = 10.sp,
                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
        }
    }
}

@Composable
private fun Padding(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(4.dp)) { content() }
}
