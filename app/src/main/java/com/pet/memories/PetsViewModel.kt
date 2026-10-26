package com.pet.memories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pet.memories.data.AppDatabase
import com.pet.memories.data.PetEntity
import com.pet.memories.data.PetRepository
import kotlinx.coroutines.launch

class PetsViewModel(application: Application) : AndroidViewModel(application) {
    val repository: PetRepository

    val allPets: LiveData<List<PetEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PetRepository(database)
        allPets = repository.allPets.asLiveData()
    }

    fun addPet(
        name: String,
        photoUri: String?,
        breed: String,
        birthday: String,
        petType: String,
        microchip: String?,
        weight: Double?
    ) = viewModelScope.launch {
        val pet = PetEntity(
            name = name,
            photoUri = photoUri,
            breed = breed,
            birthday = birthday,
            petType = petType,
            microchip = microchip,
            weight = weight
        )
        repository.insertPet(pet)
    }

    fun updatePetPhoto(petId: Long, newPhotoUri: String) = viewModelScope.launch {
        val existing = repository.getPetByIdDirect(petId)
        if (existing != null) {
            val updated = existing.copy(photoUri = newPhotoUri)
            repository.updatePet(updated)
        }
    }


    fun deletePet(petId: Long) = viewModelScope.launch {
        repository.deletePet(petId)
    }

    fun getPetById(id: Long): LiveData<PetEntity?> {
        return repository.getPetById(id).asLiveData()
    }
}
