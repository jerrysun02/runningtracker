package com.myprojects.modules.runningtracker.services

import com.google.android.gms.maps.model.LatLng
import com.myprojects.modules.runningtracker.Constants.TRACKING_STATE_PAUSED
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingManager @Inject constructor() {
    private val _isTracking = MutableStateFlow(TRACKING_STATE_PAUSED)
    val isTracking = _isTracking.asStateFlow()

    private val _locationFlow = MutableStateFlow<LocationData?>(null)
    val locationFlow = _locationFlow.asStateFlow()

    private val _timeRunInMillis = MutableStateFlow(0L)
    val timeRunInMillis = _timeRunInMillis.asStateFlow()

    fun updateTrackingState(state: Int) {
        _isTracking.value = state
    }

    fun updateLocation(locationData: LocationData?) {
        _locationFlow.value = locationData
    }

    fun updateTime(time: Long) {
        _timeRunInMillis.value = time
    }
}