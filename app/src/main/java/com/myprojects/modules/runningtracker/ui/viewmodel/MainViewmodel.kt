package com.myprojects.modules.runningtracker.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.myprojects.modules.runningtracker.db.Run
import com.myprojects.modules.runningtracker.repository.MainRepository
import com.myprojects.modules.runningtracker.services.TrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewmodel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {
    var atasehir = LatLng(49.2510221, -123.00441)
    private val _locationFlow = MutableStateFlow<LatLng>(atasehir)
    val locationFlow: StateFlow<LatLng?> = _locationFlow

    private val _polyLineFlow = MutableStateFlow<List<List<LatLng>>>(emptyList())
    val polyLineFlow: StateFlow<List<List<LatLng>>> = _polyLineFlow
    val polyLines = mutableListOf(mutableListOf(atasehir))

    fun getLocationFlow() {
        viewModelScope.launch {
            TrackingService.locationFlow.collect {
                if (it != LatLng(0.0, 0.0)) {
                    Log.d("--------", "viewmodel pos=$it size=${polyLines.size}")
                    polyLines.last().add(it)
                    Log.d("--------", "viewmodel lines=$polyLines")
                    _locationFlow.tryEmit(it)
                    _polyLineFlow.tryEmit(polyLines.toList())
                }
            }
        }
        viewModelScope.launch {
            TrackingService.isTracking.collect {
                if (it) {
                    polyLines.add(mutableListOf())
                    Log.d(
                        "------------",
                        "isTracking=true size=${polyLines.size}-${polyLines.last().size}"
                    )
                }
            }
        }
    }

    fun insertRun(run: Run) = viewModelScope.launch {
        mainRepository.insertRun(run)
    }

    fun deleteRun(run: Run) = viewModelScope.launch {
        mainRepository.deleteRun(run)
    }
}