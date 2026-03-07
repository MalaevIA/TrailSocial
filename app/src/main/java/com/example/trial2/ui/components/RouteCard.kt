package com.trail2.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trail2.data.Difficulty
import com.trail2.data.TrailRoute
import com.trail2.ui.theme.*

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
                UserAvatar(colorHex = route.author.avatarUrl, name = route.author.name, size = 38)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(route.author.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(route.region, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(route.createdAt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Photo banner
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                val photoColor = try { Color(android.graphics.Color.parseColor("#${route.photos.first()}")) } catch (e: Exception) { ForestGreen }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(photoColor, photoColor.copy(alpha = 0.7f)))))
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.5f))))
                DifficultyBadge(difficulty = route.difficulty, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
                if (route.photos.size > 1) {
                    Row(modifier = Modifier.align(Alignment.TopStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        route.photos.take(4).forEachIndexed { i, _ ->
                            Box(modifier = Modifier.size(if (i == 0) 8.dp else 6.dp).clip(CircleShape).background(if (i == 0) Color.White else Color.White.copy(0.5f)))
                        }
                    }
                }
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
fun UserAvatar(colorHex: String, name: String, size: Int) {
    val color = try { Color(android.graphics.Color.parseColor("#$colorHex")) } catch (e: Exception) { ForestGreen }
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(name.take(1), color = Color.White, fontSize = (size * 0.4).sp, fontWeight = FontWeight.Bold)
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
