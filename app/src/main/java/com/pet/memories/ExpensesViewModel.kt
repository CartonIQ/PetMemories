package com.pet.memories

import android.app.Application
import androidx.lifecycle.*
import com.pet.memories.data.AppDatabase
import com.pet.memories.data.ExpenseEntity
import com.pet.memories.data.PetRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExpensesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PetRepository

    val allExpenses: LiveData<List<ExpenseEntity>>
    val currentMonthTotal: LiveData<Double>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PetRepository(db)
        allExpenses = repository.allExpenses.asLiveData()

        currentMonthTotal = allExpenses.map { list ->
            val now = LocalDate.now()
            val currentYearMonth = String.format("%04d-%02d", now.year, now.monthValue)
            list.filter { it.date.startsWith(currentYearMonth) }
                .sumOf { it.amount }
        }
    }

    fun addExpense(
        petId: Long?,
        title: String,
        amount: Double,
        category: String,
        date: String
    ) = viewModelScope.launch {
        repository.insertExpense(
            ExpenseEntity(
                petId = petId,
                title = title,
                amount = amount,
                category = category,
                date = date
            )
        )
    }
}
