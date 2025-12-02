package com.myprojects.modules.runningtracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel

@Composable
fun RouteComposable(navController: NavController, viewmodel: MainViewmodel, id: Int) {
    val cameraPositionState = CameraPositionState()
    val polyLines by viewmodel.polyLinesFlow.collectAsStateWithLifecycle()
    val points = mutableListOf<LatLng>()

    LaunchedEffect(Unit) {
        viewmodel.getRoute(id)
    }

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
            val boundsBuilder = LatLngBounds.Builder()
            points.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            cameraPositionState.move(
                newLatLngBounds(bounds, 100)
            )
        }
    }
}