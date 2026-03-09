package com.myprojects.modules.runningtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.myprojects.modules.runningtracker.Constants.TRACKING_STATE_PAUSED
import com.myprojects.modules.runningtracker.Constants.TRACKING_STATE_RUNNING
import com.myprojects.modules.runningtracker.Constants.TRACKING_STATE_STOPPED
import com.myprojects.modules.runningtracker.db.Run
import com.myprojects.modules.runningtracker.repository.TrackingRepository
import com.myprojects.modules.runningtracker.services.TrackingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.myprojects.modules.runningtracker.util.calculateDistance
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class TrackingViewmodel @Inject constructor(
    private val trackingRepository: TrackingRepository,
    private val trackingManager: TrackingManager
) : ViewModel() {
    private val _polyLinesFlow = MutableStateFlow<List<List<LatLng>>>(emptyList())
    val polyLinesFlow = _polyLinesFlow

    val trackingState: StateFlow<Int> = trackingManager.isTracking
    
    private val _runsFlow = MutableStateFlow<List<Run>>(emptyList())
    val runsFlow: StateFlow<List<Run>> = _runsFlow

    val currentLocation: StateFlow<LatLng?> = trackingManager.locationFlow.map { it?.latLng }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val currentBearing: StateFlow<Float> = trackingManager.locationFlow.map { it?.bearing ?: 0f }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    val timeInMillis: StateFlow<Long> = trackingManager.timeRunInMillis

    val distanceInMeters: StateFlow<Float> = polyLinesFlow.map { polylines ->
        calculateDistance(polylines)
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    val avgSpeedInKMH: StateFlow<Float> = combine(
        distanceInMeters,
        timeInMillis
    ) { distance, time ->
        if (time == 0L) {
            0f
        } else {
            (distance / 1000f) / (time / 1000f / 3600f) // distance in km / time in hours
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    var runStartedAt: Long = 0L
    private val _navigateToRunsScreen = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToRunsScreen: SharedFlow<Unit> = _navigateToRunsScreen

    private val _run = MutableStateFlow<Run?>(null)
    val run: StateFlow<Run?> = _run.asStateFlow()

    init {
        // Collect tracking state to handle stopping and saving
        viewModelScope.launch {
            trackingManager.isTracking.collect { state ->
                if (state == TRACKING_STATE_STOPPED) {
                    updateRun()
                }
            }
        }

        // Collect location updates to build polylines
        viewModelScope.launch {
            trackingManager.locationFlow.collect { locationData ->
                if (locationData != null && trackingState.value == TRACKING_STATE_RUNNING) {
                    val currentPolyLines = _polyLinesFlow.value.toMutableList()
                    if (currentPolyLines.isEmpty() || currentPolyLines.last().isEmpty()) {
                        currentPolyLines.add(mutableListOf(locationData.latLng))
                    } else {
                        val currentPolyLine = currentPolyLines.last().toMutableList()
                        currentPolyLine.add(locationData.latLng)
                        currentPolyLines[currentPolyLines.lastIndex] = currentPolyLine
                    }
                    _polyLinesFlow.value = currentPolyLines
                }
            }
        }

        // Collect runs directly from the repository
        viewModelScope.launch {
            trackingRepository.getAllRunsSortedByDate().collect { runs ->
                _runsFlow.value = runs
            }
        }
    }

    // This is now redundant but kept for interface compatibility if needed, 
    // though the logic is moved to init { }
    fun getLocationFlow() { }

    fun getRunById(id: Int) =
        trackingRepository.getRunById(id)
            .onEach {
                _run.value = it
                _polyLinesFlow.value = it.locationList
            }
            .launchIn(viewModelScope)

    fun resumeRun() {
        val currentPolyLines = _polyLinesFlow.value.toMutableList()
        currentPolyLines.add(mutableListOf())
        _polyLinesFlow.value = currentPolyLines
        trackingRepository.startLocationService()
    }

    fun pauseRun() {
        trackingRepository.pauseLocationService()
    }

    fun startRun() {
        runStartedAt = System.currentTimeMillis()
        _polyLinesFlow.value = emptyList()
        val currentPolyLines = _polyLinesFlow.value.toMutableList()
        currentPolyLines.add(mutableListOf())
        _polyLinesFlow.value = currentPolyLines
        trackingRepository.startLocationService()
    }

    fun updateRun() = viewModelScope.launch {
        trackingRepository.insertRun(createRun())
        trackingRepository.stopLocationService()
        _polyLinesFlow.value = emptyList()
        _navigateToRunsScreen.emit(Unit)
    }

    fun createRun(): Run = Run(
        startedAt = runStartedAt,
        durationInMillis = timeInMillis.value,
        img = null,
        distanceInMeters = distanceInMeters.value.toInt(),
        avgSpeedInKMH = avgSpeedInKMH.value,
        caloriesBurned = 0,
        locationList = _polyLinesFlow.value
    )

    fun deleteRun(run: Run) = viewModelScope.launch {
        trackingRepository.deleteRun(run)
    }
}