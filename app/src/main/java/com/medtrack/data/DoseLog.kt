package com.medtrack.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

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
    val actedAt: Instant? = null,
    val snoozedUntil: Instant? = null,
)
