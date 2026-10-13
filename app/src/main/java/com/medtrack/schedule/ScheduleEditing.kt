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

/** Switches the schedule type, carrying over the dose times where the new type has them. */
fun Schedule.withKind(kind: ScheduleKind): Schedule {
    if (rule.kind == kind) return this
    val times = when (val r = rule) {
        is ScheduleRule.DailyTimes -> r.times
        is ScheduleRule.Weekdays -> r.times
        else -> emptyList()
    }.ifEmpty { listOf(LocalTime.of(8, 0)) }
    val newRule = when (kind) {
        ScheduleKind.DAILY -> ScheduleRule.DailyTimes(times)
        ScheduleKind.WEEKDAYS -> ScheduleRule.Weekdays(
            setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), times,
        )
        ScheduleKind.INTERVAL -> ScheduleRule.EveryNHours(8, startDate.atTime(times.min()))
        ScheduleKind.AFTER_MEAL -> ScheduleRule.AfterMeal(MealType.BREAKFAST, 0, LocalTime.of(10, 0))
    }
    return copy(rule = newRule)
}

/** Changes the start date; an interval schedule's first dose moves with it. */
fun Schedule.withStartDate(date: LocalDate): Schedule = copy(
    startDate = date,
    rule = (rule as? ScheduleRule.EveryNHours)?.let { it.copy(anchor = date.atTime(it.anchor.toLocalTime())) } ?: rule,
)
