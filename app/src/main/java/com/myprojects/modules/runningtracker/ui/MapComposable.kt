package com.myprojects.modules.runningtracker.ui

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.myprojects.modules.runningtracker.Constants.ACTION_PAUSE_SERVICE
import com.myprojects.modules.runningtracker.Constants.ACTION_START_OR_RESUME_SERVICE
import com.myprojects.modules.runningtracker.Constants.ACTION_STOP_SERVICE
import com.myprojects.modules.runningtracker.services.TrackingService
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel
import kotlinx.coroutines.launch

@Composable
fun MapComposable(navController: NavController, viewmodel: MainViewmodel) {
    val coroutineScope = rememberCoroutineScope()

    val atasehir = LatLng(49.2510221, -123.00441)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(atasehir/*locationFlow!!*/, 15f)
    }
    val polyLines by viewmodel.polyLineFlow.collectAsState()
    val location by viewmodel.locationFlow.collectAsState()
    val isTracking = TrackingService.isTracking.collectAsState()
    var text2 by remember { mutableStateOf("Stop 0") }
    val context = LocalContext.current

    fun startOrResumeTrackingService() {
        Intent(context, TrackingService::class.java).also {
            it.action = ACTION_START_OR_RESUME_SERVICE
            context.startService(it)
        }
    }

    fun pauseTrackingService() =
        Intent(context, TrackingService::class.java).also {
            it.action = ACTION_PAUSE_SERVICE
            context.startService(it)
        }

    fun stopTrackingService() =
        Intent(context, TrackingService::class.java).also {
            it.action = ACTION_STOP_SERVICE
            context.startService(it)
        }


    LaunchedEffect(Unit) {
        viewmodel.getLocationFlow()
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = cameraPositionState.position.target),
            title = ""
        )

        location?.let {
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(location!!, 12f)
            Log.d("------------", "camera=${location}")
        }


        if (polyLines.isNotEmpty()) {
            Log.d("------------", "compose lines=$polyLines")
            Marker(
                state = MarkerState(position = cameraPositionState.position.target),
                title = ""
            )
            for (polyLine in polyLines) {
                Polyline(
                    points = polyLine.toList(), color = Color.Red, width = 7f
                )
                Log.d("------------", "compose**line=$polyLine")
            }
        }
    }

    Button(
        onClick = {
            if (isTracking.value) {
                Log.d("---------", "stop...${isTracking.value}")
                text2 = "Start"
                coroutineScope.launch {
                    pauseTrackingService()
                }
            } else {
                Log.d("---------", "start...${isTracking.value}")
                coroutineScope.launch {
                    startOrResumeTrackingService()
                }
                text2 = "Stop"
            }
        }
    ) {
        Text(text = text2)
    }
}