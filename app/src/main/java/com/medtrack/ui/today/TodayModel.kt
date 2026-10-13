package com.medtrack.ui.today

import com.medtrack.data.DoseLogEntity
import com.medtrack.data.DoseStatus
import com.medtrack.data.MedicationEntity
import com.medtrack.schedule.ScheduleRule
import com.medtrack.schedule.dosesBetween
import com.medtrack.schedule.mealName
import java.time.Instant
import java.time.ZoneId

data class TodayItem(
    val key: String,
    val logId: Long?,
    val medication: MedicationEntity,
    val dueAt: Instant,
    /** null = upcoming, not yet rung. */
    val status: DoseStatus?,
    val snoozedUntil: Instant?,
    val mealHint: String?,
)

/** Today's recorded doses plus the doses still to come, in time order. */
fun buildToday(meds: List<MedicationEntity>, logs: List<DoseLogEntity>, now: Instant, zone: ZoneId): List<TodayItem> {
    val today = now.atZone(zone).toLocalDate()
    val dayStart = today.atStartOfDay(zone).toInstant()
    val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant()
    val byId = meds.associateBy { it.id }
    val todayLogs = logs.filter { it.dueAt >= dayStart && it.dueAt < dayEnd }

    val recorded = todayLogs.mapNotNull { log ->
        byId[log.medicationId]?.let { TodayItem("log-${log.id}", log.id, it, log.dueAt, log.status, log.snoozedUntil, null) }
    }
    val loggedKeys = todayLogs.map { it.medicationId to it.dueAt }.toSet()
    val handledToday = logs.filter { it.forDate == today }.map { it.medicationId }.toSet()

    val upcoming = meds.filter { it.active }.flatMap { med ->
        val rule = med.schedule.rule as? ScheduleRule.AfterMeal
        val skip = if (rule != null && med.id in handledToday) setOf(today) else emptySet()
        dosesBetween(med.schedule, now, dayEnd.minusMillis(1), zone, skip)
            .filter { (med.id to it) !in loggedKeys }
            .map { due ->
                TodayItem("up-${med.id}-${due.toEpochMilli()}", null, med, due, null, null, rule?.let { "After ${mealName(it.meal)}" })
            }
    }
    return (recorded + upcoming).sortedBy { it.dueAt }
}
