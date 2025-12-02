package com.myprojects.modules.runningtracker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLatLng(location: Location)

    @Query("SELECT * FROM location_table WHERE run_id = :runId ORDER BY id ASC")
    suspend fun getLatLngByRunId(runId: Int): List<Location>
}