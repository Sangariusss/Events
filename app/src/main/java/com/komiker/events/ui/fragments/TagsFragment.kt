package com.komiker.events.ui.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.komiker.events.R
import com.komiker.events.data.database.AppDatabase
import com.komiker.events.data.models.SelectedTags
import com.komiker.events.data.models.TagItem
import com.komiker.events.data.repository.TagRepository
import com.komiker.events.databinding.FragmentTagsBinding
import com.komiker.events.ui.adapters.TagsAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TagsFragment : Fragment() {

    private var _binding: FragmentTagsBinding? = null
    private val binding get() = _binding!!

    private lateinit var tagsAdapter: TagsAdapter
    private lateinit var tagRepository: TagRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(requireContext())
        tagRepository = TagRepository(database)
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
        tagRepository.setupRealtimeUpdates()
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
            val bundle = Bundle().apply {
                putSerializable("selectedTags", selectedTags)
            }
            if (sourceFragmentId == R.id.CreateEventFragment) {
                findNavController().popBackStack(R.id.CreateEventFragment, false)
            } else {
                val actionId = when (sourceFragmentId) {
                    R.id.FilterFragment -> R.id.action_TagsFragment_to_FilterFragment
                    else -> R.id.action_TagsFragment_to_FilterFragment
                }
                findNavController().navigate(actionId, bundle)
            }
        }
    }

    private fun setupEditTextBackgroundChange() {
        val editText = binding.editTextFindLocation

        val emptyDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_empty)
        val filledDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_filled)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    editText.background = emptyDrawable
                    loadTags()
                } else {
                    editText.background = filledDrawable
                    filterTags(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        tagsAdapter = TagsAdapter {
            //
        }

        binding.recyclerViewTags.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tagsAdapter

            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val recyclerHeight = binding.recyclerViewTags.height
                    if (recyclerHeight > 0) {
                        val headerHeight = (recyclerHeight * 0.054).toInt()
                        val subTagHeight = (recyclerHeight * 0.081).toInt()
                        tagsAdapter.setHeaderHeight(headerHeight)
                        tagsAdapter.setSubTagHeight(subTagHeight)
                        binding.recyclerViewTags.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            })
        }
    }

    private fun loadTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            tagRepository.getTagCategories().collectLatest { categories ->
                if (categories.isEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        tagRepository.syncTagCategories()
                    }
                } else {
                    val tagItems = categories.flatMap { category ->
                        listOf(TagItem.Header(category.name)) + category.subTags.map { TagItem.SubTag(it, category.name) }
                    }
                    tagsAdapter.submitList(tagItems)
                }
            }
        }
    }

    private fun filterTags(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            tagRepository.getTagCategories().collectLatest { categories ->
                val filteredItems = categories.flatMap { category ->
                    val filteredSubTags = category.subTags.filter {
                        it.contains(query, ignoreCase = true)
                    }
                    if (filteredSubTags.isNotEmpty()) {
                        listOf(TagItem.Header(category.name)) + filteredSubTags.map { TagItem.SubTag(it, category.name) }
                    } else {
                        emptyList()
                    }
                }
                tagsAdapter.submitList(filteredItems)
            }
        }
    }
}