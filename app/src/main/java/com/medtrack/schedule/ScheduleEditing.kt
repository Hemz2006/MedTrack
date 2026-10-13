package com.medtrack.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class ScheduleKind(val label: String) {
    DAILY("Daily"), WEEKDAYS("Weekdays"), INTERVAL("Every N hours"), AFTER_MEAL("After meal")
}

val ScheduleRule.kind: ScheduleKind
    get() = when (this) {
        is ScheduleRule.DailyTimes -> ScheduleKind.DAILY
        is ScheduleRule.Weekdays -> ScheduleKind.WEEKDAYS
        is ScheduleRule.EveryNHours -> ScheduleKind.INTERVAL
        is ScheduleRule.AfterMeal -> ScheduleKind.AFTER_MEAL
    }

/** A sensible "remind anyway" time for each meal. */
fun defaultFallback(meal: MealType): LocalTime = when (meal) {
    MealType.BREAKFAST -> LocalTime.of(10, 0)
    MealType.LUNCH -> LocalTime.of(15, 0)
    MealType.DINNER -> LocalTime.of(22, 0)
    MealType.ANY -> LocalTime.of(13, 0)
}

/** Changes the meal; a fallback time still at the old meal's default moves to the new meal's default. */
fun ScheduleRule.AfterMeal.withMeal(meal: MealType): ScheduleRule.AfterMeal =
    copy(meal = meal, fallbackTime = if (fallbackTime == defaultFallback(this.meal)) defaultFallback(meal) else fallbackTime)

/** Switches the rule's type, carrying over the dose times where the new type has them. */
fun ScheduleRule.withKind(kind: ScheduleKind, startDate: LocalDate): ScheduleRule {
    if (this.kind == kind) return this
    val times = when (this) {
        is ScheduleRule.DailyTimes -> times
        is ScheduleRule.Weekdays -> times
        else -> emptyList()
    }.ifEmpty { listOf(LocalTime.of(8, 0)) }
    return when (kind) {
        ScheduleKind.DAILY -> ScheduleRule.DailyTimes(times)
        ScheduleKind.WEEKDAYS -> ScheduleRule.Weekdays(
            setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), times,
        )
        ScheduleKind.INTERVAL -> ScheduleRule.EveryNHours(8, startDate.atTime(times.min()))
        ScheduleKind.AFTER_MEAL -> ScheduleRule.AfterMeal(MealType.BREAKFAST, 0, defaultFallback(MealType.BREAKFAST))
    }
}

/** Replaces the rule at [index]. */
fun Schedule.withRule(index: Int, rule: ScheduleRule): Schedule =
    copy(rules = rules.toMutableList().also { it[index] = rule })

fun Schedule.withoutRule(index: Int): Schedule =
    copy(rules = rules.filterIndexed { i, _ -> i != index })

/**
 * Adds another rule. After an after-meal rule it suggests the next meal not yet covered
 * (breakfast, then lunch, then dinner), since "after breakfast and after dinner" is the common case.
 */
fun Schedule.withAddedRule(): Schedule {
    val usedMeals = rules.filterIsInstance<ScheduleRule.AfterMeal>().map { it.meal }.toSet()
    val nextMeal = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER).firstOrNull { it !in usedMeals }
    val rule = if (rules.lastOrNull() is ScheduleRule.AfterMeal && MealType.ANY !in usedMeals && nextMeal != null) {
        ScheduleRule.AfterMeal(nextMeal, 0, defaultFallback(nextMeal))
    } else {
        ScheduleRule.DailyTimes(listOf(LocalTime.of(20, 0)))
    }
    return copy(rules = rules + rule)
}

/** Changes the start date; interval rules' first doses move with it. */
fun Schedule.withStartDate(date: LocalDate): Schedule = copy(
    startDate = date,
    rules = rules.map { rule ->
        (rule as? ScheduleRule.EveryNHours)?.let { it.copy(anchor = date.atTime(it.anchor.toLocalTime())) } ?: rule
    },
)
