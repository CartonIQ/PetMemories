package com.pet.memories

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pet.memories.ads.AdManager
import com.pet.memories.data.AppDatabase
import com.pet.memories.data.PetRepository
import com.pet.memories.databinding.DialogSetVetBinding
import com.pet.memories.databinding.FragmentSettingsBinding
import com.pet.memories.util.BackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: PetRepository
    private var pendingExportJson: String? = null

    // Storage Access Framework: Create JSON file directly in user-selected directory
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingExportJson != null) {
            writeJsonToUri(uri, pendingExportJson!!)
        }
    }

    // Storage Access Framework: Select and open JSON file to restore
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            confirmAndRestoreFromUri(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupCurrencySelector()
        setupOwnerDetails()
        val db = AppDatabase.getDatabase(requireContext())
        repository = PetRepository(db)

        loadVetContact()

        binding.btnCallVet.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("pet_settings", Context.MODE_PRIVATE)
            val phone = prefs.getString("vet_phone", "")
            if (!phone.isNullOrBlank()) {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:\$phone"))
                startActivity(dialIntent)
            } else {
                Toast.makeText(requireContext(), "Please set vet phone number first", Toast.LENGTH_SHORT).show()
                showEditVetDialog()
            }
        }

        binding.btnEditVet.setOnClickListener {
            showEditVetDialog()
        }

        // Export JSON Backup (using BackupHelper.kt)
        binding.btnExportJson.setOnClickListener {
            showExportOptionsDialog()
        }

        // Import JSON Backup (using BackupHelper.kt)
        binding.btnImportJson.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }

        binding.btnShareApp.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "PetMemories - Pet Diary & Age Tracker")
                putExtra(Intent.EXTRA_TEXT, "Keep track of your pet's age, vaccines, memories, and health with PetMemories! Download it today.")
            }
            startActivity(Intent.createChooser(shareIntent, "Share PetMemories"))
        }

        binding.btnRateUs.setOnClickListener {
            try {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=" + requireContext().packageName)
                )
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + requireContext().packageName)
                    )
                    startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(requireContext(), "Could not open Play Store", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Privacy Policy")
                .setMessage("Last Updated: October 2026\n\n" +
                    "PetMemories (\"we\", \"our\", or \"us\") is committed to protecting your privacy. This policy explains how your information is handled when you use our app.\n\n" +
                    "1. INFORMATION WE COLLECT\n" +
                    "We do NOT collect, store, or share any personal information on our servers. All data you enter (pet profiles, photos, memories, health records, expenses, owner details) is stored ONLY on your local device.\n\n" +
                    "2. PHOTOS & CAMERA\n" +
                    "PetMemories requires access to your photo gallery so you can attach pictures of your pets. These photos are copied into the app's private storage on your device and are NEVER uploaded to any server.\n\n" +
                    "3. THIRD-PARTY ADVERTISING\n" +
                    "This app uses Google AdMob to display advertisements. AdMob may collect and use certain data (such as your device's advertising ID) to provide personalized ads. You can learn more about how Google uses data at: https://policies.google.com/technologies/partner-sites\n\n" +
                    "4. DATA SECURITY\n" +
                    "All your data stays on your device. We recommend keeping your device secure with a screen lock. If you uninstall the app, your data will be permanently deleted.\n\n" +
                    "5. CHILDREN'S PRIVACY\n" +
                    "Our app does not knowingly collect personal information from children under 13.\n\n" +
                    "6. CONTACT US\n" +
                    "If you have any questions about this Privacy Policy, contact us at: cartoniq.business@gmail.com\n\n" +
                    "By using PetMemories, you agree to this Privacy Policy.")
                .setPositiveButton("Close", null)
                .show()
        }
    }

    private fun loadVetContact() {
        val prefs = requireContext().getSharedPreferences("pet_settings", Context.MODE_PRIVATE)
        val name = prefs.getString("vet_name", "City Pet Emergency Vet")
        val phone = prefs.getString("vet_phone", "(555) 019-2834")
        binding.tvVetInfo.text = if (!name.isNullOrEmpty()) "$name: $phone" else "No vet contact saved"
    }

    private fun showEditVetDialog() {
        val dialogBinding = DialogSetVetBinding.inflate(layoutInflater)
        val prefs = requireContext().getSharedPreferences("pet_settings", Context.MODE_PRIVATE)
        dialogBinding.etVetName.setText(prefs.getString("vet_name", ""))
        dialogBinding.etVetPhone.setText(prefs.getString("vet_phone", ""))

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding.etVetName.text?.toString()?.trim() ?: ""
                val phone = dialogBinding.etVetPhone.text?.toString()?.trim() ?: ""
                prefs.edit()
                    .putString("vet_name", name)
                    .putString("vet_phone", phone)
                    .apply()
                loadVetContact()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExportOptionsDialog() {
        lifecycleScope.launch {
            val pets = repository.getAllPetsDirect()
            val memories = repository.getAllMemoriesDirect()
            val weights = repository.getAllWeightsDirect()
            val vaccines = repository.getAllVaccinesDirect()
            val expenses = repository.getAllExpensesDirect()

            // Serialize with BackupHelper
            val json = BackupHelper.exportToJson(pets, memories, weights, vaccines, expenses)
            pendingExportJson = json

            val summary = "${pets.size} pets, ${memories.size} memories, ${weights.size} weights, ${vaccines.size} vaccines, ${expenses.size} expenses"

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Export Pet Data Backup")
                .setMessage("Ready to export:\n• $summary")
                .setPositiveButton("Save to Device") { _, _ ->
                    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    val fileName = "PetMemories_Backup_${today}.json"
                    createDocumentLauncher.launch(fileName)
                }
                .setNeutralButton("Share / Send") { _, _ ->
                    AdManager.showInterstitial(requireActivity()) {
                        shareBackupFile(json)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun writeJsonToUri(uri: Uri, json: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(json)
                            writer.flush()
                        }
                    }
                }
                Toast.makeText(requireContext(), R.string.backup_success, Toast.LENGTH_SHORT).show()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Export Complete")
                    .setMessage("Your backup has been saved to your selected storage location.")
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                pendingExportJson = null
            }
        }
    }

    private fun shareBackupFile(json: String) {
        try {
            val cachePath = File(requireContext().cacheDir, "backups")
            cachePath.mkdirs()
            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val file = File(cachePath, "PetMemories_Backup_\$today.json")
            FileOutputStream(file).use { it.write(json.toByteArray()) }

            val uri = FileProvider.getUriForFile(requireContext(), "com.pet.memories.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "PetMemories Backup JSON")
                putExtra(Intent.EXTRA_TEXT, "Here is your PetMemories backup file.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share PetMemories Backup"))
            Toast.makeText(requireContext(), R.string.backup_success, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmAndRestoreFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                        InputStreamReader(stream).readText()
                    } ?: ""
                }

                if (jsonString.isBlank()) {
                    Toast.makeText(requireContext(), "Selected file is empty.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Parse and validate with BackupHelper
                val parsed = BackupHelper.parseJson(jsonString)
                val summary = BackupHelper.getBackupSummary(parsed)

                // Show confirmation dialog before committing to Room database
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Restore Data from Backup?")
                    .setMessage("The backup file contains:\n\n• ${parsed.pets.size} Pets\n• ${parsed.memories.size} Memories\n• ${parsed.weights.size} Weight logs\n• ${parsed.vaccines.size} Vaccine reminders\n• ${parsed.expenses.size} Expenses\n\nDo you want to restore these records into your local PetMemories database?")
                    .setPositiveButton("Restore") { _, _ ->
                        performRestore(parsed, summary)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

            } catch (e: Exception) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Restore Error")
                    .setMessage("Failed to parse the backup file.\n\nError: ${e.message}\n\nPlease ensure this is a valid JSON backup exported from PetMemories.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun performRestore(parsed: BackupHelper.ParsedBackup, summary: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.restoreAll(
                        parsed.pets,
                        parsed.memories,
                        parsed.weights,
                        parsed.vaccines,
                        parsed.expenses
                    )
                }
                Toast.makeText(requireContext(), R.string.restore_success, Toast.LENGTH_SHORT).show()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Restore Successful")
                    .setMessage("Successfully restored:\n$summary\n\nYour pet records, timelines, health history, and expenses are now up to date.")
                    .setPositiveButton("Awesome", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupCurrencySelector() {
        val prefs = requireContext().getSharedPreferences("pet_settings", android.content.Context.MODE_PRIVATE)
        val savedCurrency = prefs.getString("currency", "INR") ?: "INR"
        
        // Set the correct radio button based on saved currency
        when (savedCurrency) {
            "USD" -> binding.rbUsd.isChecked = true
            "GBP" -> binding.rbGbp.isChecked = true
            "EUR" -> binding.rbEur.isChecked = true
            else -> binding.rbInr.isChecked = true
        }
        
        // Listen for changes
        binding.rgCurrency.setOnCheckedChangeListener { _, checkedId ->
            val newCurrency = when (checkedId) {
                binding.rbUsd.id -> "USD"
                binding.rbGbp.id -> "GBP"
                binding.rbEur.id -> "EUR"
                else -> "INR"
            }
            prefs.edit().putString("currency", newCurrency).apply()
        }
    }


    private fun setupOwnerDetails() {
        val prefs = requireContext().getSharedPreferences("pet_settings", android.content.Context.MODE_PRIVATE)
        
        // Load saved values
        binding.etOwnerName.setText(prefs.getString("owner_name", ""))
        binding.etOwnerPhone.setText(prefs.getString("owner_phone", ""))
        
        // Save button
        binding.btnSaveOwner.setOnClickListener {
            val name = binding.etOwnerName.text?.toString()?.trim() ?: ""
            val phone = binding.etOwnerPhone.text?.toString()?.trim() ?: ""
            
            prefs.edit()
                .putString("owner_name", name)
                .putString("owner_phone", phone)
                .apply()
            
            Toast.makeText(requireContext(), "Owner details saved!", Toast.LENGTH_SHORT).show()
        }
    }

}
