package com.medtrack.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medtrack.MainActivity
import java.time.Instant

/** Holds the app's single wake-up alarm. */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun set(at: Instant?) {
        val operation = PendingIntent.getBroadcast(
            context, 0, Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (at == null) {
            alarmManager.cancel(operation)
            return
        }
        if (canScheduleExact()) {
            // Alarm-clock alarms are exempt from Doze and survive aggressive OEM battery savers best.
            val show = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at.toEpochMilli(), show), operation)
        } else {
            // Degraded: may ring late. Settings and Today show a warning until exact alarms are allowed.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), operation)
        }
    }
}
