package com.trail2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.draw.blur
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.data.GeoJsonLineString
import com.trail2.ui.components.DifficultyBadge
import com.trail2.ui.components.ExploreSkeleton
import com.trail2.ui.components.RouteMapPreview
import com.trail2.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.trail2.data.Difficulty
import com.trail2.data.RegionInfo
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.util.routePhotoUrl
import com.trail2.ui.viewmodels.ExploreViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onRouteClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    onSeeAllRegions: (List<RegionInfo>, List<String>) -> Unit = { _, _ -> },
    vm: ExploreViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.explore_search), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = ForestGreen)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = vm::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.explore_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = ForestGreen) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
            }
        }

        if (uiState.isLoading && uiState.routes.isEmpty()) {
            ExploreSkeleton()
        } else {
            val isRefreshing = uiState.isLoading && uiState.routes.isNotEmpty()
            val pullState = rememberPullToRefreshState()
            val defaultRegionColors = listOf("2D6A4F", "E76F51", "457B9D", "E63946", "264653", "6D6875")
            val defaultRegions = listOf(
                stringResource(R.string.region_ural),
                stringResource(R.string.region_caucasus),
                stringResource(R.string.region_altai),
                stringResource(R.string.region_kamchatka),
                stringResource(R.string.region_karelia),
                stringResource(R.string.region_siberia)
            ).mapIndexed { i, name -> RegionInfo(name = name) }
            val regions = if (uiState.regions.isNotEmpty()) uiState.regions else defaultRegions

            PullToRefreshBox(
                state = pullState,
                isRefreshing = isRefreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.query.isBlank() && uiState.selectedRegion == null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.explore_popular_regions),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { onSeeAllRegions(regions, defaultRegionColors) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text(stringResource(R.string.filter_all), fontSize = 13.sp, color = ForestGreen)
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = ForestGreen
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    item {
                        val itemCount = regions.size.coerceAtMost(6)
                        val rowCount = (itemCount + 1) / 2
                        val gridHeight = (rowCount * 120 + (rowCount - 1).coerceAtLeast(0) * 10).dp
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.height(gridHeight),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            userScrollEnabled = false
                        ) {
                            items(itemCount) { i ->
                                RegionCard(
                                    region = regions[i],
                                    colorHex = defaultRegionColors[i % defaultRegionColors.size],
                                    onClick = { vm.searchByRegion(regions[i].name) }
                                )
                            }
                        }
                    }
                }

                val selectedRegion = uiState.selectedRegion
                if (selectedRegion != null) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { vm.clearRegion() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = ForestGreen
                                )
                            }
                            Text(
                                selectedRegion,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (uiState.users.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.explore_users), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    items(uiState.users.size) { i ->
                        val user = uiState.users[i]
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onUserClick(user.id) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(user.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("@${user.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (uiState.selectedRegion == null) {
                    item {
                        // Filter chips
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
                        FilterChipsRow(
                            filters = filterLabels,
                            selectedFilter = filterLabels[selectedFilterIndex],
                            onFilterSelected = { filter ->
                                val index = filterLabels.indexOf(filter)
                                vm.setFilter(
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
                        Spacer(Modifier.height(14.dp))
                        // Dynamic section title
                        val sectionTitle = when {
                            uiState.query.isNotBlank() -> stringResource(R.string.explore_results)
                            uiState.filterDifficulty == Difficulty.EASY -> stringResource(R.string.explore_easy_routes)
                            uiState.filterDifficulty == Difficulty.MODERATE -> stringResource(R.string.explore_moderate_routes)
                            uiState.filterDifficulty == Difficulty.HARD -> stringResource(R.string.explore_hard_routes)
                            uiState.filterDifficulty == Difficulty.EXPERT -> stringResource(R.string.explore_expert_routes)
                            else -> stringResource(R.string.explore_all_routes)
                        }
                        Text(sectionTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(2.dp))
                    }
                }

                items(uiState.routes.size) { i ->
                    val route = uiState.routes[i]
                    ExploreRouteRow(route = route, onClick = { onRouteClick(route.id) })
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
            } // end PullToRefreshBox
        }
    }
}

@Composable
fun RegionCard(region: RegionInfo, colorHex: String, onClick: () -> Unit) {
    val color = try { Color(android.graphics.Color.parseColor("#$colorHex")) } catch (_: Exception) { ForestGreen }
    val emoji = when {
        region.name.contains("Урал", ignoreCase = true) -> "⛰️"
        region.name.contains("Кавказ", ignoreCase = true) -> "🏔️"
        region.name.contains("Алтай", ignoreCase = true) -> "🗻"
        region.name.contains("Камчатка", ignoreCase = true) -> "🌋"
        region.name.contains("Карелия", ignoreCase = true) -> "🌲"
        region.name.contains("Сибирь", ignoreCase = true) -> "🌿"
        else -> "🗺️"
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.75f), color)))
            )
            // Photo
            if (region.photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(routePhotoUrl(region.photoUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                )
            }
            // Emoji badge top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 15.sp)
            }
            // Region name bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    region.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun ExploreRouteRow(route: com.trail2.data.TrailRoute, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        val effectiveGeometry = remember(route.id) {
            route.geometry ?: run {
                val lat1 = route.startLat; val lng1 = route.startLng
                val lat2 = route.endLat; val lng2 = route.endLng
                if (lat1 != null && lng1 != null && lat2 != null && lng2 != null)
                    GeoJsonLineString(coordinates = listOf(listOf(lng1, lat1), listOf(lng2, lat2)))
                else null
            }
        }
        val isLocked = route.isPaid && route.isLocked
        Row(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            // Square photo / map preview
            Box(modifier = Modifier.width(120.dp).fillMaxHeight()) {
                val photoUrl = route.photos.firstOrNull()?.let { routePhotoUrl(it) }
                if (photoUrl != null) {
                    Box(modifier = Modifier.fillMaxSize().background(ForestGreen))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                            .then(if (isLocked) Modifier.blur(12.dp) else Modifier)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))
                                )
                            )
                    )
                } else {
                    RouteMapPreview(
                        geometry = effectiveGeometry,
                        modifier = Modifier.fillMaxSize()
                            .then(if (isLocked) Modifier.blur(12.dp) else Modifier)
                    )
                }
                if (isLocked) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(28.dp),
                        tint = Color.White
                    )
                }
            }
            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title + paid badge
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        route.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        lineHeight = 18.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (route.isPaid) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isLocked) MaterialTheme.colorScheme.errorContainer
                                    else ForestGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    if (isLocked) Icons.Outlined.Lock else Icons.Outlined.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = if (isLocked) MaterialTheme.colorScheme.error else ForestGreen
                                )
                                Text(
                                    if (isLocked) stringResource(R.string.paid_route_subtitle) else stringResource(R.string.paid_route),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLocked) MaterialTheme.colorScheme.error else ForestGreen
                                )
                            }
                        }
                    }
                }
                // Region + difficulty badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        route.region,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    DifficultyBadge(route.difficulty)
                }
                // Stats
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RouteStatPill("📏", "${route.distanceKm} км")
                    RouteStatPill("❤️", "${route.likesCount}")
                }
            }
        }
    }
}

@Composable
private fun RouteStatPill(emoji: String, label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(emoji, fontSize = 10.sp)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
