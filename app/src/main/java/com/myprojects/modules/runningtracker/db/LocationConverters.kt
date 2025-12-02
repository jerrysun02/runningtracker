package com.myprojects.modules.runningtracker.db

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocationConverters {
    @TypeConverter
    fun fromLocationToJSON(locationList: List<List<LatLng>>): String {
        return Gson().toJson(locationList)
    }

    @TypeConverter
    fun fromJsonToLocation(jsonLocation: String): List<List<LatLng>> {
        val type = object : TypeToken<List<List<LatLng>>>() {}.type
        return Gson().fromJson(jsonLocation, type)
    }

    @TypeConverter
    fun fromLatLng(latLng: LatLng?): String? {
        return Gson().toJson(latLng)
    }

    @TypeConverter
    fun toLatLng(latLngString: String?): LatLng? {
        return Gson().fromJson(latLngString, LatLng::class.java)
    }
}