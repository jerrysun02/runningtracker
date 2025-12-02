package com.myprojects.modules.runningtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel
import kotlinx.coroutines.launch

@Composable
fun MapComposable(navController: NavController, viewmodel: MainViewmodel) {
    val coroutineScope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState()
    val trackingState by viewmodel.trackingState.collectAsState()
    var textLeft by remember { mutableStateOf("Pause") }
    val textRight by remember { mutableStateOf("Stop") }
    val button1Enabled by remember { mutableStateOf(true) }
    var button2Enabled by remember { mutableStateOf(true) }
    val polyLines by viewmodel.polyLinesFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewmodel.getLocationFlow()
    }

    fun pauseTracking() {
        viewmodel.pauseRun()
    }

    fun resumeTracking() {
        viewmodel.resumeRun()
    }

    fun stopTracking() {
        viewmodel.updateRun()
        navController.navigate(route = Routes.Runs.route)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = cameraPositionState.position.target)
        )

        if (polyLines.isNotEmpty()) {
            if (polyLines.last().isNotEmpty()) {
                cameraPositionState.position =
                    CameraPosition.fromLatLngZoom(polyLines.last().last(), 17f)
            }
            Marker(
                state = MarkerState(position = cameraPositionState.position.target),
                title = ""
            )
            for (polyLine in polyLines) {
                Polyline(
                    points = polyLine.toList(), color = Color.Red, width = 12f
                )
            }
            if (System.currentTimeMillis() - viewmodel.timeStarted > 1000 * 60 * 60 * 8) {
                stopTracking()
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Button(
            onClick = {
                if (trackingState == 1) {
                    textLeft = "Resume"
                    pauseTracking()
                } else {
                    textLeft = "Pause"
                    resumeTracking()
                }
                button2Enabled = true
            },
            enabled = button1Enabled
        ) {
            Text(text = textLeft)
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    stopTracking()
                }
                textLeft = "Start"
                button2Enabled = false
            },
            enabled = button2Enabled
        ) {
            Text(text = textRight)
        }
    }
}