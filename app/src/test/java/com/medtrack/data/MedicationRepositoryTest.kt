package com.medtrack.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MedicationRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, MedTrackDatabase::class.java).allowMainThreadQueries().build()
    private val photos = PhotoStore(context)
    private var changes = 0
    private val repo = MedicationRepository(db.medicationDao(), photos) { changes++ }
    private val med = newMedication(LocalDate.parse("2026-10-13"), Instant.parse("2026-10-13T00:00:00Z")).copy(genericName = "metformin")

    @Test fun saveInsertsThenUpdates() = runBlocking {
        val id = repo.save(med)
        repo.save(db.medicationDao().get(id)!!.copy(notes = "with food"))
        assertEquals("with food", db.medicationDao().get(id)!!.notes)
        assertEquals(2, changes)
    }

    @Test fun failingRebuildDoesNotFailSave() = runBlocking {
        val failing = MedicationRepository(db.medicationDao(), photos) { throw IllegalStateException("boom") }
        val id = failing.save(med)
        assertEquals("metformin", db.medicationDao().get(id)!!.genericName)
        assertEquals(1, db.medicationDao().active().size)
    }

    @Test fun pauseNotifies() = runBlocking {
        val id = repo.save(med)
        repo.setActive(id, false)
        assertFalse(db.medicationDao().get(id)!!.active)
        assertEquals(2, changes)
    }

    @Test fun deleteRemovesRowAndPhoto() = runBlocking {
        val photo = File(photos.photosDir, "p.jpg").apply { writeText("x") }
        val id = repo.save(med.copy(photoPath = photo.absolutePath))
        repo.delete(id)
        assertNull(db.medicationDao().get(id))
        assertFalse(photo.exists())
        assertEquals(2, changes)
    }
}
