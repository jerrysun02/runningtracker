package com.myprojects.modules.runningtracker.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.model.LatLng
import com.myprojects.modules.runningtracker.Constants.ACTION_PAUSE_SERVICE
import com.myprojects.modules.runningtracker.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.myprojects.modules.runningtracker.Constants.ACTION_START_OR_RESUME_SERVICE
import com.myprojects.modules.runningtracker.Constants.ACTION_STOP_SERVICE
import com.myprojects.modules.runningtracker.Constants.FASTEST_LOCATION_INTERNAL
import com.myprojects.modules.runningtracker.Constants.LOCATION_UPDATE_INTERNAL
import com.myprojects.modules.runningtracker.Constants.NOTIFICATION_CHANNEL_ID
import com.myprojects.modules.runningtracker.Constants.NOTIFICATION_CHANNEL_NAME
import com.myprojects.modules.runningtracker.R
import com.myprojects.modules.runningtracker.TrackingUtility
import com.myprojects.modules.runningtracker.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

class TrackingService : LifecycleService() {
    private var isFirstRun = true

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object {
        val isTracking = MutableStateFlow(false)

        //val pathPoints = MutableLiveData<Polylines>()
        //val pathPointsFlow = MutableStateFlow<LatLng?>(null)
        //val polyLinesFlow = MutableStateFlow<Polylines?>(null)
        val atasehir = LatLng(0.0, 0.0)
       // val polylineFlow = MutableStateFlow(listOf(listOf(atasehir)))
       // val lines = mutableListOf(mutableListOf(atasehir))
        val locationFlow = MutableStateFlow(atasehir)
    }

    private fun postInitialValues() {
        //isTracking.postValue(false)
        //pathPoints.postValue(mutableListOf())
        //polylineFlow.tryEmit(mutableListOf())
    }

    override fun onCreate() {
        super.onCreate()
        postInitialValues()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        //    isTracking.observe(this, Observer {
        updateLocationTracking(true)
        //    })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        isTracking.value = true
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

    private fun pauseService() {
        //    isTimerEnabled = false
        isTracking.value = false
        addEmptyPolyline()
    }

    private fun killService() {
        //    serviceKilled = true
        isFirstRun = true
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            if (TrackingUtility.hasLocationPermissions(this) || true) {
                val request = LocationRequest().apply {
                    interval = LOCATION_UPDATE_INTERNAL
                    fastestInterval = FASTEST_LOCATION_INTERNAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                Log.d("--------------", "update location")
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
            //    Log.d("----------", "callback")
            if (isTracking.value) {
                p0.locations.let { locations ->
                    for (location in locations) {
                        Log.d("----------", "callback------")
                        addPathPoint(location)
                    }
                }
            }
        }
    }

    private fun addEmptyPolyline() {
        //lines.add(mutableListOf())
        //Log.d("-----------", "add empty =$lines")
        //polylineFlow.tryEmit(lines.toList())
    }

    private fun addPathPoint(location: Location?) {
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            // pathPoints.value?.apply {
            //     last().add(pos)
            //     pathPoints.postValue(this)
            //     pathPointsFlow.tryEmit(pos)

            //     polyLinesFlow.tryEmit(this)
            //lines.last().add(pos)
            //val l = lines.toList().toList()
            //Log.d("-------------", "addPath....size=${lines.size}")
            //polylineFlow.tryEmit(lines)
            locationFlow.tryEmit(pos)


            Log.d("----------", "emit pos=${pos}")

            //}
        }
    }

    //private fun addEmptyPolyline() = pathPoints.value?.apply {
    //    add(mutableListOf())
    //    pathPoints.postValue(this)
    //} ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    @SuppressLint("InlinedApi")
    private fun startForegroundService() {
        //    addEmptyPolyline()
        isTracking.value = true

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

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).also {
            it.action = ACTION_SHOW_TRACKING_FRAGMENT
        },
        FLAG_UPDATE_CURRENT
    )

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}