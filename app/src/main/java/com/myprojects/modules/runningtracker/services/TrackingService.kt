package com.myprojects.modules.runningtracker.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.myprojects.modules.runningtracker.Constants.ACTION_PAUSE_SERVICE
import com.myprojects.modules.runningtracker.Constants.ACTION_START_OR_RESUME_SERVICE
import com.myprojects.modules.runningtracker.Constants.ACTION_STOP_SERVICE
import com.myprojects.modules.runningtracker.Constants.FASTEST_LOCATION_INTERNAL
import com.myprojects.modules.runningtracker.Constants.LOCATION_UPDATE_INTERNAL
import com.myprojects.modules.runningtracker.Constants.NOTIFICATION_CHANNEL_ID
import com.myprojects.modules.runningtracker.Constants.NOTIFICATION_CHANNEL_NAME
import com.myprojects.modules.runningtracker.Constants.TAG
import com.myprojects.modules.runningtracker.R
import com.myprojects.modules.runningtracker.TrackingUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TrackingService : Service() {
    private var isFirstRun = true
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val formatter = DateTimeFormatter.ofPattern("dd/M/yyyy HH:mm:ss")
    var preLocation = LatLng(0.0, 0.0)
    var preTime = 0L

    companion object {
        val isTracking = MutableStateFlow(1)
        val locationFlow = MutableStateFlow<LatLng?>(null)
        var timeStarted = 0L
        var start = ""
        var end = ""
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        CoroutineScope(Dispatchers.Main).launch {
            isTracking.collect {
                updateLocationTracking(it)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                        timeStarted = System.currentTimeMillis()
                        start = LocalDateTime.now().format(formatter)
                    } else {
                        isTracking.value = 1
                    }
                }

                ACTION_PAUSE_SERVICE -> {
                    pauseService()
                }

                ACTION_STOP_SERVICE -> {
                    end = LocalDateTime.now().format(formatter)
                    killService()
                }

                else -> {
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun pauseService() {
        isTracking.tryEmit(2)
    }

    private fun killService() {
        isFirstRun = true
        isTracking.tryEmit(0)
        stopSelf()
    }

    private fun updateLocationTracking(isTracking: Int) {
        if (isTracking != 0) {
            if (TrackingUtility.hasLocationPermissions(this)) {
                val request = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    LOCATION_UPDATE_INTERNAL
                )
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERNAL)
                    .setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERNAL)
                    .build()
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            if (p0 == null) {
                return
            }
            super.onLocationResult(p0)
            p0.locations.let { locations ->
                for (location in locations) {
                    Log.d(TAG, "callback------")
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        preLocation.latitude, preLocation.longitude,
                        location.latitude, location.longitude,
                        results
                    )
                    if (results[0] > 3 && System.currentTimeMillis() - preTime > 1000 * 3) {
                        preLocation = LatLng(location.latitude, location.longitude)
                        preTime = System.currentTimeMillis()
                        locationFlow.tryEmit(LatLng(location.latitude, location.longitude))
                    }
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun startForegroundService() {
        isTracking.value = 1

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(false)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Running App")
            .setContentText("00:00:00")

        ServiceCompat.startForeground(
            this,
            1,
            notificationBuilder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}