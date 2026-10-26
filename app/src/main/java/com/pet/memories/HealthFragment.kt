package com.pet.memories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pet.memories.data.FoodEntity
import com.pet.memories.databinding.FragmentHealthBinding

class HealthFragment : Fragment() {

    private var _binding: FragmentHealthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HealthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FAB opens the food log dialog
        binding.fabAddHealth.setOnClickListener {
            showAddFoodDialog()
        }

        // Observe food logs and build the list
        viewModel.allFood.observe(viewLifecycleOwner) { foodList ->
            binding.layoutFoodList.removeAllViews()
            if (foodList.isEmpty()) {
                binding.tvEmptyMeds.visibility = View.VISIBLE
                binding.tvEmptyMeds.text = "No food logged yet. Tap 'Log Food' to start!"
            } else {
                binding.tvEmptyMeds.visibility = View.GONE
                for (food in foodList) {
                    val foodView = layoutInflater.inflate(R.layout.item_food, binding.layoutFoodList, false)
                    foodView.findViewById<android.widget.TextView>(R.id.tvFoodName).text = food.foodName
                    foodView.findViewById<android.widget.TextView>(R.id.tvFoodDetails).text = "${food.portion} • ${food.time} • ${food.date}"
                    foodView.setOnClickListener {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete Food Log?")
                            .setMessage("Remove ${food.foodName}?")
                            .setPositiveButton("Delete") { _, _ ->
                                viewModel.deleteFood(food)
                                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    binding.layoutFoodList.addView(foodView)
                }
            }
        }
    }

    private fun showAddFoodDialog() {
        val dialogBinding = com.pet.memories.databinding.DialogAddFoodBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etFoodName.text?.toString()?.trim() ?: ""
                val portion = dialogBinding.etFoodPortion.text?.toString()?.trim() ?: ""
                val time = dialogBinding.etFoodTime.text?.toString()?.trim() ?: "Meal"

                if (name.isNotEmpty()) {
                    viewModel.addFood(0L, name, portion, time)
                    Toast.makeText(requireContext(), "Food logged!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    dialogBinding.etFoodName.error = "Required"
                }
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
