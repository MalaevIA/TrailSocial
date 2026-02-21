package com.trail2.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.trail2.ui.screens.*

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Feed : Screen("feed", "Лента", Icons.Filled.Home, Icons.Outlined.Home)
    object Explore : Screen("explore", "Поиск", Icons.Filled.Search, Icons.Outlined.Search)
    object Create : Screen("create", "Добавить", Icons.Filled.AddCircle, Icons.Outlined.AddCircle)
    object Saved : Screen("saved", "Сохранённые", Icons.Filled.AccountBox, Icons.Outlined.AccountBox)
    object Profile : Screen("profile", "Профиль", Icons.Filled.Person, Icons.Outlined.Person)
}

val bottomNavItems = listOf(
    Screen.Feed, Screen.Explore, Screen.Create, Screen.Saved, Screen.Profile
)

@Composable
fun TrailSocialApp() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (currentRoute == screen.route) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Feed.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Feed.route) {
                FeedScreen(onRouteClick = { routeId ->
                    navController.navigate("route/$routeId")
                })
            }
            composable(Screen.Explore.route) {
                ExploreScreen(
                    onRouteClick = { routeId ->
                        navController.navigate("route/$routeId")
                    }
                )
            }
            //composable(Screen.Create.route) { CreateRouteScreen() }
            //composable(Screen.Saved.route) { SavedScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable("route/{routeId}") { backStack ->
                val routeId = backStack.arguments?.getString("routeId") ?: return@composable
                RouteDetailScreen(routeId = routeId, onBack = { navController.popBackStack() })
            }
        }
    }
}
