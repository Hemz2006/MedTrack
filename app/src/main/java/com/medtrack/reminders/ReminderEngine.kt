package com.medtrack.reminders

import com.medtrack.data.AppPrefs
import com.medtrack.data.DoseLogDao
import com.medtrack.data.DoseLogEntity
import com.medtrack.data.DoseStatus
import com.medtrack.data.DoseTrigger
import com.medtrack.data.MedicationDao
import com.medtrack.data.displayName
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Owns dose logs, notifications and the wake-up alarm. Every entry point ends with a scan,
 * so the stored state, the visible notifications and the next alarm always agree.
 */
class ReminderEngine(
    private val medications: MedicationDao,
    private val doseLogs: DoseLogDao,
    private val alarms: AlarmScheduler,
    private val notifier: DoseNotifier,
    private val prefs: AppPrefs,
    private val clock: () -> Clock = { Clock.systemDefaultZone() },
) {
    private val mutex = Mutex()

    private fun now(): Instant = clock().instant().truncatedTo(ChronoUnit.MILLIS)

    suspend fun sync() = mutex.withLock { scan() }

    suspend fun markTaken(logId: Long) = setStatus(listOf(logId), DoseStatus.TAKEN)
    suspend fun markSkipped(logId: Long) = setStatus(listOf(logId), DoseStatus.SKIPPED)
    suspend fun markAllTaken(logIds: List<Long>) = setStatus(logIds, DoseStatus.TAKEN)

    suspend fun snooze(logId: Long) = mutex.withLock {
        doseLogs.snooze(logId, now().plus(SNOOZE_FOR))
        notifier.cancel(logId)
        scan()
    }

    /** Taken/skipped from the Today screen before the dose rang; stops it ringing later. */
    suspend fun recordUpcoming(medicationId: Long, dueAt: Instant, status: DoseStatus) = mutex.withLock {
        val at = now()
        val id = doseLogs.insertIgnore(
            DoseLogEntity(medicationId = medicationId, dueAt = dueAt, status = status, trigger = DoseTrigger.SCHEDULE, actedAt = at),
        )
        if (id < 0) {
            // The alarm scan got there first; the user's choice must still win over the pending dose.
            doseLogs.find(medicationId, dueAt)?.takeIf { it.status == DoseStatus.PENDING }?.let {
                doseLogs.setStatus(it.id, status, at)
                notifier.cancel(it.id)
            }
        }
        scan()
    }

    /** A meal was detected: add the dose (it also marks today as handled for the fallback). */
    suspend fun addMealDose(medicationId: Long, dueAt: Instant) = mutex.withLock {
        val id = doseLogs.insertIgnore(
            DoseLogEntity(medicationId = medicationId, dueAt = dueAt, status = DoseStatus.PENDING, trigger = DoseTrigger.MEAL),
        )
        scan(ringNow = if (id > 0 && dueAt <= now()) listOf(id) else emptyList())
    }

    private suspend fun setStatus(logIds: List<Long>, status: DoseStatus) = mutex.withLock {
        val at = now()
        logIds.forEach {
            doseLogs.setStatus(it, status, at)
            notifier.cancel(it)
        }
        scan()
    }

    private suspend fun scan(ringNow: List<Long> = emptyList()) {
        val c = clock()
        val now = now()
        val zone = c.zone
        val lastScan = prefs.lastScanAt ?: now
        val meds = medications.active()
        val recent = doseLogs.since(minOf(lastScan, now).minus(Duration.ofDays(8)))
        val handled = recent.groupBy { it.medicationId }
            .mapValues { (_, logs) -> logs.map { it.dueAt.atZone(zone).toLocalDate() }.toSet() }

        val plan = planScan(
            meds = meds.map { MedSchedule(it.id, it.schedule) },
            handledDates = handled,
            pending = doseLogs.pending().map { PendingDose(it.id, it.medicationId, it.dueAt, it.snoozedUntil) },
            lastScanAt = lastScan,
            now = now,
            zone = zone,
        )

        // Whatever fails below, the next wake-up must still be booked or later doses would be lost.
        try {
            val inserted = plan.newDoses.map {
                doseLogs.insertIgnore(DoseLogEntity(medicationId = it.medicationId, dueAt = it.dueAt, status = DoseStatus.PENDING, trigger = it.trigger))
            }.filter { it > 0 }
            plan.missedDoses.forEach {
                doseLogs.insertIgnore(DoseLogEntity(medicationId = it.medicationId, dueAt = it.dueAt, status = DoseStatus.MISSED, trigger = it.trigger))
            }
            if (plan.missedLogIds.isNotEmpty()) doseLogs.markMissed(plan.missedLogIds)
            prefs.lastScanAt = now

            notifier.cancelAllExcept(doseLogs.pending().map { it.id }.toSet())
            val ring = doseLogs.byIds((inserted + plan.ringLogIds + ringNow).distinct()).filter { it.status == DoseStatus.PENDING }
            if (ring.isNotEmpty()) {
                val medsById = medications.byIds(ring.map { it.medicationId }.distinct()).associateBy { it.id }
                notifier.ring(
                    ring.sortedBy { it.dueAt }.mapNotNull { log ->
                        medsById[log.medicationId]?.let { med ->
                            RingItem(log.id, med.displayName(), listOfNotNull(med.doseText, med.notes).filter { it.isNotBlank() }.joinToString(" · "), med.photoPath)
                        }
                    },
                )
            }
        } finally {
            alarms.set(plan.nextWake)
        }
    }
}
