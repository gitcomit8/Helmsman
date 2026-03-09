package helmsman.client.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import helmsman.client.data.local.AppDatabase
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.AuthRepository
import helmsman.client.ui.commands.CommandsScreen
import helmsman.client.ui.console.ConsoleScreen
import helmsman.client.ui.dashboard.DashboardScreen
import helmsman.client.ui.jobs.JobsScreen
import helmsman.client.ui.pairing.PairingScreen
import helmsman.client.ui.servers.ServersScreen
import androidx.navigation.NavGraph.Companion.findStartDestination

private const val ROUTE_PAIRING  = "pairing"
private const val ROUTE_MAIN     = "main"
private const val ROUTE_HOME     = "home"
private const val ROUTE_COMMANDS = "commands"
private const val ROUTE_JOBS     = "jobs"
private const val ROUTE_SERVERS  = "servers"
private const val ROUTE_CONSOLE  = "console/{commandId}"
private fun consoleRoute(id: String) = "console/$id"

data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    BottomTab(ROUTE_HOME,     "Home",     Icons.Default.Home),
    BottomTab(ROUTE_COMMANDS, "Commands", Icons.Default.Terminal),
    BottomTab(ROUTE_JOBS,     "Jobs",     Icons.Default.Schedule),
    BottomTab(ROUTE_SERVERS,  "Servers",  Icons.Default.Dns),
)

@Composable
fun HelmsmanNavGraph(
    navController: NavHostController,
    isAuthenticated: Boolean,
    api: HelmsmanApi,
    authRepository: AuthRepository,
    db: AppDatabase
) {
    NavHost(navController, startDestination = if (isAuthenticated) ROUTE_MAIN else ROUTE_PAIRING) {
        composable(ROUTE_PAIRING) {
            PairingScreen(api = api, authRepository = authRepository, onPaired = {
                navController.navigate(ROUTE_MAIN) { popUpTo(ROUTE_PAIRING) { inclusive = true } }
            })
        }
        composable(ROUTE_MAIN) {
            MainShell(
                api = api,
                db = db,
                onOpenConsole = { id -> navController.navigate(consoleRoute(id)) }
            )
        }
        composable(
            route     = ROUTE_CONSOLE,
            arguments = listOf(navArgument("commandId") { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString("commandId") ?: return@composable
            ConsoleScreen(commandId = id, db = db, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainShell(
    api: HelmsmanApi,
    db: AppDatabase,
    onOpenConsole: (String) -> Unit
) {
    val innerNav = rememberNavController()
    val backStack by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            innerNav.navigate(tab.route) {
                                popUpTo(innerNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(innerNav, startDestination = ROUTE_HOME) {
            composable(ROUTE_HOME)     { DashboardScreen(api = api, contentPadding = padding) }
            composable(ROUTE_COMMANDS) { CommandsScreen(api = api, onStreamCommand = onOpenConsole, contentPadding = padding) }
            composable(ROUTE_JOBS)     { JobsScreen(api = api, contentPadding = padding) }
            composable(ROUTE_SERVERS)  { ServersScreen(api = api, contentPadding = padding) }
        }
    }
}
