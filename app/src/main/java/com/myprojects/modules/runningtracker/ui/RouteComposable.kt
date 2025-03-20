package com.myprojects.modules.runningtracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel

@Composable
fun RouteComposable(navController: NavController, viewmodel: MainViewmodel, id: Int) {
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(Unit) {
        viewmodel.getRoute(id)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        if (viewmodel.polyLines.isNotEmpty()) {
            Marker(
                state = MarkerState(position = cameraPositionState.position.target),
                title = ""
            )
            for (polyLine in viewmodel.polyLines) {
                if (polyLine.size > 0) {
                    Polyline(
                        points = polyLine.toList(), color = Color.Red, width = 12f
                    )
                    val location = polyLine[0]
                    location.let {
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(location, 11f)
                    }
                }
            }
        }
    }
}