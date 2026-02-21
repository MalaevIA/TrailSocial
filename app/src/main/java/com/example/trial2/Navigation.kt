package com.trail2

// ══════════════════════════════════════════════════════════════
// Файл: Navigation.kt  (ПОЛНАЯ ЗАМЕНА вашего текущего файла)
//
// Добавлена вкладка "AI-маршрут" и логика отображения результата.
// ══════════════════════════════════════════════════════════════

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.ai_route.GeneratedRoute
import com.trail2.onboarding.OnboardingRepository
import com.trail2.ui.screens.*
import com.trail2.ui.screens.onboarding.OnboardingNavGraph

// ── Экраны ───────────────────────────────────────────────────

sealed class Screen {
    object Feed       : Screen()
    object Explore    : Screen()
    object AIRoute    : Screen()   // ← новая вкладка
    object Profile    : Screen()
    data class RouteDetail(val routeId: String)   : Screen()
    data class AIRouteResult(val route: GeneratedRoute) : Screen()
}

// ── Вкладки нижней панели ─────────────────────────────────────

data class BottomTab(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomTabs = listOf(
    BottomTab(Screen.Feed,    "Лента",     Icons.Filled.Home,       Icons.Outlined.Home),
    BottomTab(Screen.Explore, "Поиск",     Icons.Filled.Search,     Icons.Outlined.Search),
    BottomTab(Screen.AIRoute, "AI-маршрут",Icons.Filled.AutoAwesome,Icons.Outlined.AutoAwesome),
    BottomTab(Screen.Profile, "Профиль",   Icons.Filled.Person,     Icons.Outlined.Person)
)

// ── Главная точка входа ────────────────────────────────────────

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val repo = remember { OnboardingRepository(context) }
    val isCompleted by repo.isOnboardingCompleted
        .collectAsStateWithLifecycle(initialValue = null)

    when (isCompleted) {
        null  -> { /* splash — DataStore ещё грузится */ }
        false -> OnboardingNavGraph(onFinished = { /* recompose сработает сам */ })
        true  -> MainAppContent()
    }
}

// ── Основное приложение (после онбординга) ────────────────────

@Composable
fun MainAppContent() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Feed) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Если текущий экран — не вкладка (детали маршрута, результат AI) — показываем без Scaffold
    when (val screen = currentScreen) {

        is Screen.RouteDetail -> {
            FeedDetailScreen(               // ← ваш существующий RouteDetailScreen
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

        else -> { /* продолжаем → рисуем BottomNav */ }
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
    ) { padding ->
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

// ── Алиас для детального экрана существующего маршрута ──────────
// Если у вас файл называется RouteDetailScreen, просто замените
// FeedDetailScreen на RouteDetailScreen и удейте этот алиас.
@Composable
private fun FeedDetailScreen(routeId: String, onBack: () -> Unit) {
    // TODO: замените на ваш реальный RouteDetailScreen(routeId, onBack)
    RouteDetailScreen(routeId = routeId, onBack = onBack)
}