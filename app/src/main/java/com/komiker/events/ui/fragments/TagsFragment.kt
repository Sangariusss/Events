package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.komiker.events.R
import com.komiker.events.data.database.AppDatabase
import com.komiker.events.data.database.entities.TagCategoryEntity
import com.komiker.events.data.models.TagItem
import com.komiker.events.data.repository.TagRepository
import com.komiker.events.databinding.FragmentTagsBinding
import com.komiker.events.ui.adapters.TagsAdapter
import com.komiker.events.viewmodels.CreateEventViewModel
import com.komiker.events.viewmodels.FilterViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TagsFragment : Fragment() {

    private var _binding: FragmentTagsBinding? = null
    private val binding get() = _binding!!

    private lateinit var tagsAdapter: TagsAdapter
    private val tagRepository: TagRepository by lazy { TagRepository(AppDatabase.getDatabase(requireContext())) }
    private val createEventViewModel: CreateEventViewModel by activityViewModels()
    private val filterViewModel: FilterViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchField()
        setupButtonBack()
        setupOnBackPressed()
        loadTags()
        tagRepository.setupRealtimeUpdates(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        tagRepository.cleanupRealtime()
        super.onDestroyView()
        _binding = null
    }

    private fun setupButtonBack() {
        binding.buttonBack.setOnClickListener {
            saveTagsAndNavigateBack()
        }
    }

    private fun setupOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, true) {
            saveTagsAndNavigateBack()
        }
    }

    private fun saveTagsAndNavigateBack() {
        val selectedTags = tagsAdapter.getSelectedTags()
        val sourceFragmentId = arguments?.getInt("sourceFragmentId")

        if (sourceFragmentId == R.id.CreateEventFragment) {
            createEventViewModel.setTags(selectedTags)
        } else {
            filterViewModel.setTags(selectedTags)
        }
        findNavController().popBackStack()
    }

    private fun setupRecyclerView() {
        val sourceFragmentId = arguments?.getInt("sourceFragmentId")
        val savedTags = if (sourceFragmentId == R.id.CreateEventFragment) {
            createEventViewModel.tags.value
        } else {
            filterViewModel.tags.value
        } ?: emptyList()

        tagsAdapter = TagsAdapter(savedTags.toMutableList()) {}

        binding.recyclerViewTags.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tagsAdapter
            doOnLayout {
                val recyclerHeight = height
                if (recyclerHeight > 0) {
                    tagsAdapter.setHeaderHeight((recyclerHeight * 0.054).toInt())
                    tagsAdapter.setSubTagHeight((recyclerHeight * 0.081).toInt())
                }
            }
        }
    }

    private fun setupSearchField() {
        val emptyDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_empty)
        val filledDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_filled)
        binding.editTextFindTags.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.editTextFindTags.background = if (s.isNullOrEmpty()) emptyDrawable else filledDrawable
                if (s.isNullOrEmpty()) loadTags() else filterTags(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun prepareTagItems(categories: List<TagCategoryEntity>): List<TagItem> {
        return categories.flatMap { category ->
            listOf(TagItem.Header(category.name)) + category.subTags.map { TagItem.SubTag(it, category.name) }
        }
    }

    private fun loadTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            tagRepository.getTagCategories().collectLatest { categories ->
                if (categories.isEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch { tagRepository.syncTagCategories() }
                } else {
                    tagsAdapter.submitList(prepareTagItems(categories))
                }
            }
        }
    }

    private fun filterTags(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            tagRepository.getTagCategories().collectLatest { categories ->
                val filteredItems = categories.flatMap { category ->
                    when {
                        category.name.contains(query, ignoreCase = true) -> prepareTagItems(listOf(category))
                        category.subTags.any { it.contains(query, ignoreCase = true) } -> {
                            val filteredSubTags = category.subTags.filter { it.contains(query, ignoreCase = true) }
                            prepareTagItems(listOf(category.copy(subTags = filteredSubTags)))
                        }
                        else -> emptyList()
                    }
                }
                tagsAdapter.submitList(filteredItems)
            }
        }
    }
}