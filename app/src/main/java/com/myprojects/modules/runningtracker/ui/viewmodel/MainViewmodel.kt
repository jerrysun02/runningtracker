package com.myprojects.modules.runningtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
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
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class MainViewmodel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {
    private val _polyLinesFlow = MutableStateFlow<List<List<LatLng>>>(emptyList())
    val polyLinesFlow = _polyLinesFlow

    private val _trackingState = MutableStateFlow(0)
    val trackingState: StateFlow<Int> = _trackingState
    var state = 0

    private val _runsFlow = MutableStateFlow<List<Run>>(emptyList())
    val runsFlow: StateFlow<List<Run>> = _runsFlow

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _timeInMillis = MutableStateFlow(0L)
    val timeInMillis: StateFlow<Long> = _timeInMillis.asStateFlow()

    //val distanceInMeters: StateFlow<Float> = polyLinesFlow.map { polylines ->
    //   calculateDistance(polylines)
    //}//.asStateFlow()

    var start = ""
    var end = ""
    var timeStarted = 0L
    val formatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("yyyy-M-mm HH:mm:ss")

    init {
        // Initialize state based on TrackingService's current state
        viewModelScope.launch {
            TrackingService.isTracking.collect {
                Timber.d("isTracking: $it")
                _trackingState.value = it
                state = it
            }
        }
    }

    fun getLocationFlow() {
        viewModelScope.launch {
            TrackingService.locationFlow.collect {
                if (it != null) {
                    Timber.d("LocationFlow: $it")
                    if (state == 1) {
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

    fun getRunsFlow() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _runsFlow.value = mainRepository.getAllRunsSortedByDate()
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
                _trackingState.value = 2
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
        _trackingState.value = 0
        _timeInMillis.value = 0L
    }

    fun createRun(): Run = Run(
        start = start,
        end = end,
        img = null,
        timestamp = timeStarted,
        avgSpeedInKMH = 0f,
        distanceInMeters = 0,//distanceInMeters.value.toInt(),
        timeInMillis = _timeInMillis.value,
        caloriesBurned = 0,
        locationList = _polyLinesFlow.value
    )

    fun deleteRun(run: Run) = viewModelScope.launch {
        mainRepository.deleteRun(run)
    }
}