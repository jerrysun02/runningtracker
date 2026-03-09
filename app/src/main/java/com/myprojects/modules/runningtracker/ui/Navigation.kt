package com.myprojects.modules.runningtracker.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myprojects.modules.runningtracker.ui.viewmodel.TrackingViewmodel

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val viewmodel: TrackingViewmodel = viewModel()

    NavHost(navController = navController, startDestination = Routes.Login.route) {
        composable(Routes.Login.route) {
            LoginComposable(navController = navController)
        }
        composable(Routes.Runs.route) {
            RunsComposable(navController = navController, viewmodel)
        }
        composable(Routes.Tracking.route) {
            MapComposable(navController = navController, viewmodel)
        }
        composable(
            route = Routes.Route.route + "/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    defaultValue = "Fahad"
                    nullable = true
                }
            )
        ) { it ->
            it.arguments?.getString("id")?.let {
                RouteComposable(navController = navController, viewmodel, it.toInt())
            }
        }
    }
}

sealed class Routes(val route: String) {
    data object Login : Routes("Login")
    data object Runs : Routes("Runs")
    data object Tracking : Routes("Tracking")
    data object Route : Routes("Route")

    fun withArgs(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach { arg -> append("/$arg") }
        }
    }
}
