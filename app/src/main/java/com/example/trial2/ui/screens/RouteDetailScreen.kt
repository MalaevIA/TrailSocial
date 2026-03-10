package com.trail2.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.R
import com.trail2.data.Difficulty
import com.trail2.data.RouteStatus
import com.trail2.ui.components.*
import com.trail2.ui.components.RouteMapView
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.theme.MossGreen
import com.trail2.ui.viewmodels.RouteDetailViewModel
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.runtime.image.ImageProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    routeId: String,
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onEditRoute: (String) -> Unit = {},
    onClonedRoute: (String) -> Unit = {},
    vm: RouteDetailViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(routeId) { vm.loadRoute(routeId) }

    LaunchedEffect(uiState.clonedRouteId) {
        uiState.clonedRouteId?.let { onClonedRoute(it) }
    }

    val route = uiState.route

    // Full-screen walking/navigation mode
    if (uiState.isNavigating && route != null && route.geometry != null &&
        route.startLat != null && route.startLng != null && route.endLat != null && route.endLng != null
    ) {
        RouteWalkingScreen(
            geometry = route.geometry.coordinates,
            startLat = route.startLat,
            startLng = route.startLng,
            endLat = route.endLat,
            endLng = route.endLng,
            routeTitle = route.title,
            distanceKm = route.distanceKm,
            onStop = { vm.stopNavigation() }
        )
        return
    }

    val scrollState = rememberScrollState()

    if (uiState.isLoading && route == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (route == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.error ?: stringResource(R.string.route_not_found))
                Spacer(Modifier.height(8.dp))
                Button(onClick = onBack) { Text(stringResource(R.string.back)) }
            }
        }
        return
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.title, maxLines = 1, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = vm::toggleSave) {
                        Icon(
                            if (route.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.save),
                            tint = if (route.isSaved) ForestGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        val shareText = buildString {
                            append(route.title)
                            append("\n${route.region} · ${route.distanceKm} км")
                            if (route.description.isNotBlank()) append("\n\n${route.description}")
                            append("\n\n#Верста")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.route_share_title)))
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.commentText,
                        onValueChange = vm::onCommentTextChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.route_add_comment), fontSize = 14.sp) },
                        maxLines = 2,
                        shape = RoundedCornerShape(24.dp)
                    )
                    IconButton(
                        onClick = vm::sendComment,
                        enabled = !uiState.isSendingComment,
                        modifier = Modifier.clip(CircleShape).background(ForestGreen).size(48.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = stringResource(R.string.send), tint = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                val photoColor = try { Color(android.graphics.Color.parseColor("#${route.photos.firstOrNull()}")) } catch (_: Exception) { ForestGreen }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(photoColor.copy(0.8f), photoColor))))
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(0.4f))))

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    route.photos.take(3).forEachIndexed { i, colorHex ->
                        val c = try { Color(android.graphics.Color.parseColor("#$colorHex")) } catch (_: Exception) { MossGreen }
                        Box(
                            modifier = Modifier
                                .size(if (i == 0) 48.dp else 36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(c)
                                .border(if (i == 0) 2.dp else 0.dp, Color.White, RoundedCornerShape(8.dp))
                        )
                    }
                    if (route.photos.size > 3) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+${route.photos.size - 3}", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                DifficultyBadge(route.difficulty, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))
            }

            // ── Route map ──
            val hasGeometry = route.geometry != null && route.startLat != null &&
                    route.startLng != null && route.endLat != null && route.endLng != null

            if (hasGeometry) {
                Spacer(Modifier.height(8.dp))
                RouteMapView(
                    geometry = route.geometry!!.coordinates,
                    startLat = route.startLat!!,
                    startLng = route.startLng!!,
                    endLat = route.endLat!!,
                    endLng = route.endLng!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                )
            } else {
                // Заглушка карты — без геометрии
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Map, null, modifier = Modifier.size(48.dp), tint = ForestGreen)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.route_no_map),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    if (hasGeometry) vm.startNavigation()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = hasGeometry,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Icon(Icons.Filled.Navigation, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.route_start_walking), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAuthorClick(route.author.id) }
                ) {
                    UserAvatar(colorHex = route.author.avatarUrl, name = route.author.name, size = 44)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(route.author.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("@${route.author.username} · ${route.createdAt.take(10)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(
                        onClick = vm::toggleFollow,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            if (route.author.isFollowing) stringResource(R.string.route_unsubscribe) else stringResource(R.string.route_subscribe),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(route.title, fontWeight = FontWeight.Bold, fontSize = 22.sp)

                Spacer(Modifier.height(4.dp))
                Text("${route.commentsCount} комментариев · ${route.likesCount} лайков", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(16.dp))

                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(route.region, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Характеристики маршрута", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailStatCard("📏", "Дистанция", "${route.distanceKm} км", modifier = Modifier.weight(1f))
                    DetailStatCard("⛰️", "Набор высоты", "+${route.elevationGainM} м", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailStatCard("⏱️", "Время", formatDurationMinutes(route.durationMinutes), modifier = Modifier.weight(1f))
                    DetailStatCard("💪", "Сложность", difficultyLabel(route.difficulty), modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                Text("Описание", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Text(route.description, fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.85f))

                Spacer(Modifier.height(16.dp))

                if (route.tags.isNotEmpty()) {
                    Text("Теги", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        route.tags.forEach { TagChip(it) }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = vm::toggleLike,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (route.isLiked) Color(0xFFE63946) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (route.isLiked) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(if (route.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${route.likesCount}")
                    }
                    Button(
                        onClick = vm::toggleSave,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (route.isSaved) ForestGreen else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (route.isSaved) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(if (route.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Сохранить")
                    }
                }

                // Edit / Clone / Publish
                Spacer(Modifier.height(12.dp))
                if (uiState.isOwnRoute) {
                    // Own route: Edit + Publish (if draft)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { onEditRoute(route.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.EditLocationAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.route_edit_on_map))
                        }
                        if (route.status == RouteStatus.DRAFT) {
                            Button(
                                onClick = vm::publishDraft,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                            ) {
                                Icon(Icons.Filled.Publish, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.route_publish))
                            }
                        }
                    }
                    if (route.status == RouteStatus.DRAFT) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                stringResource(R.string.route_draft_badge),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.route_delete))
                    }
                } else {
                    // Other's route: Clone to draft
                    OutlinedButton(
                        onClick = vm::cloneToDraft,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isCloning
                    ) {
                        if (uiState.isCloning) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.route_clone_draft))
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text("Комментарии (${uiState.comments.size})", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))

                if (uiState.comments.isEmpty()) {
                    Text("Пока нет комментариев", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                } else {
                    uiState.comments.forEach { comment ->
                        CommentItem(comment)
                        Spacer(Modifier.height(12.dp))
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.route_delete_confirm)) },
            text = { Text(stringResource(R.string.route_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteRoute()
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun DetailStatCard(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CommentItem(comment: com.trail2.data.Comment) {
    Row(modifier = Modifier.fillMaxWidth()) {
        UserAvatar(colorHex = comment.author.avatarUrl, name = comment.author.name, size = 36)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.author.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Text(comment.createdAt.take(10), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(3.dp))
            Text(comment.text, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (comment.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = if (comment.isLiked) Color(0xFFE63946) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(3.dp))
                Text("${comment.likesCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun difficultyLabel(d: Difficulty) = when (d) {
    Difficulty.EASY -> "Лёгкий"
    Difficulty.MODERATE -> "Средний"
    Difficulty.HARD -> "Сложный"
    Difficulty.EXPERT -> "Экспертный"
}

fun formatDurationMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m} мин"
        m == 0 -> "${h} ч"
        else -> "${h} ч ${m} мин"
    }
}

// ── Full-screen walking/navigation screen ──────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteWalkingScreen(
    geometry: List<List<Double>>,
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double,
    routeTitle: String,
    distanceKm: Double,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var userLocationLayer by remember { mutableStateOf<UserLocationLayer?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    DisposableEffect(Unit) {
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        onDispose {
            mapView.onStop()
            MapKitFactory.getInstance().onStop()
        }
    }

    // Request location permission
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Enable user location layer when permission granted
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val layer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
            layer.isVisible = true
            layer.isHeadingEnabled = true
            userLocationLayer = layer
        }
    }

    val polylinePoints = remember(geometry) {
        if (geometry.size >= 2) {
            geometry.map { coord -> Point(coord[1], coord[0]) }
        } else {
            listOf(Point(startLat, startLng), Point(endLat, endLng))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.route_walking_title), fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onStop) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(routeTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Text("${"%.2f".format(distanceKm)} км", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE63946))
                    ) {
                        Icon(Icons.Filled.Stop, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.route_stop_walking), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { padding ->
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize().padding(padding),
            update = { mv ->
                val map = mv.mapWindow.map
                map.mapObjects.clear()

                // Draw route polyline
                if (polylinePoints.size >= 2) {
                    val polylineObj = map.mapObjects.addPolyline(Polyline(polylinePoints))
                    polylineObj.apply {
                        strokeWidth = 6f
                        setStrokeColor(android.graphics.Color.parseColor("#2D6A4F"))
                        outlineColor = android.graphics.Color.WHITE
                        outlineWidth = 2f
                    }
                }

                // Start marker
                val startBitmap = createWalkingMarkerBitmap(
                    android.graphics.Color.parseColor("#52B788"), 64, "S"
                )
                val startPlacemark = map.mapObjects.addPlacemark(polylinePoints.first())
                startPlacemark.setIcon(ImageProvider.fromBitmap(startBitmap))

                // Finish marker
                if (polylinePoints.size > 1) {
                    val finishBitmap = createWalkingMarkerBitmap(
                        android.graphics.Color.parseColor("#E63946"), 64, "F"
                    )
                    val finishPlacemark = map.mapObjects.addPlacemark(polylinePoints.last())
                    finishPlacemark.setIcon(ImageProvider.fromBitmap(finishBitmap))
                }

                // Fit camera to route
                val minLat = polylinePoints.minOf { it.latitude }
                val maxLat = polylinePoints.maxOf { it.latitude }
                val minLng = polylinePoints.minOf { it.longitude }
                val maxLng = polylinePoints.maxOf { it.longitude }

                if (minLat == maxLat && minLng == maxLng) {
                    map.move(CameraPosition(Point(minLat, minLng), 15f, 0f, 0f))
                } else {
                    val boundingBox = BoundingBox(Point(minLat, minLng), Point(maxLat, maxLng))
                    val cameraPosition = map.cameraPosition(Geometry.fromBoundingBox(boundingBox))
                    map.move(CameraPosition(cameraPosition.target, cameraPosition.zoom - 0.3f, 0f, 0f))
                }
            }
        )
    }
}

private fun createWalkingMarkerBitmap(color: Int, size: Int, label: String): android.graphics.Bitmap {
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
