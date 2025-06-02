package com.vio.notificationlib.data.datasource

import com.vio.notificationlib.domain.entities.NotificationConfig


interface NotificationScheduler {
    fun scheduleNotifications(configs: List<NotificationConfig>)
    fun setSingleSchedule(notificationConfig: NotificationConfig)
    fun cancelNotification(config: NotificationConfig)
}