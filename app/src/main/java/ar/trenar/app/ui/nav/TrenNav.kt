package ar.trenar.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ar.trenar.app.ui.board.BoardScreen
import ar.trenar.app.ui.home.HomeScreen
import ar.trenar.app.ui.search.SearchScreen
import ar.trenar.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val BOARD = "board/{stationId}"
    fun board(stationId: Int) = "board/$stationId"
}

@Composable
fun TrenNavHost(
    navController: NavHostController,
    startStationId: Int?,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenStation = { navController.navigate(Routes.board(it)) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenStation = { navController.navigate(Routes.board(it)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.BOARD,
            arguments = listOf(navArgument("stationId") { type = NavType.IntType }),
        ) { entry ->
            val stationId = entry.arguments?.getInt("stationId") ?: return@composable
            BoardScreen(
                stationId = stationId,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME)
                    }
                },
            )
        }
    }

    // Deep link from notification / widget: open the board once.
    if (startStationId != null) {
        androidx.compose.runtime.LaunchedEffect(startStationId) {
            navController.navigate(Routes.board(startStationId))
        }
    }
}
