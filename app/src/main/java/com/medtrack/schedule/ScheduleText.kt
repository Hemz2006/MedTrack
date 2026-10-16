package com.medtrack.schedule

import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Clock times everywhere in the app: "8:00 AM". */
val TIME_12: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val HM = TIME_12
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

fun mealName(meal: MealType): String = when (meal) {
    MealType.BREAKFAST -> "breakfast"
    MealType.LUNCH -> "lunch"
    MealType.DINNER -> "dinner"
    MealType.ANY -> "any meal"
}

fun Schedule.describe(): String {
    val base = rules.joinToString("; ") { it.describe() }
    return if (endDate != null) "$base, until ${endDate.format(DAY_MONTH)}" else base
}

fun ScheduleRule.describe(): String =
    when (val r = this) {
        is ScheduleRule.DailyTimes -> "Daily at ${r.times.sorted().joinToString(", ") { it.format(HM) }}"
        is ScheduleRule.Weekdays -> {
            val days = DayOfWeek.values().filter { it in r.days }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
            "$days at ${r.times.sorted().joinToString(", ") { it.format(HM) }}"
        }
        is ScheduleRule.EveryNHours -> "Every ${r.intervalHours} h: ${r.dailyTimes().joinToString(", ") { it.format(HM) }}"
        is ScheduleRule.AfterMeal -> {
            val delay = if (r.delayMinutes == 0) "Right" else "${r.delayMinutes} min"
            "$delay after ${mealName(r.meal)} (by ${r.fallbackTime.format(HM)})"
        }
    }

/** An every-N-hours rule's times in each day's cycle, from the first dose (some may fall after midnight). */
fun ScheduleRule.EveryNHours.dailyTimes(): List<java.time.LocalTime> {
    if (intervalHours <= 0) return emptyList()
    val first = anchor.toLocalTime()
    return (0 until (24 + intervalHours - 1) / intervalHours).map { first.plusHours(it.toLong() * intervalHours) }
}
