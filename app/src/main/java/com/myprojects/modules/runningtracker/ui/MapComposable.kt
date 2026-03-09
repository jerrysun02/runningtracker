package com.myprojects.modules.runningtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.myprojects.modules.runningtracker.ui.viewmodel.TrackingViewmodel
import com.myprojects.modules.runningtracker.util.formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun MapComposable(navController: NavController, viewmodel: TrackingViewmodel) {
    val coroutineScope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState()
    val trackingState by viewmodel.trackingState.collectAsStateWithLifecycle()
    val polyLines by viewmodel.polyLinesFlow.collectAsStateWithLifecycle()
    val timeInMillis by viewmodel.timeInMillis.collectAsStateWithLifecycle()
    val currentLocation by viewmodel.currentLocation.collectAsStateWithLifecycle()
    val distanceInMeters by viewmodel.distanceInMeters.collectAsStateWithLifecycle()
    val avgSpeedInKMH by viewmodel.avgSpeedInKMH.collectAsStateWithLifecycle()
    val currentBearing by viewmodel.currentBearing.collectAsStateWithLifecycle()

    var isAutoFollowEnabled by remember { mutableStateOf(true) }
    var shouldTakeSnapshot by remember { mutableStateOf(false) }

    LaunchedEffect(currentLocation, currentBearing, isAutoFollowEnabled) {
        if (isAutoFollowEnabled) {
            currentLocation?.let {
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder()
                            .target(it)
                            .zoom(17f)
                            .bearing(currentBearing)
                            .build()
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewmodel.navigateToRunsScreen.collect {
            navController.navigate(route = Routes.Runs.route) {
                popUpTo(Routes.Tracking.route) { inclusive = true }
            }
        }
    }

    fun stopTracking() {
        if (polyLines.isNotEmpty() && polyLines.any { it.isNotEmpty() }) {
            val boundsBuilder = LatLngBounds.builder()
            var hasPoints = false
            for (polyLine in polyLines) {
                for (point in polyLine) {
                    boundsBuilder.include(point)
                    hasPoints = true
                }
            }
            if (hasPoints) {
                coroutineScope.launch {
                    isAutoFollowEnabled = false
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100),
                        1000
                    )
                    delay(1200) // Wait for animation to finish
                    shouldTakeSnapshot = true
                }
            } else {
                viewmodel.updateRun()
            }
        } else {
            viewmodel.updateRun()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { isAutoFollowEnabled = false }
        ) {
            MapEffect(shouldTakeSnapshot) { map ->
                if (shouldTakeSnapshot) {
                    map.snapshot { bitmap ->
                        viewmodel.updateRun(bitmap)
                        shouldTakeSnapshot = false
                    }
                }
            }

            currentLocation?.let {
                Marker(state = MarkerState(position = it))
            }

            polyLines.forEach { polyLine ->
                Polyline(points = polyLine, color = Color.Red, width = 12f)
            }
        }

        SmallFloatingActionButton(
            onClick = { isAutoFollowEnabled = !isAutoFollowEnabled },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            containerColor = if (isAutoFollowEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isAutoFollowEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ) {
            Icon(
                imageVector = if (isAutoFollowEnabled) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                contentDescription = "Toggle Auto-Follow"
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem("Time", formatTime(timeInMillis))
                    StatItem("Distance", "%.2f km".format(distanceInMeters / 1000f))
                    StatItem("Avg Speed", "%.1f km/h".format(avgSpeedInKMH))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = {
                        if (trackingState == 1) {
                            viewmodel.pauseRun()
                        } else {
                            viewmodel.resumeRun()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (trackingState == 1) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(24.dp))

                FloatingActionButton(
                    onClick = { stopTracking() },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop, 
                        contentDescription = "Stop",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
