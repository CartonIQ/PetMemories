package com.pet.memories.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_logs")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val foodName: String,
    val portion: String,
    val time: String,
    val date: String,
    val timestamp: Long = System.currentTimeMillis()
)
