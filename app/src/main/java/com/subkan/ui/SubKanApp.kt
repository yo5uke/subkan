package com.subkan.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.subkan.ui.cards.CardManagementScreen
import com.subkan.ui.home.HomeScreen
import com.subkan.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val CARDS = "cards"
}

/**
 * One screen deep, twice.
 *
 * There is no navigation bar: the subscription list *is* the app, and Settings and card management
 * are places you visit and come back from. The card tab row is a filter over the one list, not a
 * set of destinations — which is why it is a pager rather than a nav graph.
 */
@Composable
fun SubKanApp(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateUp = { navController.popBackStack() },
                onOpenCardManagement = { navController.navigate(Routes.CARDS) },
            )
        }

        composable(Routes.CARDS) {
            CardManagementScreen(onNavigateUp = { navController.popBackStack() })
        }
    }
}
