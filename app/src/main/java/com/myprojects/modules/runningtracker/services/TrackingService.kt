package com.myprojects.modules.runningtracker.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
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
import java.text.SimpleDateFormat
import java.util.Date

class TrackingService : Service() {
    private var isFirstRun = true
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

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

    @SuppressLint("SimpleDateFormat")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                        Log.d(TAG, "service: start isTracking = 1 first")
                    } else {
                        isTracking.value = 1
                        timeStarted = System.currentTimeMillis()

                        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                        start = sdf.format(Date())
                        Log.d(TAG, "service: start isTracking = 1")
                    }
                }

                ACTION_PAUSE_SERVICE -> {
                    pauseService()
                }

                ACTION_STOP_SERVICE -> {
                    killService()
                }

                else -> {
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
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
        Log.d(TAG, "service updateLocTracking isTracking=$isTracking")
    //    Log.d(TAG, "permission = " + TrackingUtility.hasLocationPermissions(this))
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
                Log.d(TAG, "update location")
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
            super.onLocationResult(p0)
            p0.locations.let { locations ->
                for (location in locations) {
                    Log.d(TAG, "callback------")
                    locationFlow.tryEmit(LatLng(location.latitude, location.longitude))
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun startForegroundService() {
        isTracking.value = 1

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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