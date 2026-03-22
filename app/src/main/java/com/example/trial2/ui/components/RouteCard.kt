package com.trail2.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trail2.data.Difficulty
import com.trail2.data.GeoJsonLineString
import com.trail2.data.TrailRoute
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.trail2.ui.theme.*
import com.trail2.ui.util.RoutePhotoPlaceholder
import com.trail2.ui.util.formatDate
import com.trail2.ui.util.routePhotoUrl

@Composable
fun RouteCard(
    route: TrailRoute,
    onClick: () -> Unit,
    onLikeClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onAuthorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp).clickable { onAuthorClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(avatarUrl = route.author.avatarUrl, name = route.author.name, size = 38)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(route.author.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(route.region, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatDate(route.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Photo banner
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                if (route.photos.isEmpty()) {
                    // No photos — lightweight Canvas preview (never use MapView inside LazyColumn)
                    RouteMapPreview(geometry = route.geometry, modifier = Modifier.fillMaxSize())
                } else if (route.photos.size == 1) {
                    // Single photo with map fallback on error
                    val photoUrl = routePhotoUrl(route.photos[0])
                    var failed by remember(route.id, photoUrl) { mutableStateOf(false) }
                    if (failed) {
                        RouteMapPreview(geometry = route.geometry, modifier = Modifier.fillMaxSize())
                    } else {
                        RoutePhotoPlaceholder(modifier = Modifier.fillMaxSize())
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onError = { failed = true }
                        )
                    }
                } else {
                    // Multiple photos — swipeable carousel with map fallback on error
                    var anyFailed by remember(route.id, route.photos) { mutableStateOf(false) }
                    val pagerState = rememberPagerState { route.photos.size }
                    if (anyFailed) {
                        RouteMapPreview(geometry = route.geometry, modifier = Modifier.fillMaxSize())
                    } else {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                RoutePhotoPlaceholder(modifier = Modifier.fillMaxSize())
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(routePhotoUrl(route.photos[page]))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    onError = { anyFailed = true }
                                )
                            }
                        }
                        // Dot indicators
                        Row(
                            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            route.photos.indices.forEach { i ->
                                val isSelected = pagerState.currentPage == i
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else Color.White.copy(0.5f))
                                )
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.5f))))
                DifficultyBadge(difficulty = route.difficulty, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    Text(route.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📍", fontSize = 12.sp)
                        Spacer(Modifier.width(3.dp))
                        Text(route.region.take(30), fontSize = 12.sp, color = Color.White.copy(0.9f))
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouteStatItem("📏", "${route.distanceKm} км")
                RouteStatItem("⛰️", "+${route.elevationGainM} м")
                RouteStatItem("⏱️", formatDurationMinutes(route.durationMinutes))
                RouteStatItem("💪", difficultyShortLabel(route.difficulty))
            }

            // Description
            Text(
                text = route.description,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            // Tags
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                route.tags.forEach { TagChip(it) }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Action bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(
                    icon = if (route.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    tint = if (route.isLiked) Color(0xFFE63946) else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = formatCount(route.likesCount),
                    onClick = onLikeClick
                )
                ActionButton(Icons.Outlined.ChatBubbleOutline, MaterialTheme.colorScheme.onSurfaceVariant, formatCount(route.commentsCount)) { onClick() }
                ActionButton(Icons.Outlined.Share, MaterialTheme.colorScheme.onSurfaceVariant, "Поделиться") {}
                ActionButton(
                    icon = if (route.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    tint = if (route.isSaved) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = if (route.isSaved) "Сохранено" else "Сохранить",
                    onClick = onSaveClick
                )
            }
        }
    }
}

@Composable
fun UserAvatar(avatarUrl: String, name: String, size: Int) {
    val context = LocalContext.current
    var loadFailed by remember(avatarUrl) { mutableStateOf(false) }
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(ForestGreen),
        contentAlignment = Alignment.Center
    ) {
        val isValidUrl = avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")
        if (isValidUrl && !loadFailed) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { loadFailed = true }
            )
        } else {
            Text(name.take(1).uppercase(), color = Color.White, fontSize = (size * 0.4).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: Difficulty, modifier: Modifier = Modifier) {
    val (label, color) = when (difficulty) {
        Difficulty.EASY -> "Лёгкий" to Color(0xFF52B788)
        Difficulty.MODERATE -> "Средний" to Color(0xFFE9C46A)
        Difficulty.HARD -> "Сложный" to Color(0xFFE76F51)
        Difficulty.EXPERT -> "Эксперт" to Color(0xFFE63946)
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.9f)) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RouteStatItem(emoji: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 14.sp)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TagChip(tag: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text("#$tag", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = tint)
    }
}

fun formatDurationMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes} мин"
    minutes < 1440 -> "${minutes / 60} ч"
    else -> "${minutes / 1440} дн"
}

fun difficultyShortLabel(d: Difficulty): String = when (d) {
    Difficulty.EASY -> "Лёгкий"
    Difficulty.MODERATE -> "Средний"
    Difficulty.HARD -> "Сложный"
    Difficulty.EXPERT -> "Эксперт"
}

fun formatCount(count: Int): String = if (count >= 1000) "${count / 1000}.${(count % 1000) / 100}к" else count.toString()

@Composable
fun RouteMapPreview(geometry: GeoJsonLineString?, modifier: Modifier = Modifier) {
    val lineColor = Color(0xFF2D6A4F)
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (geometry != null && geometry.coordinates.size >= 2) {
            Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                val coords = geometry.coordinates
                // Normalize coordinates to [0,1] range
                val lngs = coords.map { it[0] }
                val lats = coords.map { it[1] }
                val minLng = lngs.min(); val maxLng = lngs.max()
                val minLat = lats.min(); val maxLat = lats.max()
                val lngRange = (maxLng - minLng).takeIf { it > 0 } ?: 1.0
                val latRange = (maxLat - minLat).takeIf { it > 0 } ?: 1.0
                val aspectRatio = size.width / size.height
                val scale = if (lngRange / latRange > aspectRatio) size.width / lngRange else size.height / latRange
                val offsetX = (size.width - lngRange * scale) / 2f
                val offsetY = (size.height - latRange * scale) / 2f

                fun toOffset(coord: List<Double>): Offset {
                    val x = ((coord[0] - minLng) * scale + offsetX).toFloat()
                    val y = (size.height - (coord[1] - minLat) * scale - offsetY).toFloat()
                    return Offset(x, y)
                }

                // White outline stroke
                val path = Path()
                path.moveTo(toOffset(coords[0]).x, toOffset(coords[0]).y)
                coords.drop(1).forEach { path.lineTo(toOffset(it).x, toOffset(it).y) }
                drawPath(path, Color.White.copy(alpha = 0.7f), style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // Green line
                drawPath(path, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Start dot (green filled, white border)
                val start = toOffset(coords.first())
                drawCircle(Color.White, radius = 8f, center = start)
                drawCircle(lineColor, radius = 5f, center = start)
                // End dot (red filled, white border)
                val end = toOffset(coords.last())
                drawCircle(Color.White, radius = 8f, center = end)
                drawCircle(Color(0xFFE63946), radius = 5f, center = end)
            }
        } else {
            // No geometry — show landscape icon placeholder
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Outlined.Landscape,
                contentDescription = null,
                tint = lineColor.copy(alpha = 0.4f),
                modifier = Modifier.size(52.dp).align(Alignment.Center)
            )
        }
    }
}
