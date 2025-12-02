package com.myprojects.modules.runningtracker.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.myprojects.modules.runningtracker.Constants.RUNNING_DATABASE_NAME
import com.myprojects.modules.runningtracker.db.LocationDAO
import com.myprojects.modules.runningtracker.db.RunDAO
import com.myprojects.modules.runningtracker.db.RunningDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideAppDb(app: Application): RunningDatabase {
        return Room.databaseBuilder(app, RunningDatabase::class.java, RUNNING_DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun providesRunDao(db: RunningDatabase): RunDAO {
        return db.getRunDao()
    }

    @Singleton
    @Provides
    fun providesLocationDao(db: RunningDatabase): LocationDAO {
        return db.getLocationDao()
    }

    @Singleton
    @Provides
    fun providesContext(app: Application): Context = app.applicationContext
}