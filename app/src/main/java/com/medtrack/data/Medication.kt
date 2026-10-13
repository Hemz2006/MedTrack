package com.medtrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medtrack.schedule.Schedule
import com.medtrack.schedule.ScheduleRule
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class Ingredient(val name: String, val strength: String? = null, val formula: String? = null)

/** Outcome of one lookup source (RxNorm, PubChem or MedlinePlus) for a medication. */
enum class SourceStatus { NOT_RUN, OK, NOT_FOUND, FAILED }

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val genericName: String,
    val brandName: String? = null,
    val doseText: String? = null,
    val notes: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val usedFor: String? = null,
    val rxcui: String? = null,
    val rxnormStatus: SourceStatus = SourceStatus.NOT_RUN,
    val pubchemStatus: SourceStatus = SourceStatus.NOT_RUN,
    val medlineStatus: SourceStatus = SourceStatus.NOT_RUN,
    /** Photo file paths; the first is the one shown in lists, notifications and alarms. */
    val photos: List<String> = emptyList(),
    val active: Boolean = true,
    val schedule: Schedule,
    val createdAt: Instant,
)

val MedicationEntity.coverPhoto: String? get() = photos.firstOrNull()

/** The name on the pack: the brand when there is one, otherwise the generic name. */
val MedicationEntity.title: String
    get() = brandName?.takeIf { it.isNotBlank() } ?: genericName

/** The generic name when [title] is the brand; null when there is nothing more to say. */
val MedicationEntity.subtitle: String?
    get() = genericName.takeIf { it.isNotBlank() && title != genericName }

fun MedicationEntity.displayName(): String = subtitle?.let { "$title ($it)" } ?: title

fun newMedication(today: LocalDate, now: Instant) = MedicationEntity(
    genericName = "",
    schedule = Schedule(ScheduleRule.DailyTimes(listOf(LocalTime.of(8, 0))), today),
    createdAt = now,
)
