package com.myprojects.modules.runningtracker.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Run::class],
    version = 7,
    exportSchema = false
)

@TypeConverters(BitmapConverters::class, LocationConverters::class)
abstract class RunningDatabase : RoomDatabase() {
    abstract fun getRunDao(): RunDAO
}