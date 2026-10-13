package com.medtrack.data

import android.content.Context
import java.time.Instant

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("medtrack", Context.MODE_PRIVATE)

    /** When reminders were last scanned; null before the first scan. */
    var lastScanAt: Instant?
        get() = prefs.getLong(KEY_LAST_SCAN, -1).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_LAST_SCAN) else putLong(KEY_LAST_SCAN, value.toEpochMilli())
        }.apply()

    var setupDone: Boolean
        get() = prefs.getBoolean(KEY_SETUP_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_DONE, value).apply()

    private companion object {
        const val KEY_LAST_SCAN = "lastScanAt"
        const val KEY_SETUP_DONE = "setupDone"
    }
}
