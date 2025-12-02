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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MainViewmodel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {
    private val _polyLinesFlow = MutableStateFlow<List<List<LatLng>>>(emptyList())
    val polyLinesFlow = _polyLinesFlow

    private val _trackingState = MutableStateFlow(1)
    val trackingState: StateFlow<Int> = _trackingState
    var state = 1

    private val _runsFlow = MutableStateFlow<List<Run>>(emptyList())
    val runsFlow: StateFlow<List<Run>> = _runsFlow

    var start = ""
    var end = ""
    var timeStarted = 0L
    val formatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("yyyy-M-mm HH:mm:ss")

    fun getLocationFlow() {
        viewModelScope.launch {
            TrackingService.locationFlow.collect {
                if (it != null) {
                    if (state == 1)
                        _polyLinesFlow.value = _polyLinesFlow.value.toMutableList().apply {
                            val currentPolyLine = this[this.lastIndex].toMutableList()
                            currentPolyLine.add(it)
                            this[this.lastIndex] = currentPolyLine
                        }.toList()
                }
            }
        }
        viewModelScope.launch {
            TrackingService.isTracking.collect {
                _trackingState.tryEmit(it)
                state = it
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
                run.onEach {
                    it.locationList.onEach { it1 ->
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
        _polyLinesFlow.value = _polyLinesFlow.value.toMutableList().apply {
            this.add(mutableListOf())
        }
        mainRepository.startLocationService()
    }

    fun pauseRun() {
        mainRepository.pauseLocationService()
    }

    suspend fun startRun() {
        start = LocalDateTime.now().format(formatter)
        timeStarted = System.currentTimeMillis()
        mainRepository.startLocationService()
        _polyLinesFlow.value = emptyList()
        _polyLinesFlow.value = _polyLinesFlow.value.toMutableList().apply {
            this.add(mutableListOf())
        }
    }

    fun updateRun() = viewModelScope.launch {
        end = LocalDateTime.now().format(formatter)
        mainRepository.insertRun(createRun())
        mainRepository.stopLocationService()
        _polyLinesFlow.value = emptyList()
        _trackingState.value = 0
    }

    fun createRun(): Run = Run(
        start = start,
        end = end,
        img = null,
        timestamp = timeStarted,
        avgSpeedInKMH = 0f,
        distanceInMeters = 0,
        timeInMillis = 0,
        caloriesBurned = 0,
        locationList = _polyLinesFlow.value
    )

    fun deleteRun(run: Run) = viewModelScope.launch {
        mainRepository.deleteRun(run)
    }
}