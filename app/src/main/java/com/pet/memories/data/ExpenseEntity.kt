package com.pet.memories.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val petId: Long? = null,
    val title: String,
    val amount: Double,
    val category: String, // Food, Vet, Toys, Grooming, Other
    val date: String // YYYY-MM-DD
)
