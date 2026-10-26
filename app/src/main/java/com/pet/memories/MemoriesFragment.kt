package com.pet.memories

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pet.memories.ads.AdManager
import com.pet.memories.data.MemoryEntity
import com.pet.memories.data.PetEntity
import com.pet.memories.databinding.DialogAddMemoryBinding
import com.pet.memories.databinding.FragmentMemoriesBinding
import com.pet.memories.databinding.ItemMemoryBinding
import com.pet.memories.databinding.ItemNativeAdBinding
import java.time.LocalDate
import java.util.*

class MemoriesFragment : Fragment() {

    private var _binding: FragmentMemoriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MemoriesViewModel by viewModels()
    private lateinit var memoriesAdapter: MemoriesAdapter

    private var selectedMemoryPhotoUri: Uri? = null
    private var tempMemoryPhotoView: ImageView? = null
    private var petsList = listOf<PetEntity>()

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val internalPath = com.pet.memories.util.FileUtils.copyImageToInternalStorage(requireContext(), uri, "memory")
            if (internalPath != null) {
                selectedMemoryPhotoUri = Uri.fromFile(java.io.File(internalPath))
                tempMemoryPhotoView?.setImageURI(selectedMemoryPhotoUri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        memoriesAdapter = MemoriesAdapter()
        binding.recyclerMemories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memoriesAdapter
        }

        viewModel.allPets.observe(viewLifecycleOwner) { pets ->
            petsList = pets
            memoriesAdapter.setPets(pets)
        }

        viewModel.allMemories.observe(viewLifecycleOwner) { memories ->
            memoriesAdapter.submitMemories(memories)
            binding.tvEmptyMemories.visibility = if (memories.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAddMemory.setOnClickListener {
            if (petsList.isEmpty()) {
                Toast.makeText(requireContext(), "Please add a pet first before recording memories!", Toast.LENGTH_LONG).show()
            } else {
                showAddMemoryDialog()
            }
        }
    }

    private fun showAddMemoryDialog() {
        val dialogBinding = DialogAddMemoryBinding.inflate(layoutInflater)
        selectedMemoryPhotoUri = null
        tempMemoryPhotoView = dialogBinding.dialogImgMemoryPreview

        val calendar = Calendar.getInstance()
        val todayStr = String.format(Locale.US, "%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        dialogBinding.etMemoryDate.setText(todayStr)

        dialogBinding.etMemoryDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    dialogBinding.etMemoryDate.setText(
                        String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.dialogBtnChooseMemoryPhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Save Memory") { _, _ ->
                val caption = dialogBinding.etMemoryCaption.text?.toString()?.trim() ?: ""
                val date = dialogBinding.etMemoryDate.text?.toString()?.trim() ?: todayStr
                val selectedPetId = petsList.firstOrNull()?.id ?: 0L

                if (caption.isNotEmpty()) {
                    viewModel.addMemory(
                        petId = selectedPetId,
                        photoUri = selectedMemoryPhotoUri?.toString(),
                        caption = caption,
                        date = date
                    )
                    Toast.makeText(requireContext(), "Memory saved!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    sealed class FeedItem {
        data class Memory(val entity: MemoryEntity) : FeedItem()
        data class Ad(var nativeAd: NativeAd? = null) : FeedItem()
    }

    inner class MemoriesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val items = mutableListOf<FeedItem>()
        private var petsMap = mapOf<Long, PetEntity>()

        fun setPets(pets: List<PetEntity>) {
            petsMap = pets.associateBy { it.id }
            notifyDataSetChanged()
        }

        fun submitMemories(memories: List<MemoryEntity>) {
            items.clear()
            // Native Ad inserted every 5th item as per requirement
            for ((index, memory) in memories.withIndex()) {
                items.add(FeedItem.Memory(memory))
                if ((index + 1) % 5 == 0) {
                    val adItem = FeedItem.Ad()
                    items.add(adItem)
                    // Request native ad
                    AdManager.loadNativeAd(requireContext()) { nativeAd ->
                        adItem.nativeAd = nativeAd
                        notifyDataSetChanged()
                    }
                }
            }
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is FeedItem.Memory -> 0
                is FeedItem.Ad -> 1
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                val b = ItemMemoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                MemoryViewHolder(b)
            } else {
                val b = ItemNativeAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(b)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is FeedItem.Memory -> (holder as MemoryViewHolder).bind(item.entity)
                is FeedItem.Ad -> (holder as AdViewHolder).bind(item.nativeAd)
            }
        }

        override fun getItemCount(): Int = items.size

        inner class MemoryViewHolder(private val b: ItemMemoryBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(memory: MemoryEntity) {
                val pet = petsMap[memory.petId]
                b.tvMemoryPetName.text = pet?.name ?: "Pet Memory"
                b.tvMemoryDate.text = memory.date
                b.tvMemoryCaption.text = memory.caption

                if (memory.photoUri != null) {
                    b.imgMemoryPhoto.visibility = View.VISIBLE
                    try {
                        b.imgMemoryPhoto.setImageURI(Uri.parse(memory.photoUri))
                    } catch (e: Exception) {
                        b.imgMemoryPhoto.setImageResource(R.drawable.ic_memories)
                    }
                } else {
                    b.imgMemoryPhoto.visibility = View.GONE
                }
            }
        }

        inner class AdViewHolder(private val b: ItemNativeAdBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(nativeAd: NativeAd?) {
                if (nativeAd != null) {
                    b.nativeAdView.headlineView = b.adHeadline
                    b.nativeAdView.bodyView = b.adBody
                    b.nativeAdView.callToActionView = b.adCallToAction
                    b.nativeAdView.mediaView = b.adMedia

                    b.adHeadline.text = nativeAd.headline
                    b.adBody.text = nativeAd.body
                    b.adCallToAction.text = nativeAd.callToAction ?: "Learn More"
                    nativeAd.mediaContent?.let { b.adMedia.mediaContent = it }
                    b.nativeAdView.setNativeAd(nativeAd)
                    b.root.visibility = View.VISIBLE
                } else {
                    b.root.visibility = View.GONE
                }
            }
        }
    }
}
