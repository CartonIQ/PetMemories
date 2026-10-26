package com.pet.memories.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weights ORDER BY date ASC")
    fun getAllWeights(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weights WHERE petId = :petId ORDER BY date ASC")
    fun getWeightsForPet(petId: Long): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weights")
    suspend fun getAllWeightsDirect(): List<WeightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWeights(weights: List<WeightEntity>)
}
