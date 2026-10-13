package com.medtrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Insert suspend fun insert(medication: MedicationEntity): Long
    @Update suspend fun update(medication: MedicationEntity)
    @Delete suspend fun delete(medication: MedicationEntity)

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun get(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE id = :id")
    fun observe(id: Long): Flow<MedicationEntity?>

    @Query("SELECT * FROM medications ORDER BY active DESC, genericName COLLATE NOCASE")
    fun observeAll(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE active = 1")
    suspend fun active(): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<MedicationEntity>

    @Query("UPDATE medications SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}
