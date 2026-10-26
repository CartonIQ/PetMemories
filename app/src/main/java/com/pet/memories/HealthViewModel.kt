package com.pet.memories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pet.memories.data.*
import kotlinx.coroutines.launch

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    val repository: PetRepository

    val allWeights: LiveData<List<WeightEntity>>
    val allPets: LiveData<List<PetEntity>>
    val allFood: LiveData<List<FoodEntity>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PetRepository(db)
        allWeights = repository.allWeights.asLiveData()
        allPets = repository.allPets.asLiveData()
        allFood = repository.allFood.asLiveData()
    }

    fun addWeight(petId: Long, weight: Double, date: String, notes: String? = null) = viewModelScope.launch {
        repository.insertWeight(
            WeightEntity(
                petId = petId,
                weight = weight,
                date = date,
                notes = notes
            )
        )
    }

    fun addVaccine(petId: Long, name: String, dueDate: String) = viewModelScope.launch {
        repository.insertVaccine(
            VaccineEntity(
                petId = petId,
                name = name,
                dueDate = dueDate
            )
        )
    }

    fun getVaccinesForPet(petId: Long): LiveData<List<VaccineEntity>> {
        return repository.getVaccinesForPet(petId).asLiveData()
    }

    

    

    fun addFood(petId: Long, foodName: String, portion: String, time: String) = viewModelScope.launch {
        repository.insertFood(
            FoodEntity(
                petId = petId,
                foodName = foodName,
                portion = portion,
                time = time,
                date = java.time.LocalDate.now().toString()
            )
        )
    }

    fun deleteFood(food: FoodEntity) = viewModelScope.launch {
        repository.deleteFood(food)
    }

}
