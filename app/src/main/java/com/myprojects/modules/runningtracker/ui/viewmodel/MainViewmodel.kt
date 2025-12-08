package com.myprojects.modules.runningtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.myprojects.modules.runningtracker.Constants.TRACKING_STATE_PAUSED
import com.myprojects.modules.runningtracker.Constants.TRACKING_STATE_RUNNING
import com.myprojects.modules.runningtracker.Constants.TRACKING_STATE_STOPPED
import com.myprojects.modules.runningtracker.db.Run
import com.myprojects.modules.runningtracker.repository.MainRepository
import com.myprojects.modules.runningtracker.services.TrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import com.myprojects.modules.runningtracker.util.calculateDistance
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@HiltViewModel
class MainViewmodel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {
    private val _polyLinesFlow = MutableStateFlow<List<List<LatLng>>>(emptyList())
    val polyLinesFlow = _polyLinesFlow

    private val _trackingState = MutableStateFlow(TRACKING_STATE_PAUSED)
    val trackingState: StateFlow<Int> = _trackingState
    var state = TRACKING_STATE_PAUSED

    private val _runsFlow = MutableStateFlow<List<Run>>(emptyList())
    val runsFlow: StateFlow<List<Run>> = _runsFlow

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _timeInMillis = MutableStateFlow(0L)
    val timeInMillis: StateFlow<Long> = _timeInMillis.asStateFlow()

    val distanceInMeters: StateFlow<Float> = polyLinesFlow.map { polylines ->
        calculateDistance(polylines)
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )
    var start = ""
    var end = ""
    var timeStarted = 0L
    val formatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("yyyy-M-mm HH:mm:ss")

    private val _navigateToRunsScreen = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToRunsScreen: SharedFlow<Unit> = _navigateToRunsScreen

    init {
        // Initialize state based on TrackingService's current state
        viewModelScope.launch {
            TrackingService.isTracking.collect {
                Timber.d("isTracking: $it")
                _trackingState.value = it
                state = it
                if (it == TRACKING_STATE_STOPPED) { // State 2 means run stopped due to duration or explicit stop
                    updateRun() // Save the run data
                }
            }
        }
        // Collect runs directly from the repository as a Flow
        viewModelScope.launch {
            mainRepository.getAllRunsSortedByDate().collect { runs ->
                _runsFlow.value = runs
            }
        }
    }

    fun getLocationFlow() {
        viewModelScope.launch {
            TrackingService.locationFlow.collect {
                if (it != null) {
                    Timber.d("LocationFlow: $it")
                    if (state == TRACKING_STATE_RUNNING) {
                        val currentPolyLines = _polyLinesFlow.value.toMutableList()
                        if (currentPolyLines.isEmpty() || currentPolyLines.last().isEmpty()) {
                            currentPolyLines.add(mutableListOf(it))
                        } else {
                            val currentPolyLine = currentPolyLines.last().toMutableList()
                            currentPolyLine.add(it)
                            currentPolyLines[currentPolyLines.lastIndex] = currentPolyLine
                        }
                        _polyLinesFlow.value = currentPolyLines
                    }
                    _currentLocation.value = it
                }
            }
        }
        viewModelScope.launch {
            TrackingService.timeRunInMillis.collect {
                Timber.d("TimeRunInMillis: $it")
                _timeInMillis.value = it
            }
        }
    }

    fun getRoute(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _polyLinesFlow.value = emptyList()
                val run = mainRepository.getRoute(id)
                run.onEach { runValue ->
                    runValue.locationList.onEach { it1 ->
                        if (it1.isNotEmpty()) {
                            _polyLinesFlow.value = _polyLinesFlow.value.toMutableList().apply {
                                this.add(it1.toMutableList())
                            }
                        }
                    }
                }
                _trackingState.value = TRACKING_STATE_STOPPED
            }
        }
    }

    fun resumeRun() {
        val currentPolyLines = _polyLinesFlow.value.toMutableList()
        currentPolyLines.add(mutableListOf())
        _polyLinesFlow.value = currentPolyLines
        mainRepository.startLocationService()
    }

    fun pauseRun() {
        mainRepository.pauseLocationService()
    }

    fun startRun() {
        start = LocalDateTime.now().format(formatter)
        timeStarted = System.currentTimeMillis()
        _timeInMillis.value = 0L
        _polyLinesFlow.value = emptyList()
        val currentPolyLines = _polyLinesFlow.value.toMutableList()
        currentPolyLines.add(mutableListOf())
        _polyLinesFlow.value = currentPolyLines
        mainRepository.startLocationService()
    }

    fun updateRun() = viewModelScope.launch {
        end = (System.currentTimeMillis() - timeStarted).milliseconds.inWholeSeconds.toString()
        mainRepository.insertRun(createRun())
        mainRepository.stopLocationService()
        _polyLinesFlow.value = emptyList()
        _trackingState.value = TRACKING_STATE_PAUSED
        _timeInMillis.value = 0L
        _navigateToRunsScreen.emit(Unit) // Emit event to navigate
    }

    fun createRun(): Run = Run(
        start = start,
        end = end,
        img = null,
        timestamp = timeStarted,
        avgSpeedInKMH = 0f,
        distanceInMeters = distanceInMeters.value.toInt(),
        timeInMillis = _timeInMillis.value,
        caloriesBurned = 0,
        locationList = _polyLinesFlow.value
    )

    fun deleteRun(run: Run) = viewModelScope.launch {
        Timber.d("deleteRun: $run")
        mainRepository.deleteRun(run)
    }
}