package com.vio.notificationlib.domain.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class TimeConfig(
    val hour: Int, // 0-23
    val minute: Int // 0-59
) : Parcelable