package com.myprojects.modules.runningtracker.db

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "running_table")
data class Run(
    var start: String = "",
    var end: String = "",
    var img: Bitmap? = null,
    var timestamp: Long = 0L,
    var avgSpeedInKMH: Float = 0f,
    var distanceInMeters: Int = 0,
    var timeInMillis: Long = 0L,
    var caloriesBurned: Int = 0,
    @ColumnInfo(name = "locationList")
    var locationList: List<List<LatLng>>
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}