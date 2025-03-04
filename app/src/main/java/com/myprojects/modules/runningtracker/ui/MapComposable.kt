package com.myprojects.modules.runningtracker.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.myprojects.modules.runningtracker.db.Run
import com.myprojects.modules.runningtracker.services.TrackingService
import com.myprojects.modules.runningtracker.services.TrackingService.Companion.end
import com.myprojects.modules.runningtracker.services.TrackingService.Companion.start
import com.myprojects.modules.runningtracker.services.TrackingService.Companion.timeStarted
import com.myprojects.modules.runningtracker.ui.viewmodel.MainViewmodel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date

@Composable
fun MapComposable(navController: NavController, viewmodel: MainViewmodel) {
    val coroutineScope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState()
    val location by viewmodel.locationFlow.collectAsState()
    val trackingState by viewmodel.trackingState.collectAsState()
    var textLeft by remember { mutableStateOf("Pause") }
    val textRight by remember { mutableStateOf("Stop") }
    val button1Enabled by remember { mutableStateOf(true) }
    var button2Enabled by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        //viewmodel.startRun()
    }

    fun pauseTracking() {
        Log.d("------------", "compose pause")
        Intent(context, TrackingService::class.java).also {
            it.action = ACTION_PAUSE_SERVICE
            context.startService(it)
        }
    }

    fun resumeTracking() {
        Log.d("------------", "compose resume")
        viewmodel.resumeRun()
        Intent(context, TrackingService::class.java).also {
            it.action = ACTION_START_OR_RESUME_SERVICE
            context.startService(it)
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun stopTrackingService() {
        var routines = viewmodel.polyLines

        val loc = mutableListOf(mutableListOf<LatLng>())

        viewmodel.polyLines.forEach {
            val locations = Collections.unmodifiableList(it)
            loc.add(locations)
        }

        Log.d("------------", "compose stop...$loc")
        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        end = sdf.format(Date())
        val run = Run(
            start,
            end,
            null,
            timeStarted,
            0f,
            0,
            0,
            0,
            loc
        )
        viewmodel.insertRun(run)

        navController.navigate(route = Routes.Login.route)

        Intent(context, TrackingService::class.java).also {
            it.action = ACTION_STOP_SERVICE
            context.startService(it)
        }
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
                CameraPosition.fromLatLngZoom(location!!, 15f)
        }

        if (viewmodel.polyLines.isNotEmpty()) {
            //Log.d("------------", "compose lines=$(viewmodel.polyLines)")
            Marker(
                state = MarkerState(position = cameraPositionState.position.target),
                title = ""
            )
            for (polyLine in viewmodel.polyLines) {
                Polyline(
                    points = polyLine.toList(), color = Color.Red, width = 12f
                )
                //Log.d("------------", "compose**line=$polyLine")
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
                    stopTrackingService()
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