package com.myprojects.modules.runningtracker.ui

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.maps.model.LatLng
import com.myprojects.modules.runningtracker.Constants.TAG
import com.myprojects.modules.runningtracker.TrackingUtility
import com.myprojects.modules.runningtracker.ui.theme.RunningTrackerTheme
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel
import com.vmadalin.easypermissions.EasyPermissions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val context = this@MainActivity
    val viewmodel: MainViewmodel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                    //    startOrResumeTrackingService()
                    //viewmodel.startRun()
                }

                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                }

                else -> {
                    // No location access granted.
                    Log.d(TAG, "grant?="+TrackingUtility.hasLocationPermissions(context).toString())

                }
            }
        }
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        if (TrackingUtility.hasLocationPermissions(context)) {

            Log.d(TAG, "granted....." )

        } else {
            Log.d(TAG, "NOT granted....." )
        }

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
       //     Log.d("------------", "host args=${it.arguments.toString()}")
            it.arguments?.getString("id")?.let {
       //         Log.d("------------", "host it=$it")
                RouteComposable(navController = navController, viewmodel, it.toInt())
            }
        }
    }
}


sealed class Routes(val route: String) {
    data object Login : Routes("Login")
    data object Tracking : Routes("Tracking")
    data object Route : Routes("Route")

    fun withArgs(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach { arg -> append("/$arg") }
        }
    }
}