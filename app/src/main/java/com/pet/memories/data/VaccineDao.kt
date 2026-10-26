package com.pet.memories.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccineDao {
    @Query("SELECT * FROM vaccines WHERE petId = :petId ORDER BY dueDate ASC")
    fun getVaccinesForPet(petId: Long): Flow<List<VaccineEntity>>

    @Query("SELECT * FROM vaccines")
    suspend fun getAllVaccinesDirect(): List<VaccineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccine(vaccine: VaccineEntity): Long

    @Update
    suspend fun updateVaccine(vaccine: VaccineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVaccines(vaccines: List<VaccineEntity>)
}
