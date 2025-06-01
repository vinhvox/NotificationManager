package com.vio.notificationlib.data.repository

import com.vio.notificationlib.data.datasource.FirebaseRemoteConfigDataSource
import com.vio.notificationlib.data.datasource.WorkManagerScheduler
import com.vio.notificationlib.domain.entities.NotificationConfig

class NotificationRepositoryImpl(
    private val firebaseDataSource: FirebaseRemoteConfigDataSource,
    private val workManagerScheduler: WorkManagerScheduler
) : NotificationRepository {
    override suspend fun fetchNotificationConfigs(): List<NotificationConfig> {
        return firebaseDataSource.fetchNotificationConfigs()
    }

    override suspend fun scheduleNotifications(configs: List<NotificationConfig>) {
        workManagerScheduler.scheduleNotifications(configs)
    }
}