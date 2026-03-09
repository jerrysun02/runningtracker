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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.location.Location
import java.util.Locale
import com.myprojects.modules.runningtracker.Constants.MIN_ACCURACY_THRESHOLD
import com.myprojects.modules.runningtracker.Constants.MIN_DISTANCE_CHANGE_THRESHOLD
import com.myprojects.modules.runningtracker.Constants.MIN_TIME_BETWEEN_UPDATES_THRESHOLD
import com.myprojects.modules.runningtracker.Constants.MAX_RUN_DURATION_MILLIS
import com.myprojects.modules.runningtracker.Constants.TRACKING_STATE_PAUSED
import com.myprojects.modules.runningtracker.Constants.TRACKING_STATE_RUNNING
import com.myprojects.modules.runningtracker.Constants.TRACKING_STATE_STOPPED

data class LocationData(val latLng: LatLng, val bearing: Float)

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var powerManager: PowerManager

    @Inject
    lateinit var trackingManager: TrackingManager

    private lateinit var wakeLock: PowerManager.WakeLock

    lateinit var curNotificationBuilder: NotificationCompat.Builder

    private var isFirstRun = true
    private var serviceRunningTime = 0L
    private var lastSecondTimestamp = 0L
    private var currentRunStartTime = 0L

    private var previousLocation: Location? = null
    private var previousUpdateTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder
        wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RunningTracker::WakeLock")
        postInitialValues()
        lifecycleScope.launch {
            trackingManager.isTracking.collect { state ->
                updateLocationTracking(state == TRACKING_STATE_RUNNING)
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
        trackingManager.updateTrackingState(TRACKING_STATE_PAUSED)
        trackingManager.updateLocation(null)
        trackingManager.updateTime(0L)
        serviceRunningTime = 0L
        lastSecondTimestamp = 0L
        currentRunStartTime = 0L
        previousLocation = null
        previousUpdateTime = 0L
    }

    private fun startForegroundService() {
        startTracking()
        currentRunStartTime = System.currentTimeMillis() - serviceRunningTime
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())
        startTimer()
    }

    private fun startTimer() {
        lifecycleScope.launch {
            while (trackingManager.isTracking.value == TRACKING_STATE_RUNNING) {
                serviceRunningTime = System.currentTimeMillis() - currentRunStartTime
                trackingManager.updateTime(serviceRunningTime)

                if (serviceRunningTime >= MAX_RUN_DURATION_MILLIS) {
                    Timber.d("Run duration exceeded 8 hours. Signaling stop.")
                    trackingManager.updateTrackingState(TRACKING_STATE_STOPPED)
                    break
                }

                if (serviceRunningTime >= lastSecondTimestamp + 1000L) {
                    val notificationManager =
                        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    val formattedTime = String.format(
                        Locale.getDefault(),
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
            if (trackingManager.isTracking.value == TRACKING_STATE_STOPPED) {
                killService()
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
            if (trackingManager.isTracking.value == TRACKING_STATE_RUNNING) {
                result.locations.let { locations ->
                    for (location in locations) {
                        if (location.accuracy > MIN_ACCURACY_THRESHOLD) {
                            continue
                        }

                        val currentTime = System.currentTimeMillis()
                        if (previousLocation == null ||
                            currentTime - previousUpdateTime >= MIN_TIME_BETWEEN_UPDATES_THRESHOLD ||
                            previousLocation!!.distanceTo(location) >= MIN_DISTANCE_CHANGE_THRESHOLD
                        ) {
                            trackingManager.updateLocation(LocationData(LatLng(location.latitude, location.longitude), location.bearing))
                            previousLocation = location
                            previousUpdateTime = currentTime
                        }
                    }
                }
            }
        }
    }

    private fun pauseService() {
        trackingManager.updateTrackingState(TRACKING_STATE_PAUSED)
        if (wakeLock.isHeld) wakeLock.release()
    }

    private fun startTracking() {
        trackingManager.updateTrackingState(TRACKING_STATE_RUNNING)
        currentRunStartTime = System.currentTimeMillis() - serviceRunningTime
        startTimer()
        if (!wakeLock.isHeld) wakeLock.acquire(1000 * 60 * 60 * 8L)
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
        trackingManager.updateTrackingState(TRACKING_STATE_PAUSED)
        postInitialValues()
        stopSelf()
        if (wakeLock.isHeld) wakeLock.release()
    }
}