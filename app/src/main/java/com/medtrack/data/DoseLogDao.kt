package com.medtrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface DoseLogDao {
    /** Returns the new row id, or -1 when a log for (medicationId, dueAt) already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(log: DoseLogEntity): Long

    @Query("SELECT * FROM dose_logs WHERE id = :id")
    suspend fun get(id: Long): DoseLogEntity?

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId AND dueAt = :dueAt")
    suspend fun find(medicationId: Long, dueAt: Instant): DoseLogEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM dose_logs WHERE medicationId = :medicationId AND slot = :slot AND forDate = :forDate)")
    suspend fun existsForSlot(medicationId: Long, slot: String, forDate: LocalDate): Boolean

    @Query("SELECT * FROM dose_logs WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<DoseLogEntity>

    @Query("SELECT * FROM dose_logs WHERE status = 'PENDING'")
    suspend fun pending(): List<DoseLogEntity>

    @Query("SELECT * FROM dose_logs WHERE status = 'PENDING' ORDER BY dueAt")
    fun observePending(): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE dueAt >= :from")
    suspend fun since(from: Instant): List<DoseLogEntity>

    @Query("SELECT * FROM dose_logs WHERE dueAt >= :from ORDER BY dueAt")
    fun observeSince(from: Instant): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE dueAt >= :from AND dueAt < :to ORDER BY dueAt")
    fun observeBetween(from: Instant, to: Instant): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId ORDER BY dueAt DESC LIMIT 200")
    fun observeForMedication(medicationId: Long): Flow<List<DoseLogEntity>>

    @Query("UPDATE dose_logs SET status = :status, actedAt = :actedAt, snoozedUntil = NULL WHERE id = :id")
    suspend fun setStatus(id: Long, status: DoseStatus, actedAt: Instant?)

    @Query("DELETE FROM dose_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE dose_logs SET snoozedUntil = :until WHERE id = :id AND status = 'PENDING'")
    suspend fun snooze(id: Long, until: Instant)

    @Query("UPDATE dose_logs SET status = 'MISSED' WHERE status = 'PENDING' AND id IN (:ids)")
    suspend fun markMissed(ids: List<Long>)
}
