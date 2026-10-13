package com.medtrack.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleTest {
    private val start = LocalDate.of(2026, 10, 13)
    private fun t(s: String) = LocalTime.parse(s)

    private val samples = listOf(
        Schedule(ScheduleRule.DailyTimes(listOf(t("08:00"), t("21:00"))), start),
        Schedule(ScheduleRule.Weekdays(setOf(MONDAY, WEDNESDAY, FRIDAY), listOf(t("08:00"))), start, start.plusDays(7)),
        Schedule(ScheduleRule.EveryNHours(6, LocalDateTime.of(start, t("06:00"))), start),
        Schedule(ScheduleRule.AfterMeal(MealType.BREAKFAST, 30, t("10:00")), start),
    )

    @Test fun jsonRoundTripsEveryRuleType() {
        samples.forEach { assertEquals(it, ScheduleJson.decode(ScheduleJson.encode(it))) }
    }

    @Test fun validSchedulesHaveNoError() {
        samples.forEach { assertNull(it.validationError()) }
    }

    @Test fun validationCatchesBadInput() {
        assertEquals("Add at least one time", Schedule(ScheduleRule.DailyTimes(emptyList()), start).validationError())
        assertEquals("Pick at least one day", Schedule(ScheduleRule.Weekdays(emptySet(), listOf(t("08:00"))), start).validationError())
        assertEquals("Interval must be 1–24 hours", Schedule(ScheduleRule.EveryNHours(0, LocalDateTime.of(start, t("06:00"))), start).validationError())
        assertEquals("Delay must be 0–240 minutes", Schedule(ScheduleRule.AfterMeal(MealType.ANY, 300, t("10:00")), start).validationError())
        assertEquals("End date is before start date", Schedule(ScheduleRule.DailyTimes(listOf(t("08:00"))), start, start.minusDays(1)).validationError())
    }

    @Test fun describesEachRuleType() {
        assertEquals("Daily at 08:00, 21:00", samples[0].describe())
        assertEquals("Mon, Wed, Fri at 08:00, until 20 Oct", samples[1].describe())
        assertEquals("Every 6 h from 06:00", samples[2].describe())
        assertEquals("30 min after breakfast (by 10:00)", samples[3].describe())
        assertEquals(
            "Right after any meal (by 13:00)",
            Schedule(ScheduleRule.AfterMeal(MealType.ANY, 0, t("13:00")), start).describe(),
        )
    }
}
