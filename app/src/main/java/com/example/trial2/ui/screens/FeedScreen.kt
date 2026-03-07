package com.trail2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.data.Difficulty
import com.trail2.ui.components.RouteCard
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.viewmodels.FeedViewModel
import com.trail2.ui.viewmodels.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onRouteClick: (String) -> Unit,
    onNotificationsClick: () -> Unit = {},
    onCreateRouteClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    feedVm: FeedViewModel = hiltViewModel(),
    notifVm: NotificationViewModel = hiltViewModel()
) {
    val uiState by feedVm.uiState.collectAsStateWithLifecycle()
    val notifState by notifVm.uiState.collectAsStateWithLifecycle()

    val filters = listOf("Все", "Лёгкие", "Сложные", "Выходного дня")
    val selectedFilter = when (uiState.filterDifficulty) {
        Difficulty.EASY -> "Лёгкие"
        Difficulty.HARD, Difficulty.EXPERT -> "Сложные"
        else -> "Все"
    }

    val listState = rememberLazyListState()

    LaunchedEffect(listState.canScrollForward) {
        if (!listState.canScrollForward && uiState.hasMorePages && !uiState.isLoadingMore) {
            feedVm.loadMore()
        }
    }

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
                    IconButton(onClick = onCreateRouteClick) {
                        Icon(Icons.Default.Add, contentDescription = "Создать маршрут")
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Outlined.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = onNotificationsClick) {
                        BadgedBox(badge = {
                            if (notifState.unreadCount > 0) {
                                Badge { Text("${notifState.unreadCount}") }
                            }
                        }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Уведомления")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.routes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null && uiState.routes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = feedVm::loadRoutes) { Text("Повторить") }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FilterChipsRow(
                        filters = filters,
                        selectedFilter = selectedFilter,
                        onFilterSelected = { filter ->
                            feedVm.setFilter(
                                when (filter) {
                                    "Лёгкие" -> Difficulty.EASY
                                    "Сложные" -> Difficulty.HARD
                                    else -> null
                                }
                            )
                        }
                    )
                }

                items(uiState.routes, key = { it.id }) { route ->
                    RouteCard(
                        route = route,
                        onClick = { onRouteClick(route.id) },
                        onLikeClick = { feedVm.toggleLike(route.id, route.isLiked) },
                        onSaveClick = { feedVm.toggleSave(route.id, route.isSaved) },
                        onAuthorClick = { onAuthorClick(route.author.id) }
                    )
                }

                if (uiState.isLoadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
fun FilterChipsRow(filters: List<String>, selectedFilter: String, onFilterSelected: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
