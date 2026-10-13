package com.medtrack.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medtrack.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** The single wake-up alarm fired. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val engine = context.container.engine
        runAsync { engine.sync() }
    }
}

/** Taken / Skip / Snooze pressed on a notification. */
class DoseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val logId = intent.getLongExtra(EXTRA_LOG_ID, -1)
        if (logId < 0) return
        val engine = context.container.engine
        runAsync {
            when (intent.action) {
                ACTION_TAKEN -> engine.markTaken(logId)
                ACTION_SKIP -> engine.markSkipped(logId)
                ACTION_SNOOZE -> engine.snooze(logId)
            }
        }
    }

    companion object {
        const val ACTION_TAKEN = "com.medtrack.action.TAKEN"
        const val ACTION_SKIP = "com.medtrack.action.SKIP"
        const val ACTION_SNOOZE = "com.medtrack.action.SNOOZE"
        const val EXTRA_LOG_ID = "logId"
    }
}

/** Reboot, clock or time-zone change, app update, or the exact-alarm permission changed: rebuild everything. */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = context.container
        runAsync { container.refreshReminders() }
    }
}
