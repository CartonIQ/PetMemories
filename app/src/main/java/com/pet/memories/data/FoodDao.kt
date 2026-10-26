package com.pet.memories.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    fun getAllFood(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food_logs WHERE petId = :petId ORDER BY timestamp DESC")
    fun getFoodForPet(petId: Long): Flow<List<FoodEntity>>

    @Insert
    suspend fun insertFood(food: FoodEntity): Long

    @Delete
    suspend fun deleteFood(food: FoodEntity)
}
