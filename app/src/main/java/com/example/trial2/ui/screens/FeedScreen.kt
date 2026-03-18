package com.trail2.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.data.Difficulty
import com.trail2.ui.components.RouteCard
import com.trail2.R
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
    // Перезагружаем при каждом заходе на экран
    LaunchedEffect(Unit) {
        feedVm.loadRoutes()
    }

    val uiState by feedVm.uiState.collectAsStateWithLifecycle()
    val notifState by notifVm.uiState.collectAsStateWithLifecycle()

    val filterLabels = listOf(
        stringResource(R.string.filter_all),
        stringResource(R.string.filter_easy),
        stringResource(R.string.filter_moderate),
        stringResource(R.string.filter_hard),
        stringResource(R.string.filter_expert)
    )
    val selectedFilterIndex = when (uiState.filterDifficulty) {
        Difficulty.EASY -> 1
        Difficulty.MODERATE -> 2
        Difficulty.HARD -> 3
        Difficulty.EXPERT -> 4
        else -> 0
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
                        Text(stringResource(R.string.feed_title), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = ForestGreen)
                        Text(stringResource(R.string.feed_subtitle), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onCreateRouteClick) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.feed_create_route))
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.feed_search))
                    }
                    IconButton(onClick = onNotificationsClick) {
                        BadgedBox(badge = {
                            if (notifState.unreadCount > 0) {
                                Badge { Text("${notifState.unreadCount}") }
                            }
                        }) {
                            Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.feed_notifications))
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
                    Button(onClick = feedVm::loadRoutes) { Text(stringResource(R.string.retry)) }
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
                        filters = filterLabels,
                        selectedFilter = filterLabels[selectedFilterIndex],
                        onFilterSelected = { filter ->
                            val index = filterLabels.indexOf(filter)
                            feedVm.setFilter(
                                when (index) {
                                    1 -> Difficulty.EASY
                                    2 -> Difficulty.MODERATE
                                    3 -> Difficulty.HARD
                                    4 -> Difficulty.EXPERT
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
                        onAuthorClick = { onAuthorClick(route.author.id) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(300),
                            fadeOutSpec = tween(200),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
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
            val isSelected = filter == selectedFilter
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "chip_scale_$i"
            )
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ForestGreen,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            )
        }
    }
}
