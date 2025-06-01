package com.vio.notificationlib.data.datasource

import android.content.Context
import android.util.Log
import androidx.work.*
import com.vio.notificationlib.domain.entities.NotificationConfig
import com.vio.notificationlib.domain.entities.TimeConfig
import com.vio.notificationlib.presentation.NotificationWorker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WorkManagerScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    fun scheduleNotifications(configs: List<NotificationConfig>) {
        // Clear all previous work
        workManager.cancelAllWork()
        Log.d(TAG, "Cleared all previous scheduled notifications")

        // Schedule new notifications
        configs.forEach { config ->
            when (config.scheduleType.lowercase()) {
                "daily" -> scheduleDaily(config)
                "weekly" -> scheduleWeekly(config)
                "monthly" -> scheduleMonthly(config)
            }
        }
    }

    private fun scheduleDaily(config: NotificationConfig) {
        val delay = calculateDelay(config.scheduleTime)
        logScheduleTime(config, delay)
        scheduleWork(config, delay, if (config.repeat) TimeUnit.DAYS.toMillis(1) else null)
    }

    private fun scheduleWeekly(config: NotificationConfig) {
        val days = config.days ?: listOf(1, 2, 3, 4, 5, 6, 7) // Default: all days
        days.forEach { day ->
            val delay = calculateDelayForDay(config.scheduleTime, day, isMonthly = false)
            logScheduleTime(config, delay, day, isMonthly = false)
            scheduleWork(config, delay, if (config.repeat) TimeUnit.DAYS.toMillis(7) else null)
        }
    }

    private fun scheduleMonthly(config: NotificationConfig) {
        val days = config.days ?: (1..31).toList() // Default: all days
        days.forEach { day ->
            if (day in 1..31) { // Validate day of month
                val delay = calculateDelayForDay(config.scheduleTime, day, isMonthly = true)
                logScheduleTime(config, delay, day, isMonthly = true)
                scheduleWork(config, delay, if (config.repeat) TimeUnit.DAYS.toMillis(30) else null)
            }
        }
    }

    private fun calculateDelay(timeConfig: TimeConfig): Long {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, timeConfig.hour)
        calendar.set(Calendar.MINUTE, timeConfig.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= currentTime) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis - currentTime
    }

    private fun calculateDelayForDay(timeConfig: TimeConfig, targetDay: Int, isMonthly: Boolean): Long {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, timeConfig.hour)
        calendar.set(Calendar.MINUTE, timeConfig.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (isMonthly) {
            calendar.set(Calendar.DAY_OF_MONTH, targetDay)
            if (calendar.timeInMillis <= currentTime) {
                calendar.add(Calendar.MONTH, 1)
            }
        } else {
            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val daysToAdd = (targetDay - currentDayOfWeek + 7) % 7
            calendar.add(Calendar.DAY_OF_MONTH, if (daysToAdd == 0 && calendar.timeInMillis <= currentTime) 7 else daysToAdd)
        }

        return calendar.timeInMillis - currentTime
    }

    private fun logScheduleTime(config: NotificationConfig, delay: Long, day: Int? = null, isMonthly: Boolean = false) {
        val scheduledTime = System.currentTimeMillis() + delay
        val formattedTime = dateFormat.format(Date(scheduledTime))
        val dayInfo = if (day != null) {
            if (isMonthly) " on day $day of month" else " on day $day of week"
        } else {
            ""
        }
        Log.d(TAG, "Scheduled notification '${config.id}' (${config.title}) at $formattedTime$dayInfo, repeat: ${config.repeat}")
    }

    private fun scheduleWork(config: NotificationConfig, delay: Long, repeatInterval: Long?) {
        val data = Data.Builder()
            .putString("id", config.id)
            .putString("title", config.title)
            .putString("body", config.body)
            .putString("target_feature", config.targetFeature)
            .putInt("custom_layout", config.customLayout ?: -1)
            .build()

        if (repeatInterval != null) {
            val validRepeatInterval = maxOf(repeatInterval, TimeUnit.MINUTES.toMillis(15))
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                validRepeatInterval, TimeUnit.MILLISECONDS
            )
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("notification")
                .build()
            workManager.enqueueUniquePeriodicWork(
                "${config.id}_${delay}",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        } else {
            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("notification")
                .build()
            workManager.enqueueUniqueWork(
                "${config.id}_${delay}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    companion object {
        private const val TAG = "WorkManagerScheduler"
    }
}