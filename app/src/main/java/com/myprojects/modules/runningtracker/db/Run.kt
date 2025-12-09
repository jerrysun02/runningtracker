package com.myprojects.modules.runningtracker.db

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "runs_table")
data class Run(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @ColumnInfo(name = "startedAt")
    var startedAt: Long = 0L, // Renamed from timestamp
    @ColumnInfo(name = "durationInMillis")
    var durationInMillis: Long = 0L, // Renamed from timeInMillis
    @ColumnInfo(name = "distanceInMeters")
    var distanceInMeters: Int = 0,
    @ColumnInfo(name = "avgSpeedInKMH")
    var avgSpeedInKMH: Float = 0f,
    @ColumnInfo(name = "caloriesBurned")
    var caloriesBurned: Int = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var img: Bitmap? = null,
    @ColumnInfo(name = "locationList")
    var locationList: List<List<LatLng>> = emptyList()
)