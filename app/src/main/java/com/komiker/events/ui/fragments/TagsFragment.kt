package com.komiker.events.ui.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.komiker.events.R
import com.komiker.events.data.database.AppDatabase
import com.komiker.events.data.database.entities.TagCategoryEntity
import com.komiker.events.data.models.SelectedTags
import com.komiker.events.data.models.TagItem
import com.komiker.events.data.repository.TagRepository
import com.komiker.events.databinding.FragmentTagsBinding
import com.komiker.events.ui.adapters.TagsAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TagsFragment : Fragment() {

    private var _binding: FragmentTagsBinding? = null
    private val binding get() = _binding!!

    private lateinit var tagsAdapter: TagsAdapter
    private val tagRepository: TagRepository by lazy {
        TagRepository(AppDatabase.getDatabase(requireContext()))
    }

    private val emptyDrawable: Drawable? by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_empty)
    }
    private val filledDrawable: Drawable? by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_filled)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtonBack()
        setupEditTextBackgroundChange()
        setupRecyclerView()
        loadTags()
        tagRepository.setupRealtimeUpdates(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tagRepository.cleanupRealtime()
        _binding = null
    }

    private fun setupButtonBack() {
        binding.buttonBack.setOnClickListener {
            val sourceFragmentId = arguments?.getInt("sourceFragmentId") ?: R.id.FilterFragment
            val selectedTags = SelectedTags(tagsAdapter.getSelectedTags())
            val bundle = Bundle().apply { putSerializable("selectedTags", selectedTags) }
            setFragmentResult("tagsResult", bundle)
            when (sourceFragmentId) {
                R.id.CreateEventFragment -> findNavController().popBackStack(R.id.CreateEventFragment, false)
                else -> findNavController().navigate(R.id.action_TagsFragment_to_FilterFragment, bundle)
            }
        }
    }

    private fun setupEditTextBackgroundChange() {
        val editText = binding.editTextFindLocation

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                editText.background = if (s.isNullOrEmpty()) emptyDrawable else filledDrawable
                if (s.isNullOrEmpty()) loadTags() else filterTags(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        tagsAdapter = TagsAdapter {}

        binding.recyclerViewTags.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tagsAdapter

            doOnLayout {
                val recyclerHeight = height
                if (recyclerHeight > 0) {
                    val headerHeight = (recyclerHeight * 0.054).toInt()
                    val subTagHeight = (recyclerHeight * 0.081).toInt()
                    tagsAdapter.setHeaderHeight(headerHeight)
                    tagsAdapter.setSubTagHeight(subTagHeight)
                }
            }
        }
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
                    viewLifecycleOwner.lifecycleScope.launch {
                        tagRepository.syncTagCategories()
                    }
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
                    val filteredSubTags = category.subTags.filter { it.contains(query, ignoreCase = true) }
                    if (filteredSubTags.isNotEmpty()) {
                        prepareTagItems(listOf(category.copy(subTags = filteredSubTags)))
                    } else {
                        emptyList()
                    }
                }
                tagsAdapter.submitList(filteredItems)
            }
        }
    }
}