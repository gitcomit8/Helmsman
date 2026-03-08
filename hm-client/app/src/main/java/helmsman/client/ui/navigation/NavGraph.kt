package helmsman.client.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

object Routes {
    const val PAIRING = "pairing"
    const val HOME = "home"
    const val COMMANDS = "commands"
    const val JOBS = "jobs"
    const val SERVERS = "servers"
    const val CONSOLE = "console/{commandId}"

    fun console(commandId: String) = "console/$commandId"
}

@Composable
fun HelmsmanNavGraph(
    navController: NavHostController,
    isAuthenticated: Boolean,
    api: HelmsmanApi,
    authRepository: AuthRepository,
    db: AppDatabase
) {
    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) Routes.HOME else Routes.PAIRING
    ) {
        composable(Routes.PAIRING) {
            PairingScreen(
                api = api,
                authRepository = authRepository,
                onPaired = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.PAIRING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            DashboardScreen(
                api = api,
                navController = navController
            )
        }

        composable(Routes.COMMANDS) {
            CommandsScreen(
                api = api,
                onStreamCommand = { commandId ->
                    navController.navigate(Routes.console(commandId))
                },
                navController = navController
            )
        }

        composable(Routes.JOBS) {
            JobsScreen(
                api = api,
                navController = navController
            )
        }

        composable(Routes.SERVERS) {
            ServersScreen(
                api = api,
                navController = navController
            )
        }

        composable(
            route = Routes.CONSOLE,
            arguments = listOf(navArgument("commandId") { type = NavType.StringType })
        ) { backStackEntry ->
            val commandId = backStackEntry.arguments?.getString("commandId") ?: return@composable
            ConsoleScreen(
                commandId = commandId,
                db = db,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
