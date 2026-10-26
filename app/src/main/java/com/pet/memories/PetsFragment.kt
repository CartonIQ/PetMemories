package com.pet.memories

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pet.memories.data.PetEntity
import com.pet.memories.databinding.DialogAddPetBinding
import com.pet.memories.databinding.FragmentPetsBinding
import com.pet.memories.databinding.ItemPetBinding
import com.pet.memories.util.AgeCalculator
import com.pet.memories.util.PetFacts
import java.time.LocalDate
import java.util.*

class PetsFragment : Fragment() {

    private var _binding: FragmentPetsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PetsViewModel by viewModels()
    private lateinit var petsAdapter: PetsAdapter

    private var selectedPhotoUri: Uri? = null
    private var tempPhotoView: com.google.android.material.imageview.ShapeableImageView? = null

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val internalPath = com.pet.memories.util.FileUtils.copyImageToInternalStorage(requireContext(), uri, "pet")
            if (internalPath != null) {
                selectedPhotoUri = Uri.fromFile(java.io.File(internalPath))
                tempPhotoView?.setImageURI(selectedPhotoUri)
                tempPhotoView?.setPadding(0, 0, 0, 0)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set Daily Pet Fact
        binding.tvFactText.text = PetFacts.getDailyFact()

        // Setup RecyclerView
        petsAdapter = PetsAdapter { pet ->
            val bundle = Bundle().apply { putLong("petId", pet.id) }
            findNavController().navigate(R.id.action_pets_to_petDetail, bundle)
        }
        binding.recyclerPets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = petsAdapter
        }

        viewModel.allPets.observe(viewLifecycleOwner) { pets ->
            petsAdapter.submitList(pets)
            binding.tvEmptyPets.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerPets.scheduleLayoutAnimation()
        }

        // Add Pet FAB
        binding.fabAddPet.setOnClickListener {
            showAddPetDialog()
        }
    }

    private fun showAddPetDialog() {
        val dialogBinding = DialogAddPetBinding.inflate(layoutInflater)
        selectedPhotoUri = null
        tempPhotoView = dialogBinding.dialogImgPetPreview

        val calendar = Calendar.getInstance()
        dialogBinding.etPetBirthday.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    dialogBinding.etPetBirthday.setText(
                        String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.dialogBtnChoosePhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val name = dialogBinding.etPetName.text?.toString()?.trim() ?: ""
                val type = dialogBinding.etPetType.text?.toString()?.trim().orEmpty().ifEmpty { "Dog" }
                val breed = dialogBinding.etPetBreed.text?.toString()?.trim() ?: ""
                val birthday = dialogBinding.etPetBirthday.text?.toString()?.trim() ?: ""
                val microchip = dialogBinding.etPetMicrochip.text?.toString()?.trim()
                val weight = dialogBinding.etPetWeight.text?.toString()?.toDoubleOrNull()

                if (name.isNotEmpty() && birthday.isNotEmpty()) {
                    viewModel.addPet(
                        name = name,
                        photoUri = selectedPhotoUri?.toString(),
                        breed = breed,
                        birthday = birthday,
                        petType = type,
                        microchip = microchip,
                        weight = weight
                    )
                    Toast.makeText(requireContext(), R.string.pet_added_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), R.string.fill_required_fields, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class PetsAdapter(private val onPetClick: (PetEntity) -> Unit) :
        RecyclerView.Adapter<PetsAdapter.PetViewHolder>() {

        private var items = listOf<PetEntity>()

        fun submitList(list: List<PetEntity>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
            val itemBinding = ItemPetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PetViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class PetViewHolder(private val itemBinding: ItemPetBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(pet: PetEntity) {
                itemBinding.tvPetName.text = pet.name
                itemBinding.tvPetBreed.text = "${pet.breed.ifEmpty { "Companion" }} • ${pet.petType}"
                itemBinding.tvPetAge.text = AgeCalculator.calculateAge(pet.birthday)

                if (pet.photoUri != null) {
                    try {
                        itemBinding.imgPetAvatar.setImageURI(Uri.parse(pet.photoUri))
                        itemBinding.imgPetAvatar.setPadding(0, 0, 0, 0)
                    } catch (e: Exception) {
                        itemBinding.imgPetAvatar.setImageResource(R.drawable.ic_pets)
                        itemBinding.imgPetAvatar.setPadding(10, 10, 10, 10)
                    }
                } else {
                    itemBinding.imgPetAvatar.setImageResource(R.drawable.ic_pets)
                    itemBinding.imgPetAvatar.setPadding(10, 10, 10, 10)
                }

                itemBinding.root.setOnClickListener {
                    onPetClick(pet)
                }
            }
        }
    }
}
