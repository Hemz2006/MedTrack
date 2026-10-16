package com.medtrack.schedule

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Upper bound on days searched, so a schedule whose dates are all skipped can't loop forever. */
private const val MAX_DAYS = 400

/**
 * The first dose of [rule] strictly after [after], or null when it has no more doses within [schedule]'s dates.
 * Doses whose local date is in [skipDates] are skipped. For [ScheduleRule.AfterMeal]
 * this is the fallback time; meal-triggered doses come from [mealTriggeredDose].
 */
fun nextDose(
    schedule: Schedule,
    rule: ScheduleRule,
    after: Instant,
    zone: ZoneId,
    skipDates: Set<LocalDate> = emptySet(),
): Instant? = when (rule) {
    is ScheduleRule.DailyTimes -> nextOnDays(schedule, after, zone, skipDates, rule.times) { true }
    is ScheduleRule.Weekdays -> nextOnDays(schedule, after, zone, skipDates, rule.times) { it.dayOfWeek in rule.days }
    is ScheduleRule.AfterMeal -> nextOnDays(schedule, after, zone, skipDates, listOf(rule.fallbackTime)) { true }
    is ScheduleRule.EveryNHours -> nextInterval(schedule, rule, after, zone, skipDates)
}

/** All doses of [rule] in (from, to]. */
fun dosesBetween(
    schedule: Schedule,
    rule: ScheduleRule,
    from: Instant,
    to: Instant,
    zone: ZoneId,
    skipDates: Set<LocalDate> = emptySet(),
): List<Instant> = buildList {
    var cursor = from
    while (true) {
        val next = nextDose(schedule, rule, cursor, zone, skipDates) ?: break
        if (next > to) break
        add(next)
        cursor = next
    }
}

private fun nextOnDays(
    schedule: Schedule,
    after: Instant,
    zone: ZoneId,
    skipDates: Set<LocalDate>,
    times: List<LocalTime>,
    dayAllowed: (LocalDate) -> Boolean,
): Instant? {
    if (times.isEmpty()) return null
    val sorted = times.sorted()
    var date = maxOf(after.atZone(zone).toLocalDate(), schedule.startDate)
    repeat(MAX_DAYS) {
        if (schedule.endDate != null && date > schedule.endDate) return null
        if (dayAllowed(date) && date !in skipDates) {
            for (time in sorted) {
                // atZone moves times in a DST gap forward and picks the earlier offset in an overlap.
                val candidate = date.atTime(time).atZone(zone).toInstant()
                if (candidate > after) return candidate
            }
        }
        date = date.plusDays(1)
    }
    return null
}

/**
 * Every N hours within each day, restarting each day at the first dose's clock time: every 10 hours from 8 AM is
 * 8 AM and 6 PM, every day. A day's doses stop at midnight rather than running on into the next day.
 */
private fun nextInterval(
    schedule: Schedule,
    rule: ScheduleRule.EveryNHours,
    after: Instant,
    zone: ZoneId,
    skipDates: Set<LocalDate>,
): Instant? {
    if (rule.intervalHours <= 0) return null
    val times = rule.dailyTimes()
    var day = maxOf(rule.anchor.toLocalDate(), schedule.startDate, after.atZone(zone).toLocalDate())
    repeat(MAX_DAYS) {
        if (schedule.endDate != null && day > schedule.endDate) return null
        if (day !in skipDates) {
            for (time in times) {
                val candidate = day.atTime(time).atZone(zone).toInstant()
                if (candidate > after) return candidate
            }
        }
        day = day.plusDays(1)
    }
    return null
}
