package com.vio.notificationlib.domain.entities

data class NotificationConfig(
    val id: String,
    val title: String,
    val body: String,
    val scheduleType: String, // daily, weekly, monthly
    val scheduleTime: TimeConfig, // hour and minute
    val days: List<Int>? = null, // Days of week (1-7) or month (1-31)
    val repeat: Boolean,
    val targetFeature: String? = null, // Feature or action identifier
    val customLayout: Int? = null // Resource ID for custom notification layout
)