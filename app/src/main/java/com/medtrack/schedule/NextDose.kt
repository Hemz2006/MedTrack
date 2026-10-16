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
 * Every N hours, restarting each day at the first dose's clock time: every 10 hours from 8 AM is 8 AM, 6 PM and
 * 4 AM, every day. (Counting straight on from the first dose made intervals that don't divide 24 hours drift to
 * different times each day.) Intervals that do divide 24 (6, 8, 12) give the same times either way.
 */
private fun nextInterval(
    schedule: Schedule,
    rule: ScheduleRule.EveryNHours,
    after: Instant,
    zone: ZoneId,
    skipDates: Set<LocalDate>,
): Instant? {
    if (rule.intervalHours <= 0) return null
    val firstTime = rule.anchor.toLocalTime()
    val perDay = (24 + rule.intervalHours - 1) / rule.intervalHours // doses starting less than 24h after the first
    // A day's last doses can fall after midnight, so start from the day before [after].
    var day = maxOf(rule.anchor.toLocalDate(), after.atZone(zone).toLocalDate().minusDays(1))
    repeat(MAX_DAYS) {
        for (k in 0 until perDay) {
            val local = day.atTime(firstTime).plusHours(k.toLong() * rule.intervalHours)
            val candidate = local.atZone(zone).toInstant()
            if (candidate <= after) continue
            val date = local.toLocalDate()
            if (schedule.endDate != null && date > schedule.endDate) return null
            if (date >= schedule.startDate && date !in skipDates) return candidate
        }
        day = day.plusDays(1)
    }
    return null
}
