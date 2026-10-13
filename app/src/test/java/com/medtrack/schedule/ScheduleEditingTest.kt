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
    private val dailyRule = ScheduleRule.DailyTimes(listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)))
    private fun breakfast() = ScheduleRule.AfterMeal(MealType.BREAKFAST, 0, LocalTime.of(10, 0))

    @Test fun switchingToWeekdaysKeepsTimes() {
        val rule = dailyRule.withKind(ScheduleKind.WEEKDAYS, start) as ScheduleRule.Weekdays
        assertEquals(dailyRule.times, rule.times)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), rule.days)
    }

    @Test fun switchingToIntervalAnchorsAtFirstTimeOnStartDate() {
        val rule = dailyRule.withKind(ScheduleKind.INTERVAL, start) as ScheduleRule.EveryNHours
        assertEquals(8, rule.intervalHours)
        assertEquals(LocalDateTime.parse("2026-10-13T09:00"), rule.anchor)
    }

    @Test fun switchingToAfterMealUsesDefaults() =
        assertEquals(breakfast(), dailyRule.withKind(ScheduleKind.AFTER_MEAL, start))

    @Test fun sameKindIsNoOp() = assertSame(dailyRule, dailyRule.withKind(ScheduleKind.DAILY, start))

    @Test fun startDateMovesIntervalAnchors() {
        val interval = Schedule(dailyRule.withKind(ScheduleKind.INTERVAL, start), start).withStartDate(LocalDate.parse("2026-10-20"))
        assertEquals(LocalDate.parse("2026-10-20"), interval.startDate)
        assertEquals(LocalDateTime.parse("2026-10-20T09:00"), (interval.rules.single() as ScheduleRule.EveryNHours).anchor)
    }

    @Test fun addingAfterAMealRuleSuggestsTheNextMeal() {
        val s = Schedule(breakfast(), start).withAddedRule()
        assertEquals(listOf(breakfast(), ScheduleRule.AfterMeal(MealType.LUNCH, 0, LocalTime.of(15, 0))), s.rules)
        assertEquals(MealType.DINNER, (s.withAddedRule().rules.last() as ScheduleRule.AfterMeal).meal)
    }

    @Test fun addingAfterClockTimesAddsAnEveningTime() =
        assertEquals(ScheduleRule.DailyTimes(listOf(LocalTime.of(20, 0))), Schedule(dailyRule, start).withAddedRule().rules.last())

    @Test fun editAndRemoveRuleByIndex() {
        val s = Schedule(listOf(breakfast(), dailyRule), start)
        assertEquals(listOf(breakfast(), breakfast()), s.withRule(1, breakfast()).rules)
        assertEquals(listOf(dailyRule), s.withoutRule(0).rules)
    }

    @Test fun changingMealMovesDefaultFallbackOnly() {
        assertEquals(LocalTime.of(22, 0), breakfast().withMeal(MealType.DINNER).fallbackTime)
        assertEquals(LocalTime.of(9, 30), breakfast().copy(fallbackTime = LocalTime.of(9, 30)).withMeal(MealType.DINNER).fallbackTime)
    }
}
