package com.medtrack.ui.today

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
    private val daily = MedicationEntity(1, "metformin", schedule = Schedule(ScheduleRule.DailyTimes(listOf(LocalTime.of(8, 0), LocalTime.of(21, 0))), start), createdAt = Instant.EPOCH)
    private val meal = MedicationEntity(2, "pantoprazole", schedule = Schedule(ScheduleRule.AfterMeal(MealType.BREAKFAST, 30, LocalTime.of(10, 0)), start), createdAt = Instant.EPOCH)

    @Test fun mergesLogsAndUpcomingInTimeOrder() {
        val logs = listOf(DoseLogEntity(5, 1, at("2026-10-13T08:00"), DoseStatus.TAKEN, DoseTrigger.SCHEDULE))
        val items = buildToday(listOf(daily), logs, at("2026-10-13T12:00"), zone)
        assertEquals(listOf(at("2026-10-13T08:00"), at("2026-10-13T21:00")), items.map { it.dueAt })
        assertEquals(listOf(DoseStatus.TAKEN, null), items.map { it.status })
    }

    @Test fun ignoresOtherDaysAndPausedMeds() {
        val logs = listOf(DoseLogEntity(5, 1, at("2026-10-12T21:00"), DoseStatus.MISSED, DoseTrigger.SCHEDULE))
        assertEquals(emptyList<TodayItem>(), buildToday(listOf(daily.copy(active = false)), logs, at("2026-10-13T12:00"), zone))
    }

    @Test fun afterMealShowsFallbackWithHintUntilHandled() {
        val before = buildToday(listOf(meal), emptyList(), at("2026-10-13T07:00"), zone).single()
        assertEquals(at("2026-10-13T10:00"), before.dueAt)
        assertEquals("After breakfast", before.mealHint)
        val logs = listOf(DoseLogEntity(9, 2, at("2026-10-13T08:40"), DoseStatus.PENDING, DoseTrigger.MEAL))
        val after = buildToday(listOf(meal), logs, at("2026-10-13T08:00"), zone)
        assertEquals(listOf(at("2026-10-13T08:40")), after.map { it.dueAt })
    }
}
