package com.vio.notificationlib.data.repository

import com.vio.notificationlib.data.datasource.FirebaseRemoteConfigDataSource
import com.vio.notificationlib.data.datasource.NotificationScheduler
import com.vio.notificationlib.domain.entities.NotificationConfig


class NotificationRepositoryImpl(
    private val firebaseDataSource: FirebaseRemoteConfigDataSource,
    private val scheduler: NotificationScheduler
) : NotificationRepository {
    override suspend fun fetchNotificationConfigs(): List<NotificationConfig> {
        return firebaseDataSource.getNotificationConfigs()
    }

    override suspend fun scheduleNotifications(configs: List<NotificationConfig>) {
        scheduler.scheduleNotifications(configs)
    }
}