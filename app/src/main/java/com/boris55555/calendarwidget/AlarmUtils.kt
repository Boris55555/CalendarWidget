package com.boris55555.calendarwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import java.util.Calendar

object AlarmUtils {
    fun scheduleWidgetUpdate(context: Context, intervalMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
            action = "com.boris55555.calendarwidget.ACTION_AUTO_UPDATE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        if (intervalMillis == 86400000L) { // Daily at midnight
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 1) // 1 second past midnight
                set(Calendar.MILLISECOND, 0)
            }
            
            // Use exact alarm for midnight reset to ensure it happens on time
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                // Fallback to inexact if exact alarm permission is missing
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            // Periodic updates (hourly etc)
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + intervalMillis,
                intervalMillis,
                pendingIntent
            )
        }
    }
}