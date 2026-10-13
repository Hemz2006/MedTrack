package com.medtrack.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medtrack.schedule.MealType
import com.medtrack.schedule.Schedule
import com.medtrack.schedule.ScheduleRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: MedTrackDatabase
    private val now = Instant.parse("2026-10-13T02:30:00Z")
    private val med = MedicationEntity(
        genericName = "pantoprazole",
        brandName = "Pan 40",
        ingredients = listOf(Ingredient("pantoprazole", "40 MG", "C16H15F2N3O4S")),
        schedule = Schedule(ScheduleRule.AfterMeal(MealType.BREAKFAST, 30, LocalTime.of(10, 0)), LocalDate.parse("2026-10-13")),
        createdAt = now,
    )

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MedTrackDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    private fun log(medId: Long, due: Instant, status: DoseStatus = DoseStatus.PENDING) =
        DoseLogEntity(medicationId = medId, dueAt = due, status = status, trigger = DoseTrigger.SCHEDULE, forDate = java.time.LocalDate.ofInstant(due, java.time.ZoneOffset.UTC))

    @Test fun medicationRoundTripsScheduleAndIngredients() = runBlocking {
        val id = db.medicationDao().insert(med)
        assertEquals(med.copy(id = id), db.medicationDao().get(id))
    }

    @Test fun pausingKeepsHistory() = runBlocking {
        val id = db.medicationDao().insert(med)
        db.doseLogDao().insertIgnore(log(id, now))
        db.medicationDao().setActive(id, false)
        assertEquals(emptyList<MedicationEntity>(), db.medicationDao().active())
        assertEquals(1, db.doseLogDao().since(Instant.EPOCH).size)
    }

    @Test fun deletingCascadesLogs() = runBlocking {
        val id = db.medicationDao().insert(med)
        db.doseLogDao().insertIgnore(log(id, now))
        db.medicationDao().delete(db.medicationDao().get(id)!!)
        assertNull(db.medicationDao().get(id))
        assertEquals(0, db.doseLogDao().since(Instant.EPOCH).size)
    }

    @Test fun duplicateDoseIsIgnored() = runBlocking {
        val id = db.medicationDao().insert(med)
        val first = db.doseLogDao().insertIgnore(log(id, now))
        val second = db.doseLogDao().insertIgnore(log(id, now, DoseStatus.MISSED))
        assertEquals(-1L, second)
        assertEquals(DoseStatus.PENDING, db.doseLogDao().get(first)!!.status)
    }

    @Test fun markMissedOnlyTouchesPending() = runBlocking {
        val id = db.medicationDao().insert(med)
        val pending = db.doseLogDao().insertIgnore(log(id, now))
        val taken = db.doseLogDao().insertIgnore(log(id, now.plusSeconds(60), DoseStatus.TAKEN))
        db.doseLogDao().markMissed(listOf(pending, taken))
        assertEquals(DoseStatus.MISSED, db.doseLogDao().get(pending)!!.status)
        assertEquals(DoseStatus.TAKEN, db.doseLogDao().get(taken)!!.status)
    }

    @Test fun snoozeAndSetStatus() = runBlocking {
        val id = db.medicationDao().insert(med)
        val logId = db.doseLogDao().insertIgnore(log(id, now))
        db.doseLogDao().snooze(logId, now.plusSeconds(600))
        assertEquals(now.plusSeconds(600), db.doseLogDao().get(logId)!!.snoozedUntil)
        db.doseLogDao().setStatus(logId, DoseStatus.TAKEN, now.plusSeconds(700))
        val done = db.doseLogDao().get(logId)!!
        assertEquals(DoseStatus.TAKEN, done.status)
        assertNull(done.snoozedUntil)
        assertEquals(0, db.doseLogDao().pending().size)
    }

    @Test fun displayName() {
        assertEquals("Pan 40 (pantoprazole)", med.displayName())
        assertEquals("pantoprazole", med.copy(brandName = " ").displayName())
    }
}
