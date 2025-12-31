package com.myprojects.modules.runningtracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.myprojects.modules.runningtracker.ui.viewmodel.TrackingViewmodel
import com.myprojects.modules.runningtracker.util.formatTime
import com.myprojects.modules.runningtracker.util.toKilometers
import com.myprojects.modules.runningtracker.util.toFormattedAvgSpeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteComposable(navController: NavController, viewmodel: TrackingViewmodel, id: Int) {
    val cameraPositionState = CameraPositionState()
    val polyLines by viewmodel.polyLinesFlow.collectAsStateWithLifecycle()
    val points = mutableListOf<LatLng>()

    val run by viewmodel.run.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewmodel.getRunById(id)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            if (polyLines.isNotEmpty()) {
                for (polyLine in polyLines) {
                    if (polyLine.isNotEmpty()) {
                        Polyline(
                            points = polyLine.toList(), color = Color.Red, width = 12f
                        )
                        points += polyLine.toList()
                    }
                }
                if (points.isNotEmpty()) { // Ensure points list is not empty before building bounds
                    val boundsBuilder = LatLngBounds.Builder()
                    points.forEach { boundsBuilder.include(it) }
                    val bounds = boundsBuilder.build()
                    cameraPositionState.move(
                        newLatLngBounds(bounds, 100)
                    )
                }
            }
        }
        run?.let {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Time: ${formatTime(it.durationInMillis)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Distance: ${it.distanceInMeters.toKilometers()} km",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Avg Speed: ${it.avgSpeedInKMH.toFormattedAvgSpeed()} km/h",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
    }
}