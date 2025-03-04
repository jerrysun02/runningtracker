package com.myprojects.modules.runningtracker.db

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "running_table")
data class Run(
 //   @PrimaryKey(autoGenerate = true)
 //   @ColumnInfo(name = "id")
 //   var id: Int ?= null,
    @ColumnInfo(name = "start")
    var start: String = "",
    @ColumnInfo(name = "end")
    var end: String = "",
    @ColumnInfo(name = "img")
    var img: Bitmap? = null,
    @ColumnInfo(name = "timestamp")
    var timestamp: Long = 0L,
    @ColumnInfo(name = "avgSpeedInKMH")
    var avgSpeedInKMH: Float = 0f,
    @ColumnInfo(name = "distanceInMeters")
    var distanceInMeters: Int = 0,
    @ColumnInfo(name = "timeInMillis")
    var timeInMillis: Long = 0L,
    @ColumnInfo(name = "caloriesBurned")
    var caloriesBurned: Int = 0,
    @ColumnInfo(name = "locationList")
    var locationList: List<List<LatLng>>
) {
    @PrimaryKey(autoGenerate = true)
   // @ColumnInfo(name = "id")
    var id: Int = 0
}