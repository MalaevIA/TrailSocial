package com.trail2.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trail2.data.SampleData
import com.trail2.data.Difficulty
import com.trail2.data.TrailRoute
import com.trail2.ui.components.*
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.theme.MossGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(routeId: String, onBack: () -> Unit) {
    val route = SampleData.routes.find { it.id == routeId } ?: return
    var liked by remember { mutableStateOf(route.isLiked) }
    var saved by remember { mutableStateOf(route.isSaved) }
    var commentText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.title, maxLines = 1, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { saved = !saved }) {
                        Icon(
                            if (saved) Icons.Filled.AccountBox else Icons.Outlined.AccountBox,
                            contentDescription = "Сохранить",
                            tint = if (saved) ForestGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Share, contentDescription = "Поделиться")
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
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Добавить комментарий...", fontSize = 14.sp) },
                        maxLines = 2,
                        shape = RoundedCornerShape(24.dp)
                    )
                    IconButton(
                        onClick = { commentText = "" },
                        modifier = Modifier.clip(CircleShape).background(ForestGreen).size(48.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Отправить", tint = Color.White)
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
            // Hero photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                val photoColor = try { Color(android.graphics.Color.parseColor("#${route.photos.first()}")) } catch (e: Exception) { ForestGreen }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(photoColor.copy(0.8f), photoColor))))
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(0.4f))))

                // Photo thumbnails at bottom
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    route.photos.take(3).forEachIndexed { i, colorHex ->
                        val c = try { Color(android.graphics.Color.parseColor("#$colorHex")) } catch (e: Exception) { MossGreen }
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
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+${route.photos.size - 3}", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                DifficultyBadge(route.difficulty, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Author
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(colorHex = route.author.avatarUrl, name = route.author.name, size = 44)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(route.author.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("@${route.author.username} · ${route.createdAt}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = {}, shape = RoundedCornerShape(20.dp)) {
                        Text("Подписаться", fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Title and rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(route.title, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(
                            if (i < route.rating.toInt()) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = Color(0xFFE9C46A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("${route.rating} (${route.commentsCount} отзывов)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(16.dp))

                // Location
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(route.location, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(route.region, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stats grid
                Text("Характеристики маршрута", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailStatCard("📏", "Дистанция", "${route.distanceKm} км", modifier = Modifier.weight(1f))
                    DetailStatCard("⛰️", "Набор высоты", "+${route.elevationGainM} м", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailStatCard("⏱️", "Время", formatDuration(route.durationHours), modifier = Modifier.weight(1f))
                    DetailStatCard("💪", "Сложность", difficultyLabel(route.difficulty), modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                // Description
                Text("Описание", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Text(route.description, fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.85f))

                Spacer(Modifier.height(16.dp))

                // Tags
                Text("Теги", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    route.tags.forEach { TagChip(it) }
                }

                Spacer(Modifier.height(20.dp))

                // Action buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { liked = !liked },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (liked) Color(0xFFE63946) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (liked) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (liked) "Нравится (${route.likesCount + 1})" else "Нравится (${route.likesCount})")
                    }
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Карта")
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Comments
                Text("Комментарии (${route.commentsCount})", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                SampleData.comments.forEach { comment ->
                    CommentItem(comment)
                    Spacer(Modifier.height(12.dp))
                }
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
                Text(comment.createdAt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(3.dp))
            Text(comment.text, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(3.dp))
                Text("${comment.likesCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("Ответить", fontSize = 11.sp, color = ForestGreen, fontWeight = FontWeight.Medium)
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
