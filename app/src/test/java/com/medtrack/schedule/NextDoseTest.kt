package com.medtrack.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class NextDoseTest {
    private val ist = ZoneId.of("Asia/Kolkata")
    private val london = ZoneId.of("Europe/London")
    private fun at(s: String, zone: ZoneId = ist): Instant = LocalDateTime.parse(s).atZone(zone).toInstant()
    private fun daily(vararg times: String, start: String = "2026-10-01", end: String? = null) =
        Schedule(ScheduleRule.DailyTimes(times.map(LocalTime::parse)), LocalDate.parse(start), end?.let(LocalDate::parse))

    @Test fun dailyLaterTimeSameDay() =
        assertEquals(at("2026-10-13T21:00"), nextDose(daily("08:00", "21:00"), at("2026-10-13T09:00"), ist))

    @Test fun dailyUnsortedTimes() =
        assertEquals(at("2026-10-13T08:00"), nextDose(daily("21:00", "08:00"), at("2026-10-13T07:00"), ist))

    @Test fun dailyCrossesMidnight() =
        assertEquals(at("2026-10-14T08:00"), nextDose(daily("08:00", "21:00"), at("2026-10-13T22:00"), ist))

    @Test fun strictlyAfter() =
        assertEquals(at("2026-10-13T21:00"), nextDose(daily("08:00", "21:00"), at("2026-10-13T08:00"), ist))

    @Test fun waitsForStartDate() =
        assertEquals(at("2026-10-20T08:00"), nextDose(daily("08:00", start = "2026-10-20"), at("2026-10-13T09:00"), ist))

    @Test fun courseLastDay() {
        val s = daily("08:00", end = "2026-10-14")
        assertEquals(at("2026-10-14T08:00"), nextDose(s, at("2026-10-14T07:00"), ist))
        assertNull(nextDose(s, at("2026-10-14T09:00"), ist))
    }

    @Test fun weekdaysWrapToNextWeek() {
        val s = Schedule(ScheduleRule.Weekdays(setOf(MONDAY, WEDNESDAY, FRIDAY), listOf(LocalTime.of(8, 0))), LocalDate.parse("2026-10-01"))
        // 2026-10-13 is a Tuesday, 2026-10-16 a Friday
        assertEquals(at("2026-10-14T08:00"), nextDose(s, at("2026-10-13T09:00"), ist))
        assertEquals(at("2026-10-19T08:00"), nextDose(s, at("2026-10-16T09:00"), ist))
    }

    @Test fun springForwardGapShiftsLater() {
        // 2026-03-29 01:00 -> 02:00 in London; 01:30 doesn't exist that day
        val s = daily("01:30", start = "2026-03-01")
        assertEquals(Instant.parse("2026-03-29T01:30:00Z"), nextDose(s, at("2026-03-28T12:00", london), london))
    }

    @Test fun wallClockTimeKeptAcrossDst() {
        val s = daily("08:00", start = "2026-03-01")
        assertEquals(at("2026-03-29T08:00", london), nextDose(s, at("2026-03-28T09:00", london), london))
    }

    @Test fun autumnOverlapFiresOnce() {
        // 2026-10-25 02:00 BST -> 01:00 GMT; 01:30 happens twice; we use the first
        val s = daily("01:30", start = "2026-10-01")
        val first = nextDose(s, at("2026-10-24T12:00", london), london)
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), first)
        assertEquals(Instant.parse("2026-10-26T01:30:00Z"), nextDose(s, first!!, london))
    }

    @Test fun everyNHours() {
        val s = Schedule(ScheduleRule.EveryNHours(6, LocalDateTime.parse("2026-10-13T06:00")), LocalDate.parse("2026-10-13"))
        assertEquals(at("2026-10-13T06:00"), nextDose(s, at("2026-10-12T20:00"), ist))
        assertEquals(at("2026-10-13T12:00"), nextDose(s, at("2026-10-13T06:00"), ist))
        assertEquals(at("2026-10-13T12:00"), nextDose(s, at("2026-10-13T07:00"), ist))
        assertEquals(at("2026-10-14T00:00"), nextDose(s, at("2026-10-13T23:00"), ist))
    }

    @Test fun everyNHoursIsElapsedTimeAcrossDst() {
        val s = Schedule(ScheduleRule.EveryNHours(12, LocalDateTime.parse("2026-10-24T20:00")), LocalDate.parse("2026-10-24"))
        assertEquals(Instant.parse("2026-10-25T07:00:00Z"), nextDose(s, Instant.parse("2026-10-25T06:00:00Z"), london))
    }

    @Test fun everyNHoursStopsAtEndDate() {
        val s = Schedule(ScheduleRule.EveryNHours(6, LocalDateTime.parse("2026-10-13T06:00")), LocalDate.parse("2026-10-13"), LocalDate.parse("2026-10-13"))
        assertNull(nextDose(s, at("2026-10-13T19:00"), ist))
    }

    @Test fun afterMealReturnsFallbackAndHonoursSkipDates() {
        val s = Schedule(ScheduleRule.AfterMeal(MealType.BREAKFAST, 30, LocalTime.of(10, 0)), LocalDate.parse("2026-10-01"))
        assertEquals(at("2026-10-13T10:00"), nextDose(s, at("2026-10-13T09:00"), ist))
        assertEquals(at("2026-10-14T10:00"), nextDose(s, at("2026-10-13T09:00"), ist, setOf(LocalDate.parse("2026-10-13"))))
    }

    @Test fun severalRulesMergeInTimeOrder() {
        val s = Schedule(
            listOf(ScheduleRule.AfterMeal(MealType.DINNER, 0, LocalTime.of(22, 0)), ScheduleRule.DailyTimes(listOf(LocalTime.of(8, 0), LocalTime.of(22, 0)))),
            LocalDate.parse("2026-10-01"),
        )
        assertEquals(at("2026-10-13T22:00"), nextDose(s, at("2026-10-13T09:00"), ist))
        assertEquals(listOf(at("2026-10-13T08:00"), at("2026-10-13T22:00")), dosesBetween(s, at("2026-10-13T00:00"), at("2026-10-13T23:00"), ist))
    }

    @Test fun dosesBetweenIsExclusiveStartInclusiveEnd() {
        assertEquals(
            listOf(at("2026-10-13T08:00"), at("2026-10-13T21:00"), at("2026-10-14T08:00")),
            dosesBetween(daily("08:00", "21:00"), at("2026-10-13T00:00"), at("2026-10-14T08:00"), ist),
        )
        assertEquals(emptyList<Instant>(), dosesBetween(daily("08:00"), at("2026-10-13T08:00"), at("2026-10-13T09:00"), ist))
    }
}
