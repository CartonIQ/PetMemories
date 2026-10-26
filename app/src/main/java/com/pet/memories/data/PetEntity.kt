package com.pet.memories.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val photoUri: String? = null,
    val breed: String = "",
    val birthday: String, // ISO YYYY-MM-DD
    val petType: String = "Dog",
    val microchip: String? = null,
    val weight: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
