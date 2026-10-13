package com.medtrack.schedule

import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val HM = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

fun mealName(meal: MealType): String = when (meal) {
    MealType.BREAKFAST -> "breakfast"
    MealType.LUNCH -> "lunch"
    MealType.DINNER -> "dinner"
    MealType.ANY -> "any meal"
}

fun Schedule.describe(): String {
    val base = when (val r = rule) {
        is ScheduleRule.DailyTimes -> "Daily at ${r.times.sorted().joinToString(", ") { it.format(HM) }}"
        is ScheduleRule.Weekdays -> {
            val days = DayOfWeek.values().filter { it in r.days }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
            "$days at ${r.times.sorted().joinToString(", ") { it.format(HM) }}"
        }
        is ScheduleRule.EveryNHours -> "Every ${r.intervalHours} h from ${r.anchor.toLocalTime().format(HM)}"
        is ScheduleRule.AfterMeal -> {
            val delay = if (r.delayMinutes == 0) "Right" else "${r.delayMinutes} min"
            "$delay after ${mealName(r.meal)} (by ${r.fallbackTime.format(HM)})"
        }
    }
    return if (endDate != null) "$base, until ${endDate.format(DAY_MONTH)}" else base
}
