package com.trail2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trial2.data.SampleData
import com.trail2.ui.components.RouteCard
import com.trail2.ui.theme.ForestGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(onRouteClick: (String) -> Unit) {
    val routes = remember { SampleData.routes }
    var selectedFilter by remember { mutableStateOf("Все") }
    val filters = listOf("Все", "Лёгкие", "Сложные", "Многодневные", "Выходного дня")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TrailSocial", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = ForestGreen)
                        Text("Маршруты рядом с вами", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = {}) {
                        BadgedBox(badge = { Badge { Text("3") } }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Уведомления")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stories / Active users row
            item {
                StoriesRow()
            }

            // Filter chips
            item {
                FilterChipsRow(
                    filters = filters,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )
            }

            // Routes feed
            items(routes) { route ->
                RouteCard(route = route, onClick = { onRouteClick(route.id) })
            }

            item { Spacer(Modifier.height(72.dp)) } // bottom nav space
        }
    }
}

@Composable
fun StoriesRow() {
    val users = SampleData.users
    Column {
        Text("Активные авторы", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(users.size) { i ->
                val user = users[i]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        com.trail2.ui.components.UserAvatar(
                            colorHex = user.avatarUrl,
                            name = user.name,
                            size = 52
                        )
                        if (i < 2) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).size(14.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color(0xFF52B788),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                            ) {}
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(user.username, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(filters: List<String>, selectedFilter: String, onFilterSelected: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters.size) { i ->
            val filter = filters[i]
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ForestGreen,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
