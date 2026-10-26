package com.pet.memories.util

import android.content.Context
import android.content.Intent
import com.pet.memories.data.PetEntity

object ShareCardGenerator {
    fun sharePetProfileText(context: Context, pet: PetEntity) {
        val ageStr = AgeCalculator.calculateAge(pet.birthday)
        val message = "Meet ${pet.name}! ${pet.breed.ifEmpty { "Loving Pet" }} (${pet.petType}), currently \$ageStr old. Tracked with PetMemories 🐾"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share ${pet.name}'s Profile"))
    }
}
