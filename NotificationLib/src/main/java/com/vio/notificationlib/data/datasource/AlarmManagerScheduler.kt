package com.vio.notificationlib.data.datasource

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.vio.notificationlib.domain.entities.NotificationConfig
import com.vio.notificationlib.presentation.NotificationReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

class AlarmNotificationScheduler(private val context: Context) : NotificationScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    override fun scheduleNotifications(configs: List<NotificationConfig>) {
        Log.d(TAG, "Scheduling ${configs.size} notification configs")
        if (configs.isEmpty()) {
            Log.w(TAG, "No configurations provided for scheduling")
            return
        }
        configs.forEach { config ->
            cancelNotification(config) // Cancel existing alarms
            Log.d(TAG, "Scheduling config: id=${config.id}, type=${config.scheduleType}")
            when (config.scheduleType.lowercase()) {
                "daily" -> scheduleDaily(config)
                "weekly" -> scheduleWeekly(config)
                "monthly" -> scheduleMonthly(config)
                else -> Log.w(TAG, "Unknown schedule type: ${config.scheduleType}")
            }
        }
    }

    override fun setSingleSchedule(config: NotificationConfig) {
        when (config.scheduleType.lowercase()) {
            "daily" -> scheduleDaily(config)
            "weekly" -> scheduleWeekly(config)
            "monthly" -> scheduleMonthly(config)
            else -> Log.w(TAG, "Unknown schedule type: ${config.scheduleType}")
        }
    }

    override fun cancelNotification(config: NotificationConfig) {
        val days = when (config.scheduleType.lowercase()) {
            "weekly", "monthly" -> config.days ?: listOf(1)
            else -> listOf(0)
        }
        days.forEach { day ->
            val uniqueId = getUniqueId(config.id, day)
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                Log.d(TAG, "Cancelled notification: id=${config.id}, day=$day")
            }
        }
    }

    override fun cancelAllNotifications(configs: List<NotificationConfig>) {
        configs.forEach { config ->
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                config.id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                Log.d(TAG, "Cancelled notification: id=${config.id}")
            }
        }
    }

    private fun scheduleDaily(config: NotificationConfig) {

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, config.scheduleTime.hour)
        calendar.set(Calendar.MINUTE, config.scheduleTime.minute)
        calendar.set(Calendar.SECOND, 0)
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DATE, 1)
        }
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("config", config)
            putExtra("time_show", calendar.timeInMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            config.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d(
                TAG,
                "Enqueued inexact alarm (no SCHEDULE_EXACT_ALARM): id=${config.id}, trigger=${calendar.timeInMillis.longToDateString()}"
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
        Log.e(
            TAG,
            "ScheduleDay: ${calendar.timeInMillis.longToDateString("dd/MM/yy HH:mm")} ${config.scheduleTime.hour}:${config.scheduleTime.minute}"
        )
    }

    private fun scheduleWeekly(config: NotificationConfig) {
        val days = config.days ?: listOf(1, 2, 3, 4, 5, 6, 7)

        days.forEach { day ->
            val calendar = Calendar.getInstance()
            try {
                calendar.set(Calendar.DAY_OF_WEEK, day)
            } catch (_: Exception) {
            }
            calendar.set(Calendar.HOUR_OF_DAY, config.scheduleTime.hour)
            calendar.set(Calendar.MINUTE, config.scheduleTime.minute)
            calendar.set(Calendar.SECOND, 0)
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DATE, 7)
            }
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("config", config)
                putExtra("time_show", calendar.timeInMillis)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                config.id + 19 + day,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(
                        TAG,
                        "Enqueued inexact alarm (no SCHEDULE_EXACT_ALARM): id=${config.id}, day=$day, trigger=${calendar.timeInMillis.longToDateString()}"
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                Log.e(
                    TAG,
                    "ScheduleWeek: ${calendar.timeInMillis.longToDateString("dd/MM/yy HH:mm")} ${config.scheduleTime.hour}:${config.scheduleTime.minute} day=$day"
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: Missing SCHEDULE_EXACT_ALARM permission", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarm: id=${config.id}, day=$day", e)
            }
        }
    }

    private fun scheduleMonthly(config: NotificationConfig) {
        val days = config.days ?: (1..31).toList()
        days.forEach { day ->
            if (day in 1..31) {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                calendar.set(Calendar.HOUR_OF_DAY, config.scheduleTime.hour)
                calendar.set(Calendar.MINUTE, config.scheduleTime.minute)
                calendar.set(Calendar.SECOND, 0)
                when (currentMonth) {
                    Calendar.JANUARY, Calendar.MARCH, Calendar.MAY, Calendar.JULY, Calendar.AUGUST, Calendar.OCTOBER, Calendar.DECEMBER -> {
                        if (calendar.timeInMillis < System.currentTimeMillis()) {
                            calendar.add(Calendar.DATE, 31)
                        }
                        val intent = Intent(context, NotificationReceiver::class.java).apply {
                            putExtra("config", config)
                            putExtra("time_show", calendar.timeInMillis)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            config.id + 999 + day,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                            Log.d(
                                TAG,
                                "Enqueued inexact alarm (no SCHEDULE_EXACT_ALARM): id=${config.id}, day=$day, trigger=${calendar.timeInMillis.longToDateString()}"
                            )
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        }
                    }
                    Calendar.APRIL, Calendar.JUNE, Calendar.SEPTEMBER, Calendar.NOVEMBER -> {
                        if (calendar.timeInMillis < System.currentTimeMillis()) {
                            calendar.add(Calendar.DATE, 30)
                        }
                        val intent = Intent(context, NotificationReceiver::class.java).apply {
                            putExtra("config", config)
                            putExtra("time_show", calendar.timeInMillis)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            config.id + 999 + day,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                            Log.d(
                                TAG,
                                "Enqueued inexact alarm (no SCHEDULE_EXACT_ALARM): id=${config.id}, day=$day, trigger=${calendar.timeInMillis.longToDateString()}"
                            )
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        }
                    }
                    Calendar.FEBRUARY -> {
                        val cal: GregorianCalendar = GregorianCalendar.getInstance() as GregorianCalendar
                        if (cal.isLeapYear(calendar.get(Calendar.YEAR))) {
                            if (calendar.timeInMillis < System.currentTimeMillis()) {
                                calendar.add(Calendar.DATE, 29)
                            }
                            val intent = Intent(context, NotificationReceiver::class.java).apply {
                                putExtra("config", config)
                                putExtra("time_show", calendar.timeInMillis)
                            }
                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                config.id + 999 + day,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        } else {
                            if (calendar.timeInMillis < System.currentTimeMillis()) {
                                calendar.add(Calendar.DATE, 28)
                            }
                            val intent = Intent(context, NotificationReceiver::class.java).apply {
                                putExtra("config", config)
                                putExtra("time_show", calendar.timeInMillis)
                            }
                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                config.id + 999 + day,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    calendar.timeInMillis,
                                    pendingIntent
                                )
                                Log.d(
                                    TAG,
                                    "Enqueued inexact alarm (no SCHEDULE_EXACT_ALARM): id=${config.id}, day=$day, trigger=${calendar.timeInMillis.longToDateString()}"
                                )
                            } else {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    calendar.timeInMillis,
                                    pendingIntent
                                )
                            }
                        }
                    }
                }
                Log.e(
                    TAG,
                    "ScheduleMonth: ${calendar.timeInMillis.longToDateString("dd/MM/yy HH:mm")} ${config.scheduleTime.hour}:${config.scheduleTime.minute}"
                )
            }
        }
    }

    private fun getUniqueId(id: Int, day: Int): Int {
        return "${id}_day$day".hashCode()
    }

    private fun Long.longToDateString(format: String = "dd/MM/yyyy HH:mm:ss"): String {
        return SimpleDateFormat(format, Locale.getDefault()).format(Date(this))
    }

    companion object {
        private const val TAG = "AlarmNotificationScheduler"
    }
}