package com.medtrack.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class MealTriggerTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.parse("2026-10-13")
    private fun at(s: String): Instant = LocalDateTime.parse(s).atZone(zone).toInstant()
    private fun after(meal: MealType, delay: Int = 30, start: String = "2026-10-01", end: String? = null) =
        Schedule(ScheduleRule.AfterMeal(meal, delay, LocalTime.of(10, 0)), LocalDate.parse(start), end?.let(LocalDate::parse))

    @Test fun matchingMealPlusDelay() = assertEquals(
        at("2026-10-13T08:40"),
        mealTriggeredDose(after(MealType.BREAKFAST), listOf(Meal(at("2026-10-13T08:10"), MealType.BREAKFAST)), today, zone, emptySet()),
    )

    @Test fun otherMealTypeIgnored() = assertNull(
        mealTriggeredDose(after(MealType.BREAKFAST), listOf(Meal(at("2026-10-13T13:00"), MealType.LUNCH)), today, zone, emptySet()),
    )

    @Test fun anyMatchesSnacks() = assertEquals(
        at("2026-10-13T16:00"),
        mealTriggeredDose(after(MealType.ANY, delay = 0), listOf(Meal(at("2026-10-13T16:00"), null)), today, zone, emptySet()),
    )

    @Test fun earliestMatchingMealWins() = assertEquals(
        at("2026-10-13T08:30"),
        mealTriggeredDose(
            after(MealType.ANY),
            listOf(Meal(at("2026-10-13T13:00"), MealType.LUNCH), Meal(at("2026-10-13T08:00"), MealType.BREAKFAST)),
            today, zone, emptySet(),
        ),
    )

    @Test fun handledTodayReturnsNull() = assertNull(
        mealTriggeredDose(after(MealType.BREAKFAST), listOf(Meal(at("2026-10-13T08:10"), MealType.BREAKFAST)), today, zone, setOf(today)),
    )

    @Test fun yesterdaysMealIgnored() = assertNull(
        mealTriggeredDose(after(MealType.ANY), listOf(Meal(at("2026-10-12T20:00"), MealType.DINNER)), today, zone, emptySet()),
    )

    @Test fun outsideCourseReturnsNull() {
        val meals = listOf(Meal(at("2026-10-13T08:10"), MealType.BREAKFAST))
        assertNull(mealTriggeredDose(after(MealType.BREAKFAST, start = "2026-10-14"), meals, today, zone, emptySet()))
        assertNull(mealTriggeredDose(after(MealType.BREAKFAST, end = "2026-10-12"), meals, today, zone, emptySet()))
    }

    @Test fun nonMealScheduleReturnsNull() = assertNull(
        mealTriggeredDose(
            Schedule(ScheduleRule.DailyTimes(listOf(LocalTime.of(8, 0))), LocalDate.parse("2026-10-01")),
            listOf(Meal(at("2026-10-13T08:10"), MealType.BREAKFAST)), today, zone, emptySet(),
        ),
    )
}
