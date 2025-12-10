package com.myprojects.modules.runningtracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
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
import com.myprojects.modules.runningtracker.util.formatTime
import androidx.compose.material3.Card
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement

@Composable
fun MapComposable(navController: NavController, viewmodel: MainViewmodel) {
    val coroutineScope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState()
    val trackingState by viewmodel.trackingState.collectAsState()
    val polyLines by viewmodel.polyLinesFlow.collectAsStateWithLifecycle()
    val timeInMillis by viewmodel.timeInMillis.collectAsState()
    val currentLocation by viewmodel.currentLocation.collectAsState()
    val distanceInMeters by viewmodel.distanceInMeters.collectAsState()
    val avgSpeedInKMH by viewmodel.avgSpeedInKMH.collectAsState()
    val currentBearing by viewmodel.currentBearing.collectAsState()

    LaunchedEffect(Unit) {
        viewmodel.getLocationFlow()
        viewmodel.navigateToRunsScreen.collect { // Observe for navigation events
            navController.navigate(route = Routes.Runs.route) {
                popUpTo(Routes.Tracking.route) { inclusive = true }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { //viewmodel.updateRun()
        }
    }

    fun pauseTracking() {
        viewmodel.pauseRun()
    }

    fun resumeTracking() {
        viewmodel.resumeRun()
    }

    fun stopTracking() {
        viewmodel.updateRun() // This will now trigger the navigation via SharedFlow
        // navController.navigate(route = Routes.Runs.route) // Remove direct navigation
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            currentLocation?.let {
                Marker(state = MarkerState(position = it))
                LaunchedEffect(it) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.builder()
                                .target(it)
                                .zoom(17f)
                                .bearing(currentBearing) // Apply the bearing
                                .build()
                        )
                    )
                }
            }

            if (polyLines.isNotEmpty()) {
                for (polyLine in polyLines) {
                    Polyline(
                        points = polyLine.toList(), color = Color.Red, width = 12f
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(modifier = Modifier.padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Time: ${formatTime(timeInMillis)}")
                    Text("Distance: %.2f km".format(distanceInMeters / 1000f))
                    Text("Avg Speed: %.2f km/h".format(avgSpeedInKMH))
                }
            }
            Row(
                modifier = Modifier
                    .padding(
                        horizontal = 0.dp,
                        vertical = 0.dp
                    ), // Adjust padding as parent is now a Row
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = {
                        when (trackingState) {
                            1 -> pauseTracking() // Running, so pause
                            0 -> resumeTracking() // Paused, so resume
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (trackingState == 1) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                }
                FloatingActionButton(
                    onClick = { coroutineScope.launch { stopTracking() } },
                    modifier = Modifier.padding(start = 16.dp) // Add spacing between buttons
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop")
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewMapComposable() {
    // This preview won't work without a mock NavController and ViewModel
    // For demonstration purposes, you might want to create mock objects
    // to see the composable in isolation.
}