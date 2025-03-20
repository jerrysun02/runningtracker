package com.myprojects.modules.runningtracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myprojects.modules.runningtracker.ui.theme.RunningTrackerTheme
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    val viewmodel: MainViewmodel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RunningTrackerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier.padding(it)
                    ) {
                        Navigation()
                    }
                }
            }
        }
    }
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val viewmodel: MainViewmodel = viewModel()

    NavHost(navController = navController, startDestination = Routes.Login.route) {
        composable(Routes.Login.route) {
            LoginComposable(navController = navController, viewmodel)
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