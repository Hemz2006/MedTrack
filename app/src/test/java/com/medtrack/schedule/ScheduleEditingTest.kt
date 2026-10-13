package com.medtrack.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleEditingTest {
    private val start = LocalDate.parse("2026-10-13")
    private val daily = Schedule(ScheduleRule.DailyTimes(listOf(LocalTime.of(9, 0), LocalTime.of(21, 0))), start)

    @Test fun switchingToWeekdaysKeepsTimes() {
        val rule = daily.withKind(ScheduleKind.WEEKDAYS).rule as ScheduleRule.Weekdays
        assertEquals(daily.rule.let { (it as ScheduleRule.DailyTimes).times }, rule.times)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), rule.days)
    }

    @Test fun switchingToIntervalAnchorsAtFirstTimeOnStartDate() {
        val rule = daily.withKind(ScheduleKind.INTERVAL).rule as ScheduleRule.EveryNHours
        assertEquals(8, rule.intervalHours)
        assertEquals(LocalDateTime.parse("2026-10-13T09:00"), rule.anchor)
    }

    @Test fun switchingToAfterMealUsesDefaults() =
        assertEquals(ScheduleRule.AfterMeal(MealType.BREAKFAST, 0, LocalTime.of(10, 0)), daily.withKind(ScheduleKind.AFTER_MEAL).rule)

    @Test fun sameKindIsNoOp() = assertSame(daily, daily.withKind(ScheduleKind.DAILY))

    @Test fun startDateMovesIntervalAnchor() {
        val interval = daily.withKind(ScheduleKind.INTERVAL).withStartDate(LocalDate.parse("2026-10-20"))
        assertEquals(LocalDate.parse("2026-10-20"), interval.startDate)
        assertEquals(LocalDateTime.parse("2026-10-20T09:00"), (interval.rule as ScheduleRule.EveryNHours).anchor)
    }
}
