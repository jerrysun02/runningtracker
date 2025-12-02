package com.myprojects.modules.runningtracker.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "location_table")
data class Location(
    @ColumnInfo(name = "run_id") val runId: Int,
    @ColumnInfo(name = "sub_run_id") val subRunId: Int,
    @ColumnInfo(name = "lat_lng") val latLng: LatLng
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}