package com.trail2.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.ui.components.RouteCard
import com.trail2.R
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.viewmodels.FeedTab
import com.trail2.ui.viewmodels.FeedTabState
import com.trail2.ui.viewmodels.FeedViewModel
import com.trail2.ui.viewmodels.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onRouteClick: (String) -> Unit,
    onNotificationsClick: () -> Unit = {},
    onCreateRouteClick: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onLoginClick: () -> Unit = {},
    feedVm: FeedViewModel = hiltViewModel(),
    notifVm: NotificationViewModel = hiltViewModel()
) {
    val uiState by feedVm.uiState.collectAsStateWithLifecycle()
    val notifState by notifVm.uiState.collectAsStateWithLifecycle()

    val feedListState = rememberLazyListState()
    val recommendedListState = rememberLazyListState()

    LaunchedEffect(uiState.selectedTab) {
        when (uiState.selectedTab) {
            FeedTab.FEED -> feedListState.animateScrollToItem(0)
            FeedTab.RECOMMENDED -> recommendedListState.animateScrollToItem(0)
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ForestGreen
            ) {
                Tab(
                    selected = uiState.selectedTab == FeedTab.FEED,
                    onClick = { feedVm.switchTab(FeedTab.FEED) },
                    text = { Text(stringResource(R.string.feed_tab_subscriptions), fontSize = 14.sp) }
                )
                Tab(
                    selected = uiState.selectedTab == FeedTab.RECOMMENDED,
                    onClick = { feedVm.switchTab(FeedTab.RECOMMENDED) },
                    text = { Text(stringResource(R.string.feed_tab_recommended), fontSize = 14.sp) }
                )
            }

            when (uiState.selectedTab) {
                FeedTab.FEED -> {
                    if (!uiState.isLoggedIn) {
                        FeedLoginStub(onLoginClick = onLoginClick)
                    } else {
                        FeedTabContent(
                            state = uiState.feedState,
                            listState = feedListState,
                            emptyText = stringResource(R.string.feed_empty_subscriptions),
                            onRouteClick = onRouteClick,
                            onAuthorClick = onAuthorClick,
                            onLikeClick = { id, liked -> feedVm.toggleLike(id, liked) },
                            onSaveClick = { id, saved -> feedVm.toggleSave(id, saved) },
                            onLoadMore = feedVm::loadMoreFeed,
                            onRefresh = feedVm::loadFeed,
                            onRetry = feedVm::loadFeed
                        )
                    }
                }
                FeedTab.RECOMMENDED -> FeedTabContent(
                    state = uiState.recommendedState,
                    listState = recommendedListState,
                    emptyText = stringResource(R.string.feed_empty_recommended),
                    onRouteClick = onRouteClick,
                    onAuthorClick = onAuthorClick,
                    onLikeClick = { id, liked -> feedVm.toggleLike(id, liked) },
                    onSaveClick = { id, saved -> feedVm.toggleSave(id, saved) },
                    onLoadMore = feedVm::loadMoreRecommended,
                    onRefresh = feedVm::loadRecommended,
                    onRetry = feedVm::loadRecommended
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedTabContent(
    state: FeedTabState,
    listState: LazyListState,
    emptyText: String,
    onRouteClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onLikeClick: (String, Boolean) -> Unit,
    onSaveClick: (String, Boolean) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit
) {
    val isRefreshing = state.isLoading && state.routes.isNotEmpty()
    val pullState = rememberPullToRefreshState()

    LaunchedEffect(listState.canScrollForward) {
        if (!listState.canScrollForward && state.hasMorePages && !state.isLoadingMore && state.routes.isNotEmpty()) {
            onLoadMore()
        }
    }

    when {
        state.isLoading && state.routes.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.error != null && state.routes.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
        }
        state.routes.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            }
        }
        else -> {
            PullToRefreshBox(
                state = pullState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.routes, key = { it.id }) { route ->
                        RouteCard(
                            route = route,
                            onClick = { onRouteClick(route.id) },
                            onLikeClick = { onLikeClick(route.id, route.isLiked) },
                            onSaveClick = { onSaveClick(route.id, route.isSaved) },
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

                    if (state.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            }
                        }
                    }

                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FeedLoginStub(onLoginClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                stringResource(R.string.feed_login_prompt),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp
            )
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text(stringResource(R.string.login_submit))
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
