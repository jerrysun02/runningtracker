package com.myprojects.modules.runningtracker.ui.viewmodel

import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.myprojects.modules.runningtracker.db.Run
import com.myprojects.modules.runningtracker.repository.MainRepository
import com.myprojects.modules.runningtracker.services.TrackingService
import com.myprojects.modules.runningtracker.services.TrackingService.Companion.end
import com.myprojects.modules.runningtracker.services.TrackingService.Companion.start
import com.myprojects.modules.runningtracker.services.TrackingService.Companion.timeStarted
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

    var runId = -1L
    val context = LocalContext

    fun getLocationFlow() {
        viewModelScope.launch {
            TrackingService.locationFlow.collect {
                if (it != null) {
                    Log.d("-----------", "vm pos=$it size=${_polyLinesFlow.value.size}")
                    Log.d("-----------", "vm isTracking=$state")
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
                        Log.d("-----------", "vm it1.size=${it1.size}")
                        if (it1.isNotEmpty()) {
                            _polyLinesFlow.value = _polyLinesFlow.value.toMutableList().apply {
                                this.add(it1.toMutableList())
                            }
                        }
                    }
                }
                _trackingState.value = 2
                Log.d("-----------", "vm getRoute polyLines.size=${_polyLinesFlow.value.size}")
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
        _polyLinesFlow.value = emptyList()
        _polyLinesFlow.value = _polyLinesFlow.value.toMutableList().apply {
            this.add(mutableListOf())
        }
        mainRepository.startLocationService()
        val formatter = DateTimeFormatter.ofPattern("dd/M/yyyy HH:mm:ss")
        val start = LocalDateTime.now().format(formatter)
        val run = Run(
            start = start,
            end = "",
            img = null,
            timestamp = timeStarted,
            avgSpeedInKMH = 0f,
            distanceInMeters = 0,
            timeInMillis = 0,
            caloriesBurned = 0,
            locationList = emptyList()
        )
        runId = mainRepository.addNewRun(run)
    }

    fun updateRun() = viewModelScope.launch {
        val formatter = DateTimeFormatter.ofPattern("dd/M/yyyy HH:mm:ss")
        end = LocalDateTime.now().format(formatter)
    /*    val run = Run(
            runId,
            start,
            end,
            null,
            timeStarted,
            0f,
            0,
            0,
            0,
            _polyLinesFlow.value
        )*/
        mainRepository.updateRun(createRun())
        mainRepository.stopLocationService()
        _polyLinesFlow.value = emptyList()
        _trackingState.value = 0
        Log.d("-----------", "updateRun -- stopped.......")
    }

    fun createRun() : Run {
        val formatter = DateTimeFormatter.ofPattern("dd/M/yyyy HH:mm:ss")
        end = LocalDateTime.now().format(formatter)
        val run = Run(
            runId,
            start,
            end,
            null,
            timeStarted,
            0f,
            0,
            0,
            0,
            _polyLinesFlow.value
        )
        return run
    }

    fun deleteRun(run: Run) = viewModelScope.launch {
        mainRepository.deleteRun(run)
    }
}