package ar.trenar.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import ar.trenar.app.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ar.trenar.app.ui.alertas.AlertasScreen
import ar.trenar.app.ui.board.BoardScreen
import ar.trenar.app.ui.common.BrandBar
import ar.trenar.app.ui.cercanas.CercanasScreen
import ar.trenar.app.ui.favoritos.FavoritosScreen
import ar.trenar.app.ui.mapa.MapaScreen
import ar.trenar.app.ui.search.SearchScreen
import ar.trenar.app.ui.settings.SettingsScreen

object Dest {
    const val CERCANAS = "cercanas"
    const val FAVORITOS = "favoritos"
    const val MAPA = "mapa"
    const val ALERTAS = "alertas"
    const val AJUSTES = "ajustes"
    const val SEARCH = "search"
    const val BOARD = "board/{stationId}"
    fun board(stationId: Int) = "board/$stationId"
}

private data class TabItem(val route: String, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Dest.CERCANAS, R.string.nav_cercanas, Icons.Filled.NearMe),
    TabItem(Dest.FAVORITOS, R.string.nav_favoritos, Icons.Filled.Star),
    TabItem(Dest.MAPA, R.string.nav_mapa, Icons.Filled.Map),
    TabItem(Dest.ALERTAS, R.string.nav_alertas, Icons.Filled.NotificationsActive),
    TabItem(Dest.AJUSTES, R.string.nav_ajustes, Icons.Filled.Settings),
)
private val tabRoutes = tabs.map { it.route }.toSet()

@Composable
fun TrenRoot(startStationId: Int?) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBar = currentRoute in tabRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showBar) BrandBar()
        },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    val currentDest = backStackEntry?.destination
                    tabs.forEach { tab ->
                        val selected = currentDest?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Dest.CERCANAS,
            modifier = Modifier.padding(padding),
        ) {
            composable(Dest.CERCANAS) {
                CercanasScreen(
                    onOpenStation = { nav.navigate(Dest.board(it)) },
                    onOpenSearch = { nav.navigate(Dest.SEARCH) },
                )
            }
            composable(Dest.FAVORITOS) {
                FavoritosScreen(onOpenStation = { nav.navigate(Dest.board(it)) })
            }
            composable(Dest.MAPA) {
                MapaScreen(onOpenStation = { nav.navigate(Dest.board(it)) })
            }
            composable(Dest.ALERTAS) {
                AlertasScreen()
            }
            composable(Dest.AJUSTES) {
                SettingsScreen()
            }
            composable(Dest.SEARCH) {
                SearchScreen(
                    onBack = { nav.popBackStack() },
                    onOpenStation = { nav.navigate(Dest.board(it)) },
                )
            }
            composable(
                route = Dest.BOARD,
                arguments = listOf(navArgument("stationId") { type = NavType.IntType }),
            ) { entry ->
                val stationId = entry.arguments?.getInt("stationId") ?: return@composable
                BoardScreen(stationId = stationId, onBack = { nav.popBackStack() })
            }
        }
    }

    if (startStationId != null) {
        LaunchedEffect(startStationId) {
            nav.navigate(Dest.board(startStationId))
        }
    }
}
