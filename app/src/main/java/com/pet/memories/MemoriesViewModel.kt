package com.pet.memories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pet.memories.data.AppDatabase
import com.pet.memories.data.MemoryEntity
import com.pet.memories.data.PetEntity
import com.pet.memories.data.PetRepository
import kotlinx.coroutines.launch

class MemoriesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PetRepository

    val allMemories: LiveData<List<MemoryEntity>>
    val allPets: LiveData<List<PetEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PetRepository(database)
        allMemories = repository.allMemories.asLiveData()
        allPets = repository.allPets.asLiveData()
    }

    fun addMemory(
        petId: Long,
        photoUri: String?,
        caption: String,
        date: String
    ) = viewModelScope.launch {
        val memory = MemoryEntity(
            petId = petId,
            photoUri = photoUri,
            caption = caption,
            date = date
        )
        repository.insertMemory(memory)
    }
}
