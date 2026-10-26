package com.pet.memories.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY createdAt DESC")
    fun getAllPets(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE id = :id LIMIT 1")
    fun getPetById(id: Long): Flow<PetEntity?>

    @Query("SELECT * FROM pets WHERE id = :id LIMIT 1")
    suspend fun getPetByIdDirect(id: Long): PetEntity?

    @Query("SELECT * FROM pets")
    suspend fun getAllPetsDirect(): List<PetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity): Long

    @Update
    suspend fun updatePet(pet: PetEntity)

    @Query("DELETE FROM pets WHERE id = :id")
    suspend fun deletePetById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPets(pets: List<PetEntity>)
}
