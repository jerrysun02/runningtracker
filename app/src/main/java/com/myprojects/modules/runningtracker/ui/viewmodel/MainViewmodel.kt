package com.myprojects.modules.runningtracker.ui.viewmodel

import android.util.Log
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
import javax.inject.Inject

@HiltViewModel
class MainViewmodel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {
    private val _locationFlow = MutableStateFlow<LatLng?>(null)
    val locationFlow: StateFlow<LatLng?> = _locationFlow
    val polyLines = mutableListOf(mutableListOf<LatLng>())

    private val _trackingState = MutableStateFlow(1)
    val trackingState: StateFlow<Int> = _trackingState
    var state = 1

    private val _runsFlow = MutableStateFlow<List<Run>>(emptyList())
    val runsFlow: StateFlow<List<Run>> = _runsFlow

    fun getLocationFlow() {
        viewModelScope.launch {
            TrackingService.locationFlow.collect {
                if (it != null) {
                    Log.d("--------", "vm pos=$it size=${polyLines.size}")
                    Log.d("--------", "vm isTracking=$state")

                    if (state == 1)
                        polyLines.last().add(it)
                    _locationFlow.tryEmit(it)
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

    fun resumeRun() {
        polyLines.add(mutableListOf())
    }

    fun startRun() {
        polyLines.add(mutableListOf())
        mainRepository.startLocationService()

    }

    fun insertRun(run: Run) = viewModelScope.launch {
        mainRepository.insertRun(run)
        polyLines.clear()
    }

    fun deleteRun(run: Run) = viewModelScope.launch {
        mainRepository.deleteRun(run)
    }
}