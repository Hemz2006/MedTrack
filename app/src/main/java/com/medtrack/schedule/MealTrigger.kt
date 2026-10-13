package com.medtrack.schedule

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A meal read from Health Connect. [type] is null for snacks and unknown meals. */
data class Meal(val time: Instant, val type: MealType?)

/**
 * When [rule]'s dose should ring today: the earliest matching meal today plus the delay.
 * Null when today already has a dose for the rule ([handledDates]), the course isn't running today,
 * or no meal matches.
 */
fun mealTriggeredDose(
    schedule: Schedule,
    rule: ScheduleRule.AfterMeal,
    meals: List<Meal>,
    today: LocalDate,
    zone: ZoneId,
    handledDates: Set<LocalDate>,
): Instant? {
    if (today in handledDates) return null
    if (today < schedule.startDate || (schedule.endDate != null && today > schedule.endDate)) return null
    val meal = meals
        .filter { it.time.atZone(zone).toLocalDate() == today }
        .filter { rule.meal == MealType.ANY || it.type == rule.meal }
        .minByOrNull { it.time } ?: return null
    return meal.time.plus(Duration.ofMinutes(rule.delayMinutes.toLong()))
}
