package com.myprojects.modules.runningtracker.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.myprojects.modules.runningtracker.Constants.ACTION_PAUSE_SERVICE
import com.myprojects.modules.runningtracker.Constants.ACTION_START_OR_RESUME_SERVICE
import com.myprojects.modules.runningtracker.Constants.ACTION_STOP_SERVICE
import com.myprojects.modules.runningtracker.Constants.FASTEST_LOCATION_INTERVAL
import com.myprojects.modules.runningtracker.Constants.LOCATION_UPDATE_INTERVAL
import com.myprojects.modules.runningtracker.Constants.NOTIFICATION_CHANNEL_ID
import com.myprojects.modules.runningtracker.Constants.NOTIFICATION_CHANNEL_NAME
import com.myprojects.modules.runningtracker.Constants.NOTIFICATION_ID
import com.myprojects.modules.runningtracker.Constants.TIMER_UPDATE_INTERVAL
import com.myprojects.modules.runningtracker.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var powerManager: PowerManager

    private lateinit var wakeLock: PowerManager.WakeLock

    lateinit var curNotificationBuilder: NotificationCompat.Builder

    private var isFirstRun = true
    private var serviceRunningTime = 0L
    private var lastSecondTimestamp = 0L
    private var currentRunStartTime = 0L

    companion object {
        val isTracking = MutableStateFlow(0) // 0: Paused, 1: Running, 2: Stopped/Initial
        val locationFlow = MutableStateFlow<LatLng?>(null)
        val timeRunInMillis = MutableStateFlow(0L)
    }

    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder
        wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RunningTracker::WakeLock")
        postInitialValues()
        lifecycleScope.launch {
            isTracking.collect { isTracking ->
                updateLocationTracking(isTracking == 1)
                updateNotificationTrackingState(isTracking == 1)
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
                    } else {
                        Timber.d("Resuming service...")
                        startTracking()
                    }
                }

                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused service")
                    pauseService()
                }

                ACTION_STOP_SERVICE -> {
                    Timber.d("Stopped service")
                    killService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun postInitialValues() {
        isTracking.value = 0
        locationFlow.value = null
        timeRunInMillis.value = 0L
        serviceRunningTime = 0L
        lastSecondTimestamp = 0L
        currentRunStartTime = 0L
    }

    private fun startForegroundService() {
        startTracking()
        currentRunStartTime = System.currentTimeMillis() - serviceRunningTime
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())
        startTimer()
    }

    private fun startTimer() {
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value == 1) {
                serviceRunningTime = System.currentTimeMillis() - currentRunStartTime
                timeRunInMillis.value = serviceRunningTime
                if (serviceRunningTime >= lastSecondTimestamp + 1000L) {
                    val notificationManager =
                        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    val formattedTime = String.format(
                        "%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(serviceRunningTime),
                        TimeUnit.MILLISECONDS.toMinutes(serviceRunningTime) % 60,
                        TimeUnit.MILLISECONDS.toSeconds(serviceRunningTime) % 60
                    )
                    curNotificationBuilder.setContentText(formattedTime)
                    notificationManager.notify(NOTIFICATION_ID, curNotificationBuilder.build())
                    lastSecondTimestamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_UPDATE_INTERVAL
            )
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL)
                .setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL)
                .build()

            fusedLocationProviderClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (isTracking.value == 1) {
                result.locations.let { locations ->
                    for (location in locations) {
                        locationFlow.value = LatLng(location.latitude, location.longitude)
                        Timber.d("NEW LOCATION: ${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }
    }

    private fun pauseService() {
        isTracking.value = 0
        if (wakeLock.isHeld) wakeLock.release()
    }

    private fun startTracking() {
        isTracking.value = 1
        currentRunStartTime = System.currentTimeMillis() - serviceRunningTime
        startTimer()
        if (!wakeLock.isHeld) wakeLock.acquire(1000 * 60 * 60 * 8L) // Acquire for 8 hours
    }

    private fun updateNotificationTrackingState(isTracking: Boolean) {
        val notificationActionText = if (isTracking) "Pause" else "Resume"
        val pendingIntent = if (isTracking) {
            val pauseIntent =
                Intent(this, TrackingService::class.java).apply { action = ACTION_PAUSE_SERVICE }
            PendingIntent.getService(this, 1, pauseIntent, FLAG_IMMUTABLE)
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, FLAG_IMMUTABLE)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply { isAccessible = true }
            .set(curNotificationBuilder, ArrayList<NotificationCompat.Action>())

        curNotificationBuilder = baseNotificationBuilder
            .addAction(R.drawable.ic_pause_black_24dp, notificationActionText, pendingIntent)
        notificationManager.notify(NOTIFICATION_ID, curNotificationBuilder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun killService() {
        isTracking.value = 2
        postInitialValues()
        stopForeground(true)
        stopSelf()
        if (wakeLock.isHeld) wakeLock.release()
    }
}