package com.medtrack.data

import android.util.Log
import kotlinx.coroutines.CancellationException

/** Writes to medications, then runs [onChanged] so reminders are rebuilt. */
class MedicationRepository(
    private val dao: MedicationDao,
    private val photos: PhotoStore,
    private val onChanged: suspend () -> Unit,
) {
    suspend fun save(medication: MedicationEntity): Long {
        val id = if (medication.id == 0L) dao.insert(medication) else medication.id.also { dao.update(medication) }
        notifyChanged()
        return id
    }

    suspend fun setActive(id: Long, active: Boolean) {
        dao.setActive(id, active)
        notifyChanged()
    }

    suspend fun delete(id: Long) {
        val medication = dao.get(id) ?: return
        dao.delete(medication)
        photos.delete(medication.photoPath)
        notifyChanged()
    }

    /** The data is already saved; a failed rebuild must not make the caller retry (and double-insert). */
    private suspend fun notifyChanged() {
        try {
            onChanged()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("MedTrack", "Rebuilding reminders failed; the next resume rescans", e)
        }
    }
}
