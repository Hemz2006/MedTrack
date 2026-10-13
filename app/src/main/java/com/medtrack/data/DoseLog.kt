package com.medtrack.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

enum class DoseStatus { PENDING, TAKEN, SKIPPED, MISSED }

enum class DoseTrigger { SCHEDULE, MEAL, FALLBACK }

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["medicationId", "dueAt"], unique = true)],
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val dueAt: Instant,
    val status: DoseStatus,
    val trigger: DoseTrigger,
    /** The local day this dose belongs to (a late meal dose can be due after midnight). */
    val forDate: LocalDate,
    val actedAt: Instant? = null,
    val snoozedUntil: Instant? = null,
    /** [com.medtrack.schedule.slot] of the after-meal rule this dose belongs to; null for clock-time doses. */
    val slot: String? = null,
)
