package com.medtrack.health

import com.medtrack.data.DoseLogDao
import com.medtrack.data.MedicationDao
import com.medtrack.reminders.ReminderEngine
import com.medtrack.schedule.Meal
import com.medtrack.schedule.ScheduleRule
import com.medtrack.schedule.mealTriggeredDose
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Turns today's logged meals into after-meal doses. [readMeals] returns null when meals can't be read. */
class MealChecker(
    private val readMeals: suspend (from: Instant, to: Instant) -> List<Meal>?,
    private val medications: MedicationDao,
    private val doseLogs: DoseLogDao,
    private val engine: ReminderEngine,
    private val clock: () -> Clock = { Clock.systemDefaultZone() },
) {
    suspend fun check() {
        val c = clock()
        val now = c.instant()
        val zone = c.zone
        val today = LocalDate.now(c)
        val mealMeds = medications.active().filter { it.schedule.rule is ScheduleRule.AfterMeal }
        if (mealMeds.isEmpty()) return
        val dayStart = today.atStartOfDay(zone).toInstant()
        val meals = readMeals(dayStart, now) ?: return
        val handledToday = doseLogs.since(dayStart)
            .filter { it.dueAt.atZone(zone).toLocalDate() == today }
            .map { it.medicationId }
            .toSet()
        for (med in mealMeds) {
            val handled = if (med.id in handledToday) setOf(today) else emptySet()
            val due = mealTriggeredDose(med.schedule, meals, today, zone, handled) ?: continue
            engine.addMealDose(med.id, maxOf(due, now))
        }
    }
}
