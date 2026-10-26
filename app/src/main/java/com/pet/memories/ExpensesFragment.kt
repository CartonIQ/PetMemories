package com.pet.memories

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pet.memories.data.ExpenseEntity
import com.pet.memories.databinding.DialogAddExpenseBinding
import com.pet.memories.databinding.FragmentExpensesBinding
import com.pet.memories.databinding.ItemExpenseBinding
import java.util.*

class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpensesViewModel by viewModels()
    private lateinit var expensesAdapter: ExpensesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        expensesAdapter = ExpensesAdapter()
        binding.recyclerExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expensesAdapter
        }

        viewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            expensesAdapter.submitList(expenses)
            binding.tvEmptyExpenses.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.currentMonthTotal.observe(viewLifecycleOwner) { total ->
            binding.tvMonthlyTotal.text = String.format(Locale.US, "${getCurrencySymbol()}%.2f", total)
        }

        binding.fabAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }
    }

    private fun showAddExpenseDialog() {
        val dialogBinding = DialogAddExpenseBinding.inflate(layoutInflater)
        dialogBinding.etExpenseAmount.hint = "Amount (${getCurrencySymbol()})"
        val calendar = Calendar.getInstance()
        val todayStr = String.format(Locale.US, "%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        dialogBinding.etExpenseDate.setText(todayStr)

        dialogBinding.etExpenseDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    dialogBinding.etExpenseDate.setText(
                        String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Save Expense") { _, _ ->
                val amount = dialogBinding.etExpenseAmount.text?.toString()?.toDoubleOrNull()
                val title = dialogBinding.etExpenseTitle.text?.toString()?.trim().orEmpty().ifEmpty { "Expense" }
                val category = dialogBinding.etExpenseCategory.text?.toString()?.trim().orEmpty().ifEmpty { "Food" }
                val date = dialogBinding.etExpenseDate.text?.toString()?.trim() ?: todayStr

                if (amount != null && amount > 0.0) {
                    viewModel.addExpense(
                        petId = null,
                        title = title,
                        amount = amount,
                        category = category,
                        date = date
                    )
                    Toast.makeText(requireContext(), "Expense logged!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ExpensesAdapter : RecyclerView.Adapter<ExpensesAdapter.ExpenseViewHolder>() {
        private var list = listOf<ExpenseEntity>()

        fun submitList(items: List<ExpenseEntity>) {
            list = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
            val b = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ExpenseViewHolder(b)
        }

        override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int = list.size

        inner class ExpenseViewHolder(private val b: ItemExpenseBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(item: ExpenseEntity) {
                b.tvExpenseTitle.text = item.title
                b.tvExpenseCategoryDate.text = "${item.category} • ${item.date}"
                b.tvExpenseAmount.text = String.format(Locale.US, "${getCurrencySymbol()}%.2f", item.amount)

                b.tvExpenseIcon.text = when (item.category.lowercase(Locale.US)) {
                    "food" -> "🍖"
                    "vet" -> "🩺"
                    "toys", "toy" -> "🧸"
                    "grooming" -> "✂️"
                    else -> "🐾"
                }
            }
        }
    }

    private fun getCurrencySymbol(): String {
        val prefs = requireContext().getSharedPreferences("pet_settings", android.content.Context.MODE_PRIVATE)
        return when (prefs.getString("currency", "INR")) {
            "USD" -> "$"
            "GBP" -> "£"
            "EUR" -> "€"
            else -> "₹"
        }
    }

}
