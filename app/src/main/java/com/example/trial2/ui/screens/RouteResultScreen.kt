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

import android.R.attr.strokeColor
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.trail2.ai_route.GeneratedRoute
import com.trail2.ai_route.RoutePoint
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteResultScreen(
    route: GeneratedRoute,
    onBack: () -> Unit,
    onRebuild: () -> Unit
) {
    var showMap by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onRebuild) {
                        Icon(Icons.Outlined.Refresh, "Перестроить", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Share, "Поделиться")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Переключатель Карта / Список ──────────────
            TabRow(
                selectedTabIndex = if (showMap) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(selected = showMap, onClick = { showMap = true }) {
                    Padding { Text("🗺️  Карта", modifier = Modifier.padding(vertical = 12.dp)) }
                }
                Tab(selected = !showMap, onClick = { showMap = false }) {
                    Padding { Text("📋  Детали", modifier = Modifier.padding(vertical = 12.dp)) }
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
                RouteDetailsPanel(route = route)
            }
        }
    }
}

// ── Яндекс Карта ─────────────────────────────────────────────

@Composable
fun YandexMapView(route: GeneratedRoute, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Запоминаем MapView, чтобы не пересоздавать при рекомпозиции
    val mapView = remember { MapView(context) }

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
            setupMap(mv, route, context)
        }
    )
}

private fun setupMap(mapView: MapView, route: GeneratedRoute, context: Context) {
    val map = mapView.mapWindow.map
    map.mapObjects.clear()

    if (route.points.isEmpty()) return

    // Центрируем карту на середину маршрута
    val midPoint = route.points[route.points.size / 2]
    map.move(
        com.yandex.mapkit.map.CameraPosition(
            Point(midPoint.lat, midPoint.lon), 13f, 0f, 0f
        )
    )

    // ── Ломаная линия маршрута ─────────────────────────────
    val polylinePoints = route.points.map { Point(it.lat, it.lon) }
    val polylineObj = map.mapObjects.addPolyline(Polyline(polylinePoints))
    polylineObj.apply {
        var myStrokeColor = android.graphics.Color.parseColor("#2D6A4F")
        strokeWidth = 5f
        outlineColor = android.graphics.Color.WHITE
        outlineWidth = 2f
    }

    // ── Маркеры точек ─────────────────────────────────────
    route.points.forEachIndexed { idx, point ->
        val mapPoint = Point(point.lat, point.lon)

        // Цвет и размер маркера зависит от типа точки
        val markerColor = when (point.type) {
            "start"   -> android.graphics.Color.parseColor("#52B788")   // зелёный
            "finish"  -> android.graphics.Color.parseColor("#E63946")   // красный
            else      -> android.graphics.Color.parseColor("#457B9D")   // синий
        }

        // Создаём bitmap-маркер программно
        val bitmap = createMarkerBitmap(context, point, idx + 1, markerColor)
        val placemark = map.mapObjects.addPlacemark(mapPoint)
        placemark.setIcon(ImageProvider.fromBitmap(bitmap))

        // Тап по маркеру → показать подсказку (через Toast или кастомный попап)
        placemark.addTapListener { _, _ ->
            android.widget.Toast.makeText(context, "📍 ${point.title}", android.widget.Toast.LENGTH_SHORT).show()
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
            StatBox("📏", "${route.distanceKm} км", "Дистанция")
            VerticalDivider(modifier = Modifier.height(40.dp))
            StatBox("⏱️", "${route.durationMin} мин", "Время")
            VerticalDivider(modifier = Modifier.height(40.dp))
            StatBox("📍", "${route.points.size}", "Точек")
            VerticalDivider(modifier = Modifier.height(40.dp))
            val diffLabel = when (route.difficulty) {
                "EASY" -> "Лёгкий"
                "MODERATE" -> "Средний"
                else -> "Сложный"
            }
            val diffColor = when (route.difficulty) {
                "EASY" -> Color(0xFF52B788)
                "MODERATE" -> Color(0xFFE9C46A)
                else -> Color(0xFFE63946)
            }
            StatBox("💪", diffLabel, "Уровень", diffColor)
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
private fun RouteDetailsPanel(route: GeneratedRoute) {
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
        Text("Точки маршрута", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        route.points.forEachIndexed { idx, point ->
            RoutePointCard(point = point, number = idx + 1)
        }

        HorizontalDivider()

        // Советы
        if (route.tips.isNotEmpty()) {
            Text("Советы", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            route.tips.forEach { tip ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Text("💡", fontSize = 16.sp, modifier = Modifier.width(28.dp))
                    Text(tip, fontSize = 14.sp, lineHeight = 20.sp,
                         color = MaterialTheme.colorScheme.onSurface.copy(0.85f))
                }
            }
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
