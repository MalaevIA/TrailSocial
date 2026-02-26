package com.trail2

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.ai_route.GeneratedRoute
import com.trail2.onboarding.OnboardingViewModel
import com.trail2.ui.screens.*
import com.trail2.ui.screens.onboarding.OnboardingNavGraph

// ── Экраны ────────────────────────────────────────────────

sealed class Screen {
    object Feed       : Screen()
    object Explore    : Screen()
    object AIRoute    : Screen()
    object Profile    : Screen()
    data class RouteDetail(val routeId: String) : Screen()
    data class AIRouteResult(val route: GeneratedRoute) : Screen()
}

// ── Вкладки нижней панели ─────────────────────────────────

data class BottomTab(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomTabs = listOf(
    BottomTab(Screen.Feed,    "Лента",      Icons.Filled.Home,        Icons.Outlined.Home),
    BottomTab(Screen.Explore, "Поиск",      Icons.Filled.Search,      Icons.Outlined.Search),
    BottomTab(Screen.AIRoute, "AI-маршрут", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    BottomTab(Screen.Profile, "Профиль",    Icons.Filled.Person,      Icons.Outlined.Person)
)

// ── Главная точка входа ───────────────────────────────────

@Composable
fun AppNavigation(
    vm: OnboardingViewModel = hiltViewModel()
) {
    val isCompleted by vm.isOnboardingCompleted.collectAsStateWithLifecycle()

    when (isCompleted) {
        null  -> { /* Splash — DataStore ещё загружается */ }
        true  -> MainAppContent()
        false -> OnboardingNavGraph(
            onFinished = { /* recompose сработает через StateFlow */ },
            viewModel  = vm
        )
    }
}

// ── Основное приложение ───────────────────────────────────

@Composable
fun MainAppContent() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Feed) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    when (val screen = currentScreen) {
        is Screen.RouteDetail -> {
            RouteDetailScreen(
                routeId = screen.routeId,
                onBack  = { currentScreen = bottomTabs[selectedTabIndex].screen }
            )
            return
        }
        is Screen.AIRouteResult -> {
            RouteResultScreen(
                route     = screen.route,
                onBack    = { currentScreen = Screen.AIRoute },
                onRebuild = { currentScreen = Screen.AIRoute }
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
                        selected  = selectedTabIndex == index,
                        onClick   = {
                            selectedTabIndex = index
                            currentScreen    = tab.screen
                        },
                        icon = {
                            Icon(
                                if (selectedTabIndex == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
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
                Screen.Feed    -> FeedScreen(
                    onRouteClick = { id -> currentScreen = Screen.RouteDetail(id) }
                )
                Screen.Explore -> ExploreScreen(
                    onRouteClick = { id -> currentScreen = Screen.RouteDetail(id) }
                )
                Screen.AIRoute -> RouteBuilderScreen(
                    onRouteReady = { route -> currentScreen = Screen.AIRouteResult(route) }
                )
                Screen.Profile -> ProfileScreen()
                else -> {}
            }
        }
    }
}
