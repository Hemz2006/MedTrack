package com.medtrack.ui.today

import com.medtrack.data.DoseLogEntity
import com.medtrack.data.DoseStatus
import com.medtrack.data.MedicationEntity
import com.medtrack.schedule.ScheduleRule
import com.medtrack.schedule.dosesBetween
import com.medtrack.schedule.mealName
import com.medtrack.schedule.slot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TodayItem(
    val key: String,
    val logId: Long?,
    val medication: MedicationEntity,
    val dueAt: Instant,
    /** null = upcoming, not yet rung. */
    val status: DoseStatus?,
    val snoozedUntil: Instant?,
    val mealHint: String?,
    /** When it was taken or skipped, if recorded. */
    val actedAt: Instant? = null,
    /** The after-meal rule this dose belongs to, for recording it before it rings. */
    val slot: String? = null,
)

/** Today's recorded doses plus the doses still to come, in time order. */
fun buildToday(meds: List<MedicationEntity>, logs: List<DoseLogEntity>, now: Instant, zone: ZoneId): List<TodayItem> {
    val today = now.atZone(zone).toLocalDate()
    val dayStart = today.atStartOfDay(zone).toInstant()
    val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant()
    val byId = meds.associateBy { it.id }
    val todayLogs = logs.filter { it.dueAt >= dayStart && it.dueAt < dayEnd }

    val recorded = todayLogs.mapNotNull { log ->
        byId[log.medicationId]?.let { TodayItem("log-${log.id}", log.id, it, log.dueAt, log.status, log.snoozedUntil, null, log.actedAt) }
    }
    val loggedKeys = todayLogs.map { it.medicationId to it.dueAt }.toSet()
    val handledToday = logs.filter { it.forDate == today && it.slot != null }.map { it.medicationId to it.slot }.toSet()

    val upcoming = meds.filter { it.active }.flatMap { med ->
        med.steps.flatMap { it.rules.map { rule -> it to rule } }.flatMap { (step, rule) ->
            val slot = rule.slot
            val skip = if (slot != null && (med.id to slot) in handledToday) setOf(today) else emptySet()
            val mealHint = (rule as? ScheduleRule.AfterMeal)?.let { "After ${mealName(it.meal)}" }
            dosesBetween(step, rule, now, dayEnd.minusMillis(1), zone, skip)
                .filter { (med.id to it) !in loggedKeys }
                .map { due -> TodayItem("up-${med.id}-${due.toEpochMilli()}", null, med, due, null, null, mealHint, slot = slot) }
        }.distinctBy { it.key } // two rules landing on the same minute are one dose
    }
    return (recorded + upcoming).sortedBy { it.dueAt }
}

/** How a dose looks as a blister capsule (Today strip and detail history). */
enum class CapsuleState(val spoken: String) {
    TAKEN("taken"), DUE("due now"), MISSED("missed"), SKIPPED("skipped"), UPCOMING("later"),
}

/** A rung dose that is still waiting for the user. */
fun TodayItem.isDueNow(now: Instant): Boolean = status == DoseStatus.PENDING && dueAt <= now

fun TodayItem.capsuleState(now: Instant): CapsuleState = when (status) {
    DoseStatus.TAKEN -> CapsuleState.TAKEN
    DoseStatus.SKIPPED -> CapsuleState.SKIPPED
    DoseStatus.MISSED -> CapsuleState.MISSED
    DoseStatus.PENDING -> if (isDueNow(now)) CapsuleState.DUE else CapsuleState.UPCOMING
    null -> CapsuleState.UPCOMING
}

fun DoseStatus.capsuleState(): CapsuleState = when (this) {
    DoseStatus.TAKEN -> CapsuleState.TAKEN
    DoseStatus.SKIPPED -> CapsuleState.SKIPPED
    DoseStatus.MISSED -> CapsuleState.MISSED
    DoseStatus.PENDING -> CapsuleState.DUE
}

data class TodayGroup(val key: String, val label: String, val items: List<TodayItem>) {
    fun isDueNow(now: Instant): Boolean = items.any { it.isDueNow(now) }
}

private val HH_MM: DateTimeFormatter = com.medtrack.schedule.TIME_12

/** Groups time-ordered items by display time; upcoming after-meal doses group under their meal ("After dinner, by 22:00"). */
fun groupToday(items: List<TodayItem>, zone: ZoneId): List<TodayGroup> =
    items.groupBy { item ->
        val time = HH_MM.format(item.dueAt.atZone(zone))
        item.mealHint?.let { "$it, by $time" } ?: time
    }.map { (label, groupItems) -> TodayGroup("group-$label", label, groupItems) }
