package com.myprojects.modules.runningtracker.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: Run): Long

    @Delete
    suspend fun deleteRun(run: Run)

    @Update
    suspend fun updateRun(run: Run)

    @Query("SELECT * FROM runs_table ORDER BY startedAt DESC")
    fun getAllRunsSortedByDate(): Flow<List<Run>>

    @Query("SELECT * FROM runs_table WHERE id = :id LIMIT 1")
    fun getRoute(id: Int): List<Run>

    @Query("SELECT * FROM runs_table ORDER BY durationInMillis DESC")
    fun getAllRunsSortedByTimeInMillis(): Flow<List<Run>>

    @Query("SELECT * FROM runs_table ORDER BY caloriesBurned DESC")
    fun getAllRunsSortedByCaloriesBurned(): Flow<List<Run>>

    @Query("SELECT * FROM runs_table ORDER BY avgSpeedInKMH DESC")
    fun getAllRunsSortedByAvgSpeed(): Flow<List<Run>>

    @Query("SELECT * FROM runs_table ORDER BY distanceInMeters DESC")
    fun getAllRunsSortedByDistance(): Flow<List<Run>>

    @Query("SELECT SUM(durationInMillis) FROM runs_table")
    fun getTotalTimeInMillis(): Flow<Long>

    @Query("SELECT SUM(caloriesBurned) FROM runs_table")
    fun getTotalCaloriesBurned(): Flow<Int>

    @Query("SELECT SUM(distanceInMeters) FROM runs_table")
    fun getTotalDistanceInMeters(): Flow<Int>

    @Query("SELECT AVG(avgSpeedInKMH) FROM runs_table")
    fun getTotalAvgSpeed(): Flow<Float>
}