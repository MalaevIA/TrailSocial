package com.trail2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.ui.components.DifficultyBadge
import com.trail2.ui.components.ExploreSkeleton
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
                Spacer(Modifier.height(10.dp))
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
                        Text(stringResource(R.string.explore_popular_regions), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                    }

                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.height(300.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(regions.size.coerceAtMost(6)) { i ->
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
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (uiState.query.isBlank()) stringResource(R.string.explore_all_routes) else stringResource(R.string.explore_results),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(8.dp))
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.BottomStart
    ) {
        // Цветной фон-заглушка
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(color.copy(0.7f), color)))
        )
        // Фото региона (если есть)
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
            // Тёмный градиент поверх фото для читаемости текста
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )
        }
        // Название региона
        Column(modifier = Modifier.padding(10.dp)) {
            Text(region.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ExploreRouteRow(route: com.trail2.data.TrailRoute, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.width(90.dp).height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.fillMaxSize().background(ForestGreen))
                val photoUrl = route.photos.firstOrNull()?.let { routePhotoUrl(it) }
                if (photoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(photoUrl).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                DifficultyBadge(route.difficulty)
            }
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text(route.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                Text(route.region, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📏 ${route.distanceKm}км", fontSize = 11.sp)
                    Text("❤️ ${route.likesCount}", fontSize = 11.sp)
                }
            }
        }
    }
}
