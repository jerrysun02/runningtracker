package com.myprojects.modules.runningtracker.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel

@Composable
fun StartComposable(navController: NavController) {
    Button(
        onClick = { navController.navigate(route = Routes.Tracking.route) }
    ) {
        Text("Start")
    }
}

sealed class Routes(val route: String) {
    object Login : Routes("Login")
    object Tracking : Routes("Tracking")
}