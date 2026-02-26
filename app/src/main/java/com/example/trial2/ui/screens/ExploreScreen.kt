package com.trail2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
import com.trail2.ui.components.DifficultyBadge
import com.trail2.ui.theme.ForestGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onRouteClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val routes = SampleData.routes

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Поиск маршрутов", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = ForestGreen)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Урал, Кавказ, Алтай...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = ForestGreen) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Популярные регионы", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(regionCards.size) { i ->
                        val (region, color, count) = regionCards[i]
                        RegionCard(region, color, count) {}
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Все маршруты", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
            }

            val filtered = if (searchQuery.isBlank()) routes
            else routes.filter { it.title.contains(searchQuery, true) || it.region.contains(searchQuery, true) || it.tags.any { t -> t.contains(searchQuery, true) } }

            items(filtered.size) { i ->
                val route = filtered[i]
                ExploreRouteRow(route = route, onClick = { onRouteClick(route.id) })
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

val regionCards = listOf(
    Triple("Урал", "2D6A4F", "12 маршрутов"),
    Triple("Кавказ", "E76F51", "28 маршрутов"),
    Triple("Алтай", "457B9D", "19 маршрутов"),
    Triple("Камчатка", "E63946", "7 маршрутов"),
    Triple("Карелия", "264653", "15 маршрутов"),
    Triple("Сибирь", "6D6875", "9 маршрутов")
)

@Composable
fun RegionCard(region: String, colorHex: String, count: String, onClick: () -> Unit) {
    val color = try { Color(android.graphics.Color.parseColor("#$colorHex")) } catch (e: Exception) { ForestGreen }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(color.copy(0.7f), color)))
            .clickable { onClick() },
        contentAlignment = Alignment.BottomStart
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(region, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(count, color = Color.White.copy(0.8f), fontSize = 11.sp)
        }
    }
}

@Composable
fun ExploreRouteRow(route: com.trail2.data.TrailRoute, onClick: () -> Unit) {
    val photoColor = try { Color(android.graphics.Color.parseColor("#${route.photos.first()}")) } catch (e: Exception) { ForestGreen }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.width(90.dp).height(80.dp).background(photoColor),
                contentAlignment = Alignment.Center
            ) {
                DifficultyBadge(route.difficulty)
            }
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text(route.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                Text(route.region, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📏 ${route.distanceKm}км", fontSize = 11.sp)
                    Text("⭐ ${route.rating}", fontSize = 11.sp)
                    Text("❤️ ${route.likesCount}", fontSize = 11.sp)
                }
            }
        }
    }
}
