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
import android.os.PowerManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.myprojects.modules.runningtracker.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.myprojects.modules.runningtracker.ui.MainActivity
import com.myprojects.modules.runningtracker.Constants.NOTIFICATION_CHANNEL_ID
import com.myprojects.modules.runningtracker.R
import com.myprojects.modules.runningtracker.services.TrackingService
import dagger.hilt.android.qualifiers.ApplicationContext

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

    @Singleton
    @Provides
    fun provideTrackingService(@ApplicationContext app: Context) = TrackingService()

    @Singleton
    @Provides
    fun providePowerManager(app: Application): PowerManager {
        return app.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    @Singleton
    @Provides
    fun provideFusedLocationProviderClient(
        app: Application
    ): com.google.android.gms.location.FusedLocationProviderClient {
        return com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(app)
    }

    @Singleton
    @Provides
    fun provideNotificationBuilder(
        app: Application
    ): NotificationCompat.Builder {
        val activityIntent = Intent(app, MainActivity::class.java).apply {
            action = ACTION_SHOW_TRACKING_FRAGMENT
        }
        val pendingIntent = PendingIntent.getActivity(
            app,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(app, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_run)
            .setContentTitle("Running App")
            .setContentText("00:00:00")
            .setContentIntent(pendingIntent)
    }
}