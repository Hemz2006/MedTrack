@file:UseSerializers(
    LocalTimeSerializer::class,
    LocalDateSerializer::class,
    LocalDateTimeSerializer::class,
    DayOfWeekSerializer::class,
)

package com.medtrack.schedule

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Serializable
enum class MealType { BREAKFAST, LUNCH, DINNER, ANY }

@Serializable
sealed interface ScheduleRule {
    @Serializable @SerialName("daily")
    data class DailyTimes(val times: List<LocalTime>) : ScheduleRule

    @Serializable @SerialName("weekdays")
    data class Weekdays(val days: Set<DayOfWeek>, val times: List<LocalTime>) : ScheduleRule

    /** Doses every [intervalHours] of elapsed time from [anchor], continuing across days. */
    @Serializable @SerialName("interval")
    data class EveryNHours(val intervalHours: Int, val anchor: LocalDateTime) : ScheduleRule

    /** Fires [delayMinutes] after a matching meal; fires at [fallbackTime] if no meal was logged. */
    @Serializable @SerialName("meal")
    data class AfterMeal(val meal: MealType, val delayMinutes: Int, val fallbackTime: LocalTime) : ScheduleRule
}

@Serializable
data class Schedule(
    val rule: ScheduleRule,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
)

object ScheduleJson {
    private val json = Json { ignoreUnknownKeys = true }
    fun encode(schedule: Schedule): String = json.encodeToString(Schedule.serializer(), schedule)
    fun decode(text: String): Schedule = json.decodeFromString(Schedule.serializer(), text)
}

/** A user-facing message describing why the schedule can't be saved, or null if it's valid. */
fun Schedule.validationError(): String? {
    if (endDate != null && endDate < startDate) return "End date is before start date"
    return when (val r = rule) {
        is ScheduleRule.DailyTimes -> if (r.times.isEmpty()) "Add at least one time" else null
        is ScheduleRule.Weekdays -> when {
            r.days.isEmpty() -> "Pick at least one day"
            r.times.isEmpty() -> "Add at least one time"
            else -> null
        }
        is ScheduleRule.EveryNHours -> if (r.intervalHours !in 1..24) "Interval must be 1–24 hours" else null
        is ScheduleRule.AfterMeal -> if (r.delayMinutes !in 0..240) "Delay must be 0–240 minutes" else null
    }
}
