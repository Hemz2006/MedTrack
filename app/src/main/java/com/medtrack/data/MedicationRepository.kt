package com.medtrack.data

/** Writes to medications, then runs [onChanged] so reminders are rebuilt. */
class MedicationRepository(
    private val dao: MedicationDao,
    private val photos: PhotoStore,
    private val onChanged: suspend () -> Unit,
) {
    suspend fun save(medication: MedicationEntity): Long {
        val id = if (medication.id == 0L) dao.insert(medication) else medication.id.also { dao.update(medication) }
        onChanged()
        return id
    }

    suspend fun setActive(id: Long, active: Boolean) {
        dao.setActive(id, active)
        onChanged()
    }

    suspend fun delete(id: Long) {
        val medication = dao.get(id) ?: return
        dao.delete(medication)
        photos.delete(medication.photoPath)
        onChanged()
    }
}
