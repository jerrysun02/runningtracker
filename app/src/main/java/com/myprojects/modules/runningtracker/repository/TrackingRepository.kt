package com.myprojects.modules.runningtracker.repository

import android.content.Context
import android.content.Intent
import com.myprojects.modules.runningtracker.Constants.ACTION_PAUSE_SERVICE
import com.myprojects.modules.runningtracker.Constants.ACTION_START_OR_RESUME_SERVICE
import com.myprojects.modules.runningtracker.Constants.ACTION_STOP_SERVICE
import com.myprojects.modules.runningtracker.db.Run
import com.myprojects.modules.runningtracker.db.RunDAO
import com.myprojects.modules.runningtracker.services.TrackingService
import javax.inject.Inject

class TrackingRepository @Inject constructor(
    private val context: Context,
    private val runDAO: RunDAO
) {
    suspend fun insertRun(run: Run): Long = runDAO.insertRun(run)

    suspend fun updateRun(run: Run) = runDAO.updateRun(run)

    suspend fun deleteRun(run: Run) = runDAO.deleteRun(run)

    fun getAllRunsSortedByDate() = runDAO.getAllRunsSortedByDate()

    fun getRoute(id: Int) = runDAO.getRoute(id)

    fun getAllRunsSortedByDistance() = runDAO.getAllRunsSortedByDistance()

    fun getAllRunsSortedByTimeInMills() = runDAO.getAllRunsSortedByTimeInMillis()

    fun getAllRunsSortedByAvgSpeed() = runDAO.getAllRunsSortedByAvgSpeed()

    fun getAllRunsSortedByCaloriesBurned() = runDAO.getAllRunsSortedByCaloriesBurned()

    fun getTotalAvgSpeed() = runDAO.getTotalAvgSpeed()

    fun getTotalDistance() = runDAO.getTotalDistanceInMeters()

    fun getTotalCaloriesBurned() = runDAO.getTotalCaloriesBurned()

    fun getTotalTimeInMillis() = runDAO.getTotalTimeInMillis()

    fun startLocationService() =
        Intent(context, TrackingService::class.java).also {
            it.action = ACTION_START_OR_RESUME_SERVICE
            context.startService(it)
        }

    fun pauseLocationService() =
        Intent(context, TrackingService::class.java).also {
            it.action = ACTION_PAUSE_SERVICE
            context.startService(it)
        }

    fun stopLocationService() =
        Intent(context, TrackingService::class.java).also {
            it.action = ACTION_STOP_SERVICE
            context.startService(it)
        }
}