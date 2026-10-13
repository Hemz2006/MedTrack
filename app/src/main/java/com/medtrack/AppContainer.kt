package com.medtrack

import android.content.Context
import androidx.room.Room
import com.medtrack.data.AppPrefs
import com.medtrack.data.MedTrackDatabase
import com.medtrack.data.MedicationRepository
import com.medtrack.data.PhotoStore
import com.medtrack.lookup.MedicationLookup
import com.medtrack.lookup.urlConnectionFetch
import com.medtrack.reminders.AlarmScheduler
import com.medtrack.reminders.DoseNotifier
import com.medtrack.reminders.ReminderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** App-wide singletons. [appScope] is for work that must outlive a screen (dose actions, saves). */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db: MedTrackDatabase = Room.databaseBuilder(context, MedTrackDatabase::class.java, "medtrack.db").build()
    val prefs = AppPrefs(context)
    val photos = PhotoStore(context)
    val lookup = MedicationLookup(urlConnectionFetch)
    val engine = ReminderEngine(db.medicationDao(), db.doseLogDao(), AlarmScheduler(context), DoseNotifier(context), prefs)
    val medications = MedicationRepository(db.medicationDao(), photos) { refreshReminders() }

    suspend fun refreshReminders() {
        engine.sync()
    }
}
