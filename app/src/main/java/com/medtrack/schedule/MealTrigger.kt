package com.medtrack.schedule

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A meal read from Health Connect. [type] is null for snacks and unknown meals. */
data class Meal(val time: Instant, val type: MealType?)

/**
 * When an after-meal dose should ring today: the earliest matching meal today plus the delay.
 * Null when today already has a dose ([handledDates]), the course isn't running today,
 * no meal matches, or the schedule isn't after-meal.
 */
fun mealTriggeredDose(
    schedule: Schedule,
    meals: List<Meal>,
    today: LocalDate,
    zone: ZoneId,
    handledDates: Set<LocalDate>,
): Instant? {
    val rule = schedule.rule as? ScheduleRule.AfterMeal ?: return null
    if (today in handledDates) return null
    if (today < schedule.startDate || (schedule.endDate != null && today > schedule.endDate)) return null
    val meal = meals
        .filter { it.time.atZone(zone).toLocalDate() == today }
        .filter { rule.meal == MealType.ANY || it.type == rule.meal }
        .minByOrNull { it.time } ?: return null
    return meal.time.plus(Duration.ofMinutes(rule.delayMinutes.toLong()))
}
