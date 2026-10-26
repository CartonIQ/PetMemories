package com.pet.memories

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pet.memories.ads.AdManager
import com.pet.memories.data.PetEntity
import com.pet.memories.data.VaccineEntity
import com.pet.memories.databinding.DialogAddVaccineBinding
import com.pet.memories.databinding.FragmentPetDetailBinding
import com.pet.memories.databinding.ItemVaccineBinding
import com.pet.memories.util.*
import java.util.*

class PetDetailFragment : Fragment() {
    private val pickPhotoLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        if (uri != null) {
            val internalPath = com.pet.memories.util.FileUtils.copyImageToInternalStorage(requireContext(), uri, "pet")
            if (internalPath != null) {
                val fileUri = android.net.Uri.fromFile(java.io.File(internalPath))
                binding.imgDetailPhoto.setImageURI(fileUri)
                if (petId > 0L) {
                    petsViewModel.updatePetPhoto(petId, internalPath)
                    Toast.makeText(requireContext(), "Photo updated!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private var _binding: FragmentPetDetailBinding? = null
    private val binding get() = _binding!!

    private val petsViewModel: PetsViewModel by viewModels()
    private val healthViewModel: HealthViewModel by viewModels()

    private var petId: Long = 0L
    private var currentPet: PetEntity? = null
    private lateinit var vaccineAdapter: VaccineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        petId = arguments?.getLong("petId") ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPetDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Back button - goes to previous screen
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Photo click to change
        binding.imgDetailPhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }

        // Setup Vaccines Adapter
        vaccineAdapter = VaccineAdapter()
        binding.recyclerVaccines.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = vaccineAdapter
        }

        petsViewModel.getPetById(petId).observe(viewLifecycleOwner) { pet ->
            currentPet = pet
            if (pet != null) {
                renderPet(pet)
            }
        }

        healthViewModel.getVaccinesForPet(petId).observe(viewLifecycleOwner) { vaccines ->
            vaccineAdapter.submitList(vaccines)
            binding.tvEmptyVaccines.visibility = if (vaccines.isEmpty()) View.VISIBLE else View.GONE
        }

        // Action Buttons
        binding.btnGeneratePassport.setOnClickListener {
            currentPet?.let { pet ->
                AdManager.showInterstitial(requireActivity()) {
                    val prefs = requireContext().getSharedPreferences("pet_settings", android.content.Context.MODE_PRIVATE)
                    val ownerName = prefs.getString("owner_name", "") ?: ""
                    val ownerPhone = prefs.getString("owner_phone", "") ?: ""
                    val ownerContact = if (ownerName.isNotEmpty() || ownerPhone.isNotEmpty()) "$ownerName - $ownerPhone" else "Not specified"
                    val bitmap = PetPassportGenerator.generateAndSharePassport(requireContext(), pet, ownerContact)
                    showImagePreview(bitmap, "Pet Passport - ${pet.name}")
                }
            }
        }

        binding.btnLostPetPoster.setOnClickListener {
            currentPet?.let { pet ->
                AdManager.showInterstitial(requireActivity()) {
                    val prefs = requireContext().getSharedPreferences("pet_settings", android.content.Context.MODE_PRIVATE)
                    val ownerPhone = prefs.getString("owner_phone", "") ?: ""
                    val contactForPoster = if (ownerPhone.isNotEmpty()) ownerPhone else "PLEASE CALL US"
                    val bitmap = LostPetPosterGenerator.generateAndSharePoster(requireContext(), pet, contactForPoster)
                    showImagePreview(bitmap, "Lost Pet Poster - ${pet.name}")
                }
            }
        }

        binding.btnShareProfile.setOnClickListener {
            currentPet?.let { pet ->
                ShareCardGenerator.sharePetProfileText(requireContext(), pet)
            }
        }

        binding.btnAddVaccine.setOnClickListener {
            showAddVaccineDialog()
        }

        binding.btnDeletePet.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Pet")
                .setMessage("Are you sure you want to remove ${currentPet?.name}? All memories and records will be deleted.")
                .setPositiveButton("Delete") { _, _ ->
                    petsViewModel.deletePet(petId)
                    findNavController().popBackStack()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun renderPet(pet: PetEntity) {
        binding.tvDetailName.text = pet.name
        binding.tvDetailBreed.text = "${pet.breed.ifEmpty { "Mixed Breed" }} • ${pet.petType}"
        binding.tvDetailAge.text = AgeCalculator.calculateAge(pet.birthday)
        binding.tvDetailNextBirthday.text = AgeCalculator.daysUntilNextBirthday(pet.birthday)
        binding.tvDetailMicrochip.text = "Microchip: " + (pet.microchip ?: "Not registered")
        binding.tvDetailWeight.text = "Recorded Weight: " + (if (pet.weight != null) "${pet.weight} kg" else "Not recorded")

        if (pet.photoUri != null) {
            try {
                binding.imgDetailPhoto.setImageURI(Uri.parse(pet.photoUri))
                binding.imgDetailPhoto.setPadding(0, 0, 0, 0)
            } catch (e: Exception) {
                binding.imgDetailPhoto.setImageResource(R.drawable.ic_pets)
            }
        } else {
            binding.imgDetailPhoto.setImageResource(R.drawable.ic_pets)
        }
    }

    private fun showAddVaccineDialog() {
        val dialogBinding = DialogAddVaccineBinding.inflate(layoutInflater)
        val calendar = Calendar.getInstance()

        dialogBinding.etVaccineDueDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    dialogBinding.etVaccineDueDate.setText(
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
            .setPositiveButton("Add Reminder") { _, _ ->
                val vName = dialogBinding.etVaccineName.text?.toString()?.trim() ?: ""
                val dueDate = dialogBinding.etVaccineDueDate.text?.toString()?.trim() ?: ""
                if (vName.isNotEmpty() && dueDate.isNotEmpty()) {
                    healthViewModel.addVaccine(petId, vName, dueDate)
                    NotificationScheduler.scheduleVaccineReminder(
                        requireContext(),
                        currentPet?.name ?: "Pet",
                        vName,
                        dueDate
                    )
                    Toast.makeText(requireContext(), R.string.vaccine_scheduled, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class VaccineAdapter : RecyclerView.Adapter<VaccineAdapter.VaccineViewHolder>() {
        private var list = listOf<VaccineEntity>()

        fun submitList(items: List<VaccineEntity>) {
            list = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaccineViewHolder {
            val b = ItemVaccineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VaccineViewHolder(b)
        }

        override fun onBindViewHolder(holder: VaccineViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int = list.size

        inner class VaccineViewHolder(private val b: ItemVaccineBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(item: VaccineEntity) {
                b.tvVaccineName.text = item.name
                b.tvVaccineDue.text = "Due: " + item.dueDate
                b.tvVaccineStatus.text = if (item.completed) "Completed" else "Scheduled"
            }
        }
    }

    private fun showImagePreview(bitmap: android.graphics.Bitmap, title: String) {
        val dialogBinding = com.pet.memories.databinding.DialogImagePreviewBinding.inflate(layoutInflater)
        dialogBinding.ivPreview.setImageBitmap(bitmap)
        dialogBinding.tvPreviewTitle.text = title
        
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()
        dialog.show()
        
        dialogBinding.btnSavePreview.setOnClickListener {
            saveBitmapToDownloads(bitmap, title)
            dialog.dismiss()
        }
        
        dialogBinding.btnViewPreview.setOnClickListener {
            viewBitmapInGallery(bitmap, title)
            dialog.dismiss()
        }
        
        dialogBinding.btnSharePreview.setOnClickListener {
            shareBitmapHelper(bitmap, title)
            dialog.dismiss()
        }
    }
    
    private fun saveBitmapToDownloads(bitmap: android.graphics.Bitmap, title: String) {
        try {
            val fileName = "${title.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"
            val resolver = requireContext().contentResolver
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/PetMemories")
                }
            }
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { output ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, output)
                }
                Toast.makeText(requireContext(), "Saved to Pictures/PetMemories! 📸", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun viewBitmapInGallery(bitmap: android.graphics.Bitmap, title: String) {
        try {
            val fileName = "${title.replace(" ", "_")}_preview.jpg"
            val cacheFile = java.io.File(requireContext().cacheDir, fileName)
            java.io.FileOutputStream(cacheFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "com.pet.memories.fileprovider", cacheFile)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/jpeg")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No viewer available", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareBitmapHelper(bitmap: android.graphics.Bitmap, title: String) {
        try {
            val fileName = "${title.replace(" ", "_")}_share.jpg"
            val cacheFile = java.io.File(requireContext().cacheDir, fileName)
            java.io.FileOutputStream(cacheFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "com.pet.memories.fileprovider", cacheFile)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, title)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(intent, "Share $title"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}
