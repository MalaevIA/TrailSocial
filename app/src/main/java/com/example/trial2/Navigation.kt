package com.trail2

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    data class AIRouteResult(val route: GeneratedRoute) : Screen()
    data class UserProfile(val userId: String) : Screen()
    data class FollowList(val userId: String, val type: FollowListType) : Screen()
    object RouteMapPicker : Screen()
    object Settings : Screen()
}

enum class FollowListType { FOLLOWERS, FOLLOWING }

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
    val waypoints: List<com.trail2.ui.screens.WaypointEntry> = emptyList()
)

@Composable
fun MainAppContent() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Feed) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var routeMapResult by remember { mutableStateOf<RouteMapData?>(null) }
    val builderVm: RouteBuilderViewModel = hiltViewModel()

    val navigateBack: () -> Unit = { currentScreen = bottomTabs[selectedTabIndex].screen }

    when (val screen = currentScreen) {
        is Screen.RouteDetail -> {
            RouteDetailScreen(
                routeId = screen.routeId,
                onBack = navigateBack,
                onAuthorClick = { userId -> currentScreen = Screen.UserProfile(userId) }
            )
            return
        }
        is Screen.AIRouteResult -> {
            RouteResultScreen(
                route = screen.route,
                onBack = {
                    builderVm.resetFully()
                    currentScreen = Screen.AIRoute
                },
                onRebuild = {
                    builderVm.resetFully()
                    currentScreen = Screen.AIRoute
                },
                onSaved = { routeId ->
                    builderVm.resetFully()
                    selectedTabIndex = 0
                    currentScreen = Screen.RouteDetail(routeId)
                }
            )
            return
        }
        is Screen.RouteCreate -> {
            val createVm: com.trail2.ui.viewmodels.RouteCreateViewModel = hiltViewModel()

            // Apply map result when returning from picker
            LaunchedEffect(routeMapResult) {
                routeMapResult?.let { data ->
                    createVm.setRouteCoordinates(
                        data.startLat, data.startLng,
                        data.endLat, data.endLng,
                        data.geometry,
                        data.distanceKm,
                        data.waypoints
                    )
                    routeMapResult = null
                }
            }

            RouteCreateScreen(
                onBack = navigateBack,
                onRouteCreated = { routeId -> currentScreen = Screen.RouteDetail(routeId) },
                onPickRoute = { currentScreen = Screen.RouteMapPicker },
                vm = createVm
            )
            return
        }
        is Screen.RouteMapPicker -> {
            RouteMapPickerScreen(
                onBack = { currentScreen = Screen.RouteCreate },
                onRouteSelected = { startLat, startLng, endLat, endLng, geometry, distanceKm, waypoints ->
                    routeMapResult = RouteMapData(startLat, startLng, endLat, endLng, geometry, distanceKm, waypoints)
                    currentScreen = Screen.RouteCreate
                }
            )
            return
        }
        is Screen.Notifications -> {
            NotificationsScreen(
                onBack = navigateBack,
                onRouteClick = { routeId -> currentScreen = Screen.RouteDetail(routeId) },
                onUserClick = { userId -> currentScreen = Screen.UserProfile(userId) }
            )
            return
        }
        is Screen.UserProfile -> {
            UserProfileScreen(
                userId = screen.userId,
                onBack = navigateBack,
                onRouteClick = { routeId -> currentScreen = Screen.RouteDetail(routeId) },
                onFollowListClick = { userId, type -> currentScreen = Screen.FollowList(userId, type) }
            )
            return
        }
        is Screen.FollowList -> {
            FollowListScreen(
                userId = screen.userId,
                type = screen.type,
                onBack = navigateBack,
                onUserClick = { userId -> currentScreen = Screen.UserProfile(userId) }
            )
            return
        }
        is Screen.Settings -> {
            SettingsScreen(onBack = navigateBack)
            return
        }
        is Screen.Login -> {
            LoginScreen(
                onLoginSuccess = { currentScreen = Screen.Feed },
                onGoToRegister = { }
            )
            return
        }
        else -> {}
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            currentScreen = tab.screen
                        },
                        icon = {
                            Icon(
                                if (selectedTabIndex == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.labelResId)
                            )
                        },
                        label = { Text(stringResource(tab.labelResId)) }
                    )
                }
            }
        }
    ) { _ ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "main_screen"
        ) { screen ->
            when (screen) {
                Screen.Feed -> FeedScreen(
                    onRouteClick = { id -> currentScreen = Screen.RouteDetail(id) },
                    onNotificationsClick = { currentScreen = Screen.Notifications },
                    onCreateRouteClick = { currentScreen = Screen.RouteCreate },
                    onSearchClick = {
                        selectedTabIndex = 1
                        currentScreen = Screen.Explore
                    },
                    onAuthorClick = { userId -> currentScreen = Screen.UserProfile(userId) }
                )
                Screen.Explore -> ExploreScreen(
                    onRouteClick = { id -> currentScreen = Screen.RouteDetail(id) },
                    onUserClick = { userId -> currentScreen = Screen.UserProfile(userId) }
                )
                Screen.AIRoute -> RouteBuilderScreen(
                    onRouteReady = { route -> currentScreen = Screen.AIRouteResult(route) },
                    vm = builderVm
                )
                Screen.Profile -> ProfileScreen(
                    onRouteClick = { id -> currentScreen = Screen.RouteDetail(id) },
                    onFollowListClick = { userId, type -> currentScreen = Screen.FollowList(userId, type) },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
                else -> {}
            }
        }
    }
}
