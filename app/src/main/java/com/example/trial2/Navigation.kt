package com.trail2

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.ui.theme.ForestGreen
import com.trail2.ai_route.GeneratedRoute
import com.trail2.ai_route.RouteBuilderViewModel
import com.trail2.onboarding.OnboardingViewModel
import com.trail2.ui.screens.*
import com.trail2.ui.screens.auth.LoginScreen
import com.trail2.ui.screens.onboarding.OnboardingNavGraph
import com.trail2.ui.viewmodels.AuthViewModel

sealed class Screen {
    object Feed : Screen()
    object Explore : Screen()
    object AIRoute : Screen()
    object Profile : Screen()
    object Login : Screen()
    object RouteCreate : Screen()
    object Notifications : Screen()
    data class RouteDetail(val routeId: String) : Screen()
    data class RouteEdit(val routeId: String) : Screen()
    data class AIRouteResult(val route: GeneratedRoute) : Screen()
    data class UserProfile(val userId: String) : Screen()
    data class FollowList(val userId: String, val type: FollowListType) : Screen()
    object RouteMapPicker : Screen()
    object Settings : Screen()
    object AdminPanel : Screen()
}

enum class FollowListType { FOLLOWERS, FOLLOWING }

@Composable
private fun TrailBottomBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex

                val pillScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.4f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "pill_scale_$index"
                )
                val pillAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(180),
                    label = "pill_alpha_$index"
                )
                val iconOffsetY by animateDpAsState(
                    targetValue = if (isSelected) (-2).dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "icon_y_$index"
                )
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    animationSpec = tween(200),
                    label = "icon_color_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    // Animated green pill behind icon
                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 36.dp)
                            .graphicsLayer {
                                scaleX = pillScale
                                scaleY = pillScale
                                alpha = pillAlpha
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(ForestGreen)
                    )
                    // Icon on top of pill
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelResId),
                        tint = iconColor,
                        modifier = Modifier
                            .size(24.dp)
                            .offset(y = iconOffsetY)
                    )
                }
            }
        }
    }
}

data class BottomTab(
    val screen: Screen,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomTabs = listOf(
    BottomTab(Screen.Feed, R.string.tab_feed, Icons.Filled.Home, Icons.Outlined.Home),
    BottomTab(Screen.Explore, R.string.tab_explore, Icons.Filled.Search, Icons.Outlined.Search),
    BottomTab(Screen.AIRoute, R.string.tab_ai_route, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    BottomTab(Screen.Profile, R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun AppNavigation(
    onboardingVm: OnboardingViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel()
) {
    val isOnboardingCompleted by onboardingVm.isOnboardingCompleted.collectAsStateWithLifecycle()
    val isLoggedIn by authVm.isLoggedIn.collectAsStateWithLifecycle()

    when {
        isOnboardingCompleted == null -> {
            // Splash — DataStore loading
        }
        isOnboardingCompleted == false -> {
            OnboardingNavGraph(
                onFinished = { },
                onGoToLogin = { onboardingVm.skipOnboarding() },
                viewModel = onboardingVm
            )
        }
        !isLoggedIn -> {
            LoginScreen(
                onLoginSuccess = { },
                onGoToRegister = {
                    onboardingVm.logout()
                }
            )
        }
        else -> {
            MainAppContent()
        }
    }
}

private data class RouteMapData(
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val geometry: List<List<Double>>,
    val distanceKm: Double,
    val durationMinutes: Int = 0,
    val waypoints: List<com.trail2.ui.screens.WaypointEntry> = emptyList()
)

@Composable
fun MainAppContent() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Feed) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var routeMapResult by remember { mutableStateOf<RouteMapData?>(null) }
    var editingRouteId by remember { mutableStateOf<String?>(null) }
    val builderVm: RouteBuilderViewModel = hiltViewModel()

    val navigateBack: () -> Unit = { currentScreen = bottomTabs[selectedTabIndex].screen }

    Box(Modifier.fillMaxSize()) {
        // ── Layer 1: Scaffold со статичным BottomBar + анимация между табами ──
        Scaffold(
            bottomBar = {
                TrailBottomBar(
                    tabs = bottomTabs,
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { index ->
                        selectedTabIndex = index
                        currentScreen = bottomTabs[index].screen
                    }
                )
            }
        ) { _ ->
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { if (forward) it / 5 else -it / 5 } +
                            fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { if (forward) -it / 5 else it / 5 } +
                                    fadeOut(tween(220)))
                },
                label = "tab_content"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> FeedScreen(
                        onRouteClick = { id -> currentScreen = Screen.RouteDetail(id) },
                        onNotificationsClick = { currentScreen = Screen.Notifications },
                        onCreateRouteClick = { currentScreen = Screen.RouteCreate },
                        onSearchClick = { selectedTabIndex = 1; currentScreen = Screen.Explore },
                        onAuthorClick = { userId -> currentScreen = Screen.UserProfile(userId) }
                    )
                    1 -> ExploreScreen(
                        onRouteClick = { id -> currentScreen = Screen.RouteDetail(id) },
                        onUserClick = { userId -> currentScreen = Screen.UserProfile(userId) }
                    )
                    2 -> RouteBuilderScreen(
                        onRouteReady = { route -> currentScreen = Screen.AIRouteResult(route) },
                        vm = builderVm
                    )
                    3 -> ProfileScreen(
                        onRouteClick = { id -> currentScreen = Screen.RouteDetail(id) },
                        onFollowListClick = { userId, type -> currentScreen = Screen.FollowList(userId, type) },
                        onSettingsClick = { currentScreen = Screen.Settings }
                    )
                    else -> {}
                }
            }
        }

        // ── Layer 2: полноэкранные экраны поверх Scaffold (бар не трогается) ──
        AnimatedContent(
            targetState = currentScreen.takeIf { tabScreenIndex(it) < 0 },
            transitionSpec = {
                when {
                    targetState != null && initialState == null -> {
                        // Первый push с таба — приезжает справа
                        (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it } +
                                fadeIn(tween(200))) togetherWith fadeOut(tween(0))
                    }
                    targetState != null -> {
                        // Push внутри полноэкранных (RouteDetail → UserProfile и т.п.)
                        (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it } +
                                fadeIn(tween(200))) togetherWith
                                (slideOutHorizontally(tween(380, easing = FastOutSlowInEasing)) { -it / 6 } +
                                        fadeOut(tween(200)))
                    }
                    else -> {
                        // Pop обратно на таб — уезжает вправо
                        fadeIn(tween(0)) togetherWith
                                (slideOutHorizontally(tween(350, easing = FastOutSlowInEasing)) { it } +
                                        fadeOut(tween(250)))
                    }
                }
            },
            label = "overlay_nav"
        ) { overlayScreen ->
            if (overlayScreen != null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (overlayScreen) {
                        is Screen.RouteDetail -> RouteDetailScreen(
                            routeId = overlayScreen.routeId,
                            onBack = navigateBack,
                            onAuthorClick = { userId -> currentScreen = Screen.UserProfile(userId) },
                            onEditRoute = { routeId -> currentScreen = Screen.RouteEdit(routeId) },
                            onClonedRoute = { routeId -> currentScreen = Screen.RouteDetail(routeId) }
                        )
                        is Screen.AIRouteResult -> RouteResultScreen(
                            route = overlayScreen.route,
                            onBack = { builderVm.resetFully(); currentScreen = Screen.AIRoute },
                            onRebuild = { builderVm.resetFully(); currentScreen = Screen.AIRoute },
                            onSaved = { routeId ->
                                builderVm.resetFully()
                                selectedTabIndex = 0
                                currentScreen = Screen.RouteDetail(routeId)
                            },
                            onEditRoute = { routeId ->
                                builderVm.resetFully()
                                currentScreen = Screen.RouteEdit(routeId)
                            }
                        )
                        is Screen.RouteEdit -> {
                            val createVm: com.trail2.ui.viewmodels.RouteCreateViewModel = hiltViewModel()
                            LaunchedEffect(overlayScreen.routeId) { createVm.loadForEdit(overlayScreen.routeId) }
                            LaunchedEffect(routeMapResult) {
                                routeMapResult?.let { data ->
                                    createVm.setRouteCoordinates(data.startLat, data.startLng, data.endLat, data.endLng, data.geometry, data.distanceKm, data.durationMinutes, data.waypoints)
                                    routeMapResult = null
                                }
                            }
                            RouteCreateScreen(
                                onBack = navigateBack,
                                onRouteCreated = { routeId -> currentScreen = Screen.RouteDetail(routeId) },
                                onPickRoute = { editingRouteId = overlayScreen.routeId; currentScreen = Screen.RouteMapPicker },
                                vm = createVm
                            )
                        }
                        is Screen.RouteCreate -> {
                            val createVm: com.trail2.ui.viewmodels.RouteCreateViewModel = hiltViewModel()
                            LaunchedEffect(Unit) { if (routeMapResult == null) createVm.resetForm() }
                            LaunchedEffect(routeMapResult) {
                                routeMapResult?.let { data ->
                                    createVm.setRouteCoordinates(data.startLat, data.startLng, data.endLat, data.endLng, data.geometry, data.distanceKm, data.durationMinutes, data.waypoints)
                                    routeMapResult = null
                                }
                            }
                            RouteCreateScreen(
                                onBack = navigateBack,
                                onRouteCreated = { routeId -> currentScreen = Screen.RouteDetail(routeId) },
                                onPickRoute = { editingRouteId = null; currentScreen = Screen.RouteMapPicker },
                                vm = createVm
                            )
                        }
                        is Screen.RouteMapPicker -> {
                            val returnToEdit = editingRouteId
                            RouteMapPickerScreen(
                                onBack = { currentScreen = if (returnToEdit != null) Screen.RouteEdit(returnToEdit) else Screen.RouteCreate },
                                onRouteSelected = { startLat, startLng, endLat, endLng, geometry, distanceKm, durationMinutes, waypoints ->
                                    routeMapResult = RouteMapData(startLat, startLng, endLat, endLng, geometry, distanceKm, durationMinutes, waypoints)
                                    currentScreen = if (returnToEdit != null) Screen.RouteEdit(returnToEdit) else Screen.RouteCreate
                                }
                            )
                        }
                        is Screen.Notifications -> NotificationsScreen(
                            onBack = navigateBack,
                            onRouteClick = { routeId -> currentScreen = Screen.RouteDetail(routeId) },
                            onUserClick = { userId -> currentScreen = Screen.UserProfile(userId) }
                        )
                        is Screen.UserProfile -> UserProfileScreen(
                            userId = overlayScreen.userId,
                            onBack = navigateBack,
                            onRouteClick = { routeId -> currentScreen = Screen.RouteDetail(routeId) },
                            onFollowListClick = { userId, type -> currentScreen = Screen.FollowList(userId, type) }
                        )
                        is Screen.FollowList -> FollowListScreen(
                            userId = overlayScreen.userId,
                            type = overlayScreen.type,
                            onBack = navigateBack,
                            onUserClick = { userId -> currentScreen = Screen.UserProfile(userId) }
                        )
                        is Screen.Settings -> {
                            val profileVm: com.trail2.ui.viewmodels.ProfileViewModel = hiltViewModel()
                            val profileState by profileVm.uiState.collectAsStateWithLifecycle()
                            SettingsScreen(
                                onBack = navigateBack,
                                isAdmin = profileState.user?.isAdmin == true,
                                onAdminPanelClick = { currentScreen = Screen.AdminPanel }
                            )
                        }
                        is Screen.AdminPanel -> AdminPanelScreen(
                            onBack = navigateBack,
                            onUserClick = { userId -> currentScreen = Screen.UserProfile(userId) },
                            onRouteClick = { routeId -> currentScreen = Screen.RouteDetail(routeId) }
                        )
                        is Screen.Login -> LoginScreen(
                            onLoginSuccess = { currentScreen = Screen.Feed },
                            onGoToRegister = { }
                        )
                        else -> {}
                    }
                }
            }
        }
    }
}

private fun tabScreenIndex(screen: Screen): Int = when (screen) {
    Screen.Feed -> 0
    Screen.Explore -> 1
    Screen.AIRoute -> 2
    Screen.Profile -> 3
    else -> -1
}
