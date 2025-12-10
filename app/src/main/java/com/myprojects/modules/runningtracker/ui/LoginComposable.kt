package com.myprojects.modules.runningtracker.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.myprojects.modules.runningtracker.ui.viewmodel.TrackingViewmodel
import kotlinx.coroutines.launch

@Composable
fun LoginComposable(navController: NavController) {
    val context = LocalContext.current
    val activity = context as Activity
    val locationPermissionsGranted =
        remember { mutableStateOf(areLocationPermissionsAlreadyGranted(context)) }
    val shouldShowPermissionRationale = remember {
        mutableStateOf(
            shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val shouldDirectUserToApplicationSettings = remember {
        mutableStateOf(false)
    }

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            locationPermissionsGranted.value =
                permissions.values.reduce { acc, isPermissionGranted ->
                    acc && isPermissionGranted
                }
            if (!locationPermissionsGranted.value) {
                shouldShowPermissionRationale.value =
                    shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
            } else {
                navController.navigate(Routes.Runs.route)
            }
            shouldDirectUserToApplicationSettings.value =
                !shouldShowPermissionRationale.value && !locationPermissionsGranted.value
        })

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, effect = {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START &&
                !locationPermissionsGranted.value &&
                !shouldShowPermissionRationale.value
            ) {
                //         locationPermissionLauncher.launch(locationPermissions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    )

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = {
        SnackbarHost(hostState = snackbarHostState)
    }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = "",
                onValueChange = { },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier
                    .padding(3.dp)
            )
            TextField(
                value = "",
                onValueChange = { },
                label = { Text("Year") },
                singleLine = true,
                modifier = Modifier
                    .padding(3.dp)
            )
            Button(
                onClick = {
                    if (!locationPermissionsGranted.value
                        && !shouldShowPermissionRationale.value
                    ) {
                        locationPermissionLauncher.launch(locationPermissions)
                    }
                }
            ) {
                Text("Log In")
            }
        }
        if (locationPermissionsGranted.value) {
            navController.navigate(Routes.Runs.route)
        }
        if (shouldShowPermissionRationale.value) {
            LaunchedEffect(Unit) {
                scope.launch {
                    val userAction = snackbarHostState.showSnackbar(
                        message = "Please authorize location permissions",
                        actionLabel = "Approve",
                        duration = SnackbarDuration.Indefinite,
                        withDismissAction = true
                    )
                    when (userAction) {
                        SnackbarResult.ActionPerformed -> {
                            shouldShowPermissionRationale.value = false
                            locationPermissionLauncher.launch(locationPermissions)
                        }

                        SnackbarResult.Dismissed -> {
                            shouldShowPermissionRationale.value = false
                        }
                    }
                }
            }
        }
        if (shouldDirectUserToApplicationSettings.value) {
            openApplicationSettings(context)
        }
    }
}

fun areLocationPermissionsAlreadyGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

fun openApplicationSettings(context: Context) {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).also {
        context.startActivity(it)
    }
}