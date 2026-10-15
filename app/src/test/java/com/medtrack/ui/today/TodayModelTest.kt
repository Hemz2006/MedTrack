package com.medtrack.ui.today

import com.medtrack.data.doseOn
import com.medtrack.data.DoseLogEntity
import com.medtrack.data.DoseStatus
import com.medtrack.data.DoseTrigger
import com.medtrack.data.MedicationEntity
import com.medtrack.schedule.MealType
import com.medtrack.schedule.Schedule
import com.medtrack.schedule.ScheduleRule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class TodayModelTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(s: String): Instant = LocalDateTime.parse(s).atZone(zone).toInstant()
    private val start = LocalDate.parse("2026-10-01")
    private val daily = MedicationEntity(1, "metformin", steps = listOf(Schedule(ScheduleRule.DailyTimes(listOf(LocalTime.of(8, 0), LocalTime.of(21, 0))), start)), createdAt = Instant.EPOCH)
    private val meal = MedicationEntity(2, "pantoprazole", steps = listOf(Schedule(ScheduleRule.AfterMeal(MealType.BREAKFAST, 30, LocalTime.of(10, 0)), start)), createdAt = Instant.EPOCH)

    @Test fun mergesLogsAndUpcomingInTimeOrder() {
        val logs = listOf(DoseLogEntity(5, 1, at("2026-10-13T08:00"), DoseStatus.TAKEN, DoseTrigger.SCHEDULE, LocalDate.parse("2026-10-13")))
        val items = buildToday(listOf(daily), logs, at("2026-10-13T12:00"), zone)
        assertEquals(listOf(at("2026-10-13T08:00"), at("2026-10-13T21:00")), items.map { it.dueAt })
        assertEquals(listOf(DoseStatus.TAKEN, null), items.map { it.status })
    }

    @Test fun ignoresOtherDaysAndPausedMeds() {
        val logs = listOf(DoseLogEntity(5, 1, at("2026-10-12T21:00"), DoseStatus.MISSED, DoseTrigger.SCHEDULE, LocalDate.parse("2026-10-12")))
        assertEquals(emptyList<TodayItem>(), buildToday(listOf(daily.copy(active = false)), logs, at("2026-10-13T12:00"), zone))
    }

    @Test fun afterMealShowsFallbackWithHintUntilHandled() {
        val before = buildToday(listOf(meal), emptyList(), at("2026-10-13T07:00"), zone).single()
        assertEquals(at("2026-10-13T10:00"), before.dueAt)
        assertEquals("After breakfast", before.mealHint)
        val logs = listOf(DoseLogEntity(9, 2, at("2026-10-13T08:40"), DoseStatus.PENDING, DoseTrigger.MEAL, LocalDate.parse("2026-10-13"), slot = "meal:BREAKFAST"))
        val after = buildToday(listOf(meal), logs, at("2026-10-13T08:00"), zone)
        assertEquals(listOf(at("2026-10-13T08:40")), after.map { it.dueAt })
    }

    @Test fun breakfastAndDinnerAreSeparateDoses() {
        val rules = listOf(
            ScheduleRule.AfterMeal(MealType.BREAKFAST, 0, LocalTime.of(10, 0)),
            ScheduleRule.AfterMeal(MealType.DINNER, 0, LocalTime.of(22, 0)),
        )
        val twice = meal.copy(steps = listOf(Schedule(rules, start)))
        val before = buildToday(listOf(twice), emptyList(), at("2026-10-13T07:00"), zone)
        assertEquals(listOf("After breakfast", "After dinner"), before.map { it.mealHint })
        assertEquals(listOf("meal:BREAKFAST", "meal:DINNER"), before.map { it.slot })
        // Breakfast taken: dinner is still to come.
        val logs = listOf(DoseLogEntity(9, 2, at("2026-10-13T08:40"), DoseStatus.TAKEN, DoseTrigger.MEAL, LocalDate.parse("2026-10-13"), slot = "meal:BREAKFAST"))
        val after = buildToday(listOf(twice), logs, at("2026-10-13T09:00"), zone)
        assertEquals(listOf(at("2026-10-13T08:40"), at("2026-10-13T22:00")), after.map { it.dueAt })
    }

    @Test fun lateMealDoseDueAfterMidnightDoesNotHandleNextDay() {
        val dinner = MedicationEntity(3, "x", steps = listOf(Schedule(ScheduleRule.AfterMeal(MealType.DINNER, 120, LocalTime.of(23, 0)), start)), createdAt = Instant.EPOCH)
        val logs = listOf(DoseLogEntity(9, 3, at("2026-10-14T00:30"), DoseStatus.PENDING, DoseTrigger.MEAL, LocalDate.parse("2026-10-13"), slot = "meal:DINNER"))
        val items = buildToday(listOf(dinner), logs, at("2026-10-14T07:00"), zone)
        assertEquals(listOf(at("2026-10-14T00:30"), at("2026-10-14T23:00")), items.map { it.dueAt })
        assertEquals("After dinner", items.last().mealHint)
    }
    @Test fun groupsByTimeAndMealHint() {
        val logs = listOf(
            DoseLogEntity(5, 1, at("2026-10-13T08:00"), DoseStatus.TAKEN, DoseTrigger.SCHEDULE, LocalDate.parse("2026-10-13")),
        )
        val other = daily.copy(id = 4, genericName = "pan", steps = listOf(Schedule(ScheduleRule.DailyTimes(listOf(LocalTime.of(8, 0))), start)))
        val logs2 = logs + DoseLogEntity(6, 4, at("2026-10-13T08:00"), DoseStatus.PENDING, DoseTrigger.SCHEDULE, LocalDate.parse("2026-10-13"))
        val items = buildToday(listOf(daily, other, meal), logs2, at("2026-10-13T08:05"), zone)
        val groups = groupToday(items, zone)
        assertEquals(listOf("8:00 AM", "After breakfast, by 10:00 AM", "9:00 PM"), groups.map { it.label })
        assertEquals(2, groups.first().items.size)
        assertEquals(listOf(true, false, false), groups.map { it.isDueNow(at("2026-10-13T08:05")) })
    }

    @Test fun capsuleStatesFollowStatusAndTime() {
        val now = at("2026-10-13T12:00")
        fun item(status: DoseStatus?, due: String) = TodayItem("k", 1, daily, at(due), status, null, null)
        assertEquals(CapsuleState.TAKEN, item(DoseStatus.TAKEN, "2026-10-13T08:00").capsuleState(now))
        assertEquals(CapsuleState.DUE, item(DoseStatus.PENDING, "2026-10-13T11:00").capsuleState(now))
        assertEquals(CapsuleState.UPCOMING, item(DoseStatus.PENDING, "2026-10-13T13:00").capsuleState(now))
        assertEquals(CapsuleState.MISSED, item(DoseStatus.MISSED, "2026-10-13T08:00").capsuleState(now))
        assertEquals(CapsuleState.SKIPPED, item(DoseStatus.SKIPPED, "2026-10-13T08:00").capsuleState(now))
        assertEquals(CapsuleState.UPCOMING, item(null, "2026-10-13T21:00").capsuleState(now))
    }

    @Test fun recordedItemsCarryActedAt() {
        val logs = listOf(DoseLogEntity(5, 1, at("2026-10-13T08:00"), DoseStatus.TAKEN, DoseTrigger.SCHEDULE, LocalDate.parse("2026-10-13"), actedAt = at("2026-10-13T08:03")))
        assertEquals(at("2026-10-13T08:03"), buildToday(listOf(daily), logs, at("2026-10-13T12:00"), zone).first().actedAt)
    }

    @Test fun stepsSwitchTimesAndDoseOnTheirStartDay() {
        val steps = listOf(
            Schedule(ScheduleRule.DailyTimes(listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))), start, LocalDate.parse("2026-10-13")).copy(dose = "2 puffs"),
            Schedule(ScheduleRule.DailyTimes(listOf(LocalTime.of(8, 0))), LocalDate.parse("2026-10-14")).copy(dose = "1 puff"),
        )
        val taper = daily.copy(steps = steps, doseText = "usual")
        assertEquals(3, buildToday(listOf(taper), emptyList(), at("2026-10-13T00:01"), zone).size)
        assertEquals(listOf(at("2026-10-14T08:00")), buildToday(listOf(taper), emptyList(), at("2026-10-14T00:01"), zone).map { it.dueAt })
        assertEquals("2 puffs", taper.doseOn(LocalDate.parse("2026-10-13")))
        assertEquals("1 puff", taper.doseOn(LocalDate.parse("2026-10-14")))
        assertEquals("usual", taper.doseOn(LocalDate.parse("2026-09-01"))) // before the course: the usual dose
    }
}
