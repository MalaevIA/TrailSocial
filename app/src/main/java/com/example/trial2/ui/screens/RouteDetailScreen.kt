package com.trail2.ui.screens

import android.content.Intent
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.R
import com.trail2.data.Difficulty
import com.trail2.ui.components.*
import com.trail2.ui.components.RouteMapView
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.theme.MossGreen
import com.trail2.ui.viewmodels.RouteDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    routeId: String,
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
    vm: RouteDetailViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(routeId) { vm.loadRoute(routeId) }

    val route = uiState.route
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
                            append("\n\n#TrailSocial")
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
            if (route.geometry != null && route.startLat != null && route.startLng != null && route.endLat != null && route.endLng != null) {
                Spacer(Modifier.height(8.dp))
                RouteMapView(
                    geometry = route.geometry.coordinates,
                    startLat = route.startLat,
                    startLng = route.startLng,
                    endLat = route.endLat,
                    endLng = route.endLng,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(8.dp))
            }

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
