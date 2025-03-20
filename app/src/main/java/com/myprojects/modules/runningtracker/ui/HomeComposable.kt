package com.myprojects.modules.runningtracker.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.myprojects.modules.runningtracker.ui.Routes.Route
import com.myprojects.modules.runningtracker.Constants.TAG
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel
import kotlinx.coroutines.launch

@Composable
fun HomeComposable(navController: NavController, viewModel: MainViewmodel) {
    val context = LocalContext.current
    val activity = context as Activity
    var locationPermissionsGranted =
        remember { mutableStateOf(areLocationPermissionsAlreadyGranted(context)) }
    var shouldShowPermissionRationale = remember {
        mutableStateOf(
            shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    var shouldDirectUserToApplicationSettings = remember {
        mutableStateOf(false)
    }

 /*   var currentPermissionsStatus = remember {
        mutableStateOf(
            decideCurrentPermissionStatus(
                locationPermissionsGranted,
                shouldShowPermissionRationale
            )
        )
    }*/

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
                Log.d(TAG, "granted...")
                navController.navigate(Routes.Login.route)
            }
            shouldDirectUserToApplicationSettings.value =
                !shouldShowPermissionRationale.value && !locationPermissionsGranted.value
   /*         currentPermissionsStatus.value = decideCurrentPermissionStatus(
                locationPermissionsGranted,
                shouldShowPermissionRationale
            )*/
        })

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, effect = {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START &&
                !locationPermissionsGranted.value &&
                !shouldShowPermissionRationale.value
            ) {
                locationPermissionLauncher.launch(locationPermissions)
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
    }) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxWidth(),
                text = "Location Permissions",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.padding(20.dp))
        /*    Text(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxWidth(),
                text = "Current Permission Status: $currentPermissionsStatus",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )*/
        }
        if (locationPermissionsGranted.value) {
            navController.navigate(Routes.Login.route)
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
/*
fun decideCurrentPermissionStatus(
    locationPermissionsGranted: MutableState<Boolean>,
    shouldShowPermissionRationale: MutableState<Boolean>
): String {
    return if (locationPermissionsGranted.value) "Granted"
    else if (shouldShowPermissionRationale.value) "Rejected"
    else "Denied"
}*/