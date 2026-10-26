package com.pet.memories.data

import kotlinx.coroutines.flow.Flow

class PetRepository(private val database: AppDatabase) {

    val allFood = database.foodDao().getAllFood()
    suspend fun insertFood(food: FoodEntity): Long = database.foodDao().insertFood(food)
    suspend fun deleteFood(food: FoodEntity) = database.foodDao().deleteFood(food)


                
    val allPets: Flow<List<PetEntity>> = database.petDao().getAllPets()
    val allMemories: Flow<List<MemoryEntity>> = database.memoryDao().getAllMemories()
    val allWeights: Flow<List<WeightEntity>> = database.weightDao().getAllWeights()
    val allExpenses: Flow<List<ExpenseEntity>> = database.expenseDao().getAllExpenses()

    fun getPetById(id: Long): Flow<PetEntity?> = database.petDao().getPetById(id)
    suspend fun getPetByIdDirect(id: Long): PetEntity? = database.petDao().getPetByIdDirect(id)
    fun getVaccinesForPet(petId: Long): Flow<List<VaccineEntity>> = database.vaccineDao().getVaccinesForPet(petId)
    fun getWeightsForPet(petId: Long): Flow<List<WeightEntity>> = database.weightDao().getWeightsForPet(petId)

    suspend fun insertPet(pet: PetEntity): Long = database.petDao().insertPet(pet)
    suspend fun updatePet(pet: PetEntity) = database.petDao().updatePet(pet)
    suspend fun deletePet(id: Long) = database.petDao().deletePetById(id)

    suspend fun insertMemory(memory: MemoryEntity): Long = database.memoryDao().insertMemory(memory)
    suspend fun insertWeight(weight: WeightEntity): Long = database.weightDao().insertWeight(weight)
    suspend fun insertVaccine(vaccine: VaccineEntity): Long = database.vaccineDao().insertVaccine(vaccine)
    suspend fun insertExpense(expense: ExpenseEntity): Long = database.expenseDao().insertExpense(expense)

    suspend fun getAllPetsDirect(): List<PetEntity> = database.petDao().getAllPetsDirect()
    suspend fun getAllMemoriesDirect(): List<MemoryEntity> = database.memoryDao().getAllMemoriesDirect()
    suspend fun getAllWeightsDirect(): List<WeightEntity> = database.weightDao().getAllWeightsDirect()
    suspend fun getAllVaccinesDirect(): List<VaccineEntity> = database.vaccineDao().getAllVaccinesDirect()
    suspend fun getAllExpensesDirect(): List<ExpenseEntity> = database.expenseDao().getAllExpensesDirect()

    suspend fun restoreAll(
        pets: List<PetEntity>,
        memories: List<MemoryEntity>,
        weights: List<WeightEntity>,
        vaccines: List<VaccineEntity>,
        expenses: List<ExpenseEntity>
    ) {
        database.petDao().insertAllPets(pets)
        database.memoryDao().insertAllMemories(memories)
        database.weightDao().insertAllWeights(weights)
        database.vaccineDao().insertAllVaccines(vaccines)
        database.expenseDao().insertAllExpenses(expenses)
    }
}
