package com.medtrack.health

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medtrack.data.AppPrefs
import com.medtrack.data.DoseTrigger
import com.medtrack.data.MedTrackDatabase
import com.medtrack.data.MedicationEntity
import com.medtrack.reminders.AlarmScheduler
import com.medtrack.reminders.DoseNotifier
import com.medtrack.reminders.ReminderEngine
import com.medtrack.schedule.Meal
import com.medtrack.schedule.MealType
import com.medtrack.schedule.Schedule
import com.medtrack.schedule.ScheduleRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class MealCheckerTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(s: String): Instant = LocalDateTime.parse(s).atZone(zone).toInstant()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: MedTrackDatabase
    private lateinit var engine: ReminderEngine
    private var now = at("2026-10-13T08:15")
    private var meals: List<Meal>? = emptyList()
    private lateinit var checker: MealChecker
    private var medId = 0L

    @Before fun setUp() = runBlocking {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        db = Room.inMemoryDatabaseBuilder(context, MedTrackDatabase::class.java).allowMainThreadQueries().build()
        val clock = { Clock.fixed(now, zone) }
        engine = ReminderEngine(db.medicationDao(), db.doseLogDao(), AlarmScheduler(context), DoseNotifier(context).apply { ensureChannel() }, AppPrefs(context), clock)
        checker = MealChecker({ _, _ -> meals }, db.medicationDao(), db.doseLogDao(), engine, clock)
        medId = db.medicationDao().insert(
            MedicationEntity(
                genericName = "metformin",
                schedule = Schedule(ScheduleRule.AfterMeal(MealType.BREAKFAST, 30, LocalTime.of(10, 0)), LocalDate.parse("2026-10-01")),
                createdAt = now,
            ),
        )
        engine.sync()
    }

    private fun logs() = runBlocking { db.doseLogDao().since(Instant.EPOCH) }
    private fun nextAlarm() = context.getSystemService(AlarmManager::class.java).nextAlarmClock?.let { Instant.ofEpochMilli(it.triggerTime) }
    private fun doseNotifications() = context.getSystemService(NotificationManager::class.java).activeNotifications.filter { it.tag == DoseNotifier.TAG }

    @Test fun breakfastSchedulesDoseAndReplacesFallback() = runBlocking {
        meals = listOf(Meal(at("2026-10-13T08:10"), MealType.BREAKFAST))
        checker.check()
        val log = logs().single()
        assertEquals(at("2026-10-13T08:40"), log.dueAt)
        assertEquals(DoseTrigger.MEAL, log.trigger)
        assertEquals(at("2026-10-13T08:40"), nextAlarm())
        now = at("2026-10-13T08:40"); engine.sync()
        assertEquals(1, doseNotifications().size)
        now = at("2026-10-13T08:45"); engine.sync()
        assertEquals(at("2026-10-13T10:40"), nextAlarm()) // missed-check, not the 10:00 fallback
    }

    @Test fun lateMealRingsImmediately() = runBlocking {
        now = at("2026-10-13T09:30")
        meals = listOf(Meal(at("2026-10-13T08:00"), MealType.BREAKFAST))
        checker.check()
        assertEquals(at("2026-10-13T09:30"), logs().single().dueAt)
        assertEquals(1, doseNotifications().size)
    }

    @Test fun secondMealDoesNotAddAnotherDose() = runBlocking {
        meals = listOf(Meal(at("2026-10-13T08:10"), MealType.BREAKFAST))
        checker.check()
        meals = meals!! + Meal(at("2026-10-13T08:20"), MealType.BREAKFAST)
        now = at("2026-10-13T08:30")
        checker.check()
        assertEquals(1, logs().size)
    }

    @Test fun unreadableHealthConnectDoesNothing() = runBlocking {
        meals = null
        checker.check()
        assertEquals(0, logs().size)
        assertEquals(at("2026-10-13T10:00"), nextAlarm())
    }

    @Test fun mealAfterFallbackIsIgnored() = runBlocking {
        now = at("2026-10-13T10:00"); engine.sync() // fallback rings
        now = at("2026-10-13T10:15")
        meals = listOf(Meal(at("2026-10-13T10:05"), MealType.BREAKFAST))
        checker.check()
        assertEquals(listOf(DoseTrigger.FALLBACK), logs().map { it.trigger })
    }
}
