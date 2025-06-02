package com.vio.notificationlib.utils

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.vio.notificationlib.data.datasource.AlarmNotificationScheduler
import com.vio.notificationlib.data.datasource.FirebaseRemoteConfigDataSource
import com.vio.notificationlib.data.repository.NotificationRepositoryImpl
import com.vio.notificationlib.domain.entities.NotificationConfig
import com.vio.notificationlib.domain.usecases.FetchNotificationConfigUseCase
import com.vio.notificationlib.domain.usecases.ScheduleNotificationsUseCase

class NotificationHelper(
    private val context: Context,
    private val activityClass: Class<*>
) {
    private val fetchUseCase: FetchNotificationConfigUseCase by lazy {
        val firebaseDataSource =
            FirebaseRemoteConfigDataSource(FirebaseRemoteConfig.getInstance(), activityClass)
        val scheduler = AlarmNotificationScheduler(context)
        val repository = NotificationRepositoryImpl(firebaseDataSource, scheduler)
        object : FetchNotificationConfigUseCase {
            override suspend fun execute() = repository.fetchNotificationConfigs()
        }
    }

    private val scheduleUseCase: ScheduleNotificationsUseCase by lazy {
        val firebaseDataSource =
            FirebaseRemoteConfigDataSource(FirebaseRemoteConfig.getInstance(), activityClass)
        val scheduler = AlarmNotificationScheduler(context)
        val repository = NotificationRepositoryImpl(firebaseDataSource, scheduler)
        object : ScheduleNotificationsUseCase {
            override suspend fun execute(configs: List<NotificationConfig>) {
                configs.forEach {
                    Log.e(TAG, "execute: $it")
                }
                repository.scheduleNotifications(configs)
            }
        }
    }

    suspend fun scheduleNotifications() {
        Log.d(TAG, "Starting notification scheduling")
        val configs = fetchUseCase.execute()
        scheduleUseCase.execute(configs)
        Log.d(TAG, "Completed notification scheduling")
    }

    companion object {
        private const val TAG = "NotificationHelper"
    }
}