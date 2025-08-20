package com.vio.notificationlib.data.repository

import com.vio.notificationlib.domain.entities.NotificationConfig

interface NotificationRepository {
    suspend fun fetchNotificationConfigs(): List<NotificationConfig>
    suspend fun scheduleNotifications(configs: List<NotificationConfig>)
    suspend fun cancelScheduleNotifications(configs: List<NotificationConfig>)
}