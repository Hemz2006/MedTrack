package com.medtrack.ui.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.medtrack.health.HealthConnectGateway
import com.medtrack.reminders.DoseNotifier
import com.medtrack.health.HealthStatus

data class SetupStatus(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
    val batteryUnrestricted: Boolean,
    val health: HealthStatus,
)

suspend fun readSetupStatus(context: Context, health: HealthConnectGateway): SetupStatus {
    val notifications = context.getSystemService(NotificationManager::class.java)
    val alarms = context.getSystemService(AlarmManager::class.java)
    val power = context.getSystemService(PowerManager::class.java)
    return SetupStatus(
        notifications = notifications.areNotificationsEnabled() && channelAllowed(notifications),
        exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms(),
        fullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || notifications.canUseFullScreenIntent(),
        batteryUnrestricted = power.isIgnoringBatteryOptimizations(context.packageName),
        health = health.status(),
    )
}

/** A channel the app has not created yet counts as fine; the app creates it on start. */
private fun channelAllowed(nm: NotificationManager): Boolean =
    nm.getNotificationChannel(DoseNotifier.CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE

fun channelBlocked(context: Context): Boolean {
    val nm = context.getSystemService(NotificationManager::class.java)
    return nm.areNotificationsEnabled() && !channelAllowed(nm)
}
