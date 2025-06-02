package com.vio.notificationlib.domain.entities


import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NotificationConfig(
    val id: Int,
    val title: String,
    val body: String,
    val scheduleType: String, // "daily", "weekly", "monthly"
    val scheduleTime: TimeConfig,
    val days: List<Int>? = null, // For weekly (1-7) or monthly (1-31)
    val repeat: Boolean,
    val targetFeature: String? = null,
    val customLayout: Int? = null
) : Parcelable
