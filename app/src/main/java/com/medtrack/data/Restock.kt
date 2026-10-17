package com.medtrack.data

import java.time.LocalDate

/** How many days before a course's last day to ask about buying more. */
const val RESTOCK_DAYS_AHEAD = 3L

/** The medication's last day: the end of its last dose, or null when it goes on with no end. */
val MedicationEntity.courseEnd: LocalDate? get() = steps.lastOrNull()?.endDate

/** Identifies one ask: a medication and the end date it was asked about (a new end date asks again). */
fun restockKey(med: MedicationEntity, end: LocalDate): String = "${med.id}@$end"

/**
 * The first active medication whose course ends within [RESTOCK_DAYS_AHEAD] days of [today] (today included) and
 * hasn't been asked about for that end date ([asked] holds [restockKey]s), soonest end first.
 */
fun restockDue(meds: List<MedicationEntity>, today: LocalDate, asked: Set<String>): MedicationEntity? =
    meds.filter { med ->
        val end = med.courseEnd ?: return@filter false
        med.active && end >= today && end <= today.plusDays(RESTOCK_DAYS_AHEAD) && restockKey(med, end) !in asked
    }.minByOrNull { it.courseEnd!! }
