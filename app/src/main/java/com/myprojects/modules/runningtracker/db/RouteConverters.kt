package com.myprojects.modules.runningtracker.db

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RouteConverters {
    @TypeConverter
    fun fromLocationToJSON(locationList: List<List<LatLng>>): String {
        return Gson().toJson(locationList)
    }

    @TypeConverter
    fun fromJsonToLocation(jsonLocation: String): List<List<LatLng>> {
        val type = object : TypeToken<List<List<LatLng>>>() {}.type
        return Gson().fromJson(jsonLocation, type)
    }
}