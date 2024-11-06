package com.myprojects.modules.runningtracker.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.myprojects.modules.runningtracker.services.Polylines
import com.myprojects.modules.runningtracker.services.TrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewmodel @Inject constructor(
    //val mainRepository: MainRepository
) : ViewModel() {
    /*var atasehir = LatLng(49.2510221, -123.00441)
    private val _locationFlow = MutableStateFlow<LatLng>(atasehir)
    val locationFlow: StateFlow<LatLng?> = _locationFlow

    private val _polyLinesFlow = MutableStateFlow<Polylines?>(null)
    val polyLinesFlow: StateFlow<Polylines?> = _polyLinesFlow*/

    private val _polyLineFlow = MutableStateFlow<List<List<LatLng>>>(emptyList())
    val polyLineFlow: StateFlow<List<List<LatLng>>> = _polyLineFlow
    fun getLocationFlow() {
    /*    viewModelScope.launch {
            TrackingService.pathPointsFlow.collect {
                it?.let {
                    _locationFlow.value = it
                    Log.d("-------", "viewModel**loc=" + it.toString())
                }
            }
        }
        viewModelScope.launch {
            TrackingService.polyLinesFlow.collect {
                Log.d("-------", "viewModel**poly-ss=" + it.toString())
                it?.let {
                    _polyLinesFlow.value = it
                    Log.d("-------", "viewModel**poly=" + it.toString())
                }
            }
        }*/
        viewModelScope.launch {
            TrackingService.polylineFlow.collect {
                Log.d("-------", "viewModel**poly22=$it")
                if (it.size > 2) {
                    _polyLineFlow.tryEmit(it)//.subList(1, it.size - 1)
                }
            }
        }
        viewModelScope.launch {
            TrackingService.isTracking.collect {


            }
        }
    }
}