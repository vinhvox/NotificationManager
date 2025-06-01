package com.vio.notificationlib.domain.usecases

import com.vio.notificationlib.domain.entities.NotificationConfig

interface ScheduleNotificationsUseCase {
    suspend fun execute(configs: List<NotificationConfig>)
}