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

    /** The Today screen's layout: "time" (grouped by time, the default) or "medication". */
    var todayView: String
        get() = prefs.getString(KEY_TODAY_VIEW, null) ?: "time"
        set(value) = prefs.edit().putString(KEY_TODAY_VIEW, value).apply()

    /** The user closed Today's "meal reminders use their fallback times" alert; don't show it again. */
    var healthWarningHidden: Boolean
        get() = prefs.getBoolean(KEY_HEALTH_WARNING_HIDDEN, false)
        set(value) = prefs.edit().putBoolean(KEY_HEALTH_WARNING_HIDDEN, value).apply()

    private companion object {
        const val KEY_HEALTH_WARNING_HIDDEN = "healthWarningHidden"
        const val KEY_LAST_SCAN = "lastScanAt"
        const val KEY_SETUP_DONE = "setupDone"
        const val KEY_TODAY_VIEW = "todayView"
    }
}
