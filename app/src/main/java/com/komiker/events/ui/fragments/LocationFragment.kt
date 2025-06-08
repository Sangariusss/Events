package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.komiker.events.R
import com.komiker.events.data.repository.LocationRepository
import com.komiker.events.databinding.FragmentLocationBinding
import com.komiker.events.ui.adapters.LocationAdapter
import com.komiker.events.viewmodels.CreateEventViewModel
import com.komiker.events.viewmodels.FilterViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LocationFragment : Fragment() {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationAdapter: LocationAdapter
    private lateinit var locationRepository: LocationRepository

    private val createEventViewModel: CreateEventViewModel by activityViewModels()
    private val filterViewModel: FilterViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationRepository = LocationRepository(requireContext())
        setupRecyclerView()
        setupSearchField()
        setupButtonBack()
        setupOnBackPressed()
        loadLocations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupButtonBack() {
        binding.buttonBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, true) {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        locationAdapter = LocationAdapter { selectedLocation ->
            val sourceFragmentId = arguments?.getInt("sourceFragmentId")

            if (sourceFragmentId == R.id.CreateEventFragment) {
                createEventViewModel.setLocation(selectedLocation.address)
            } else {
                filterViewModel.setLocation(selectedLocation.address)
            }

            findNavController().popBackStack()
        }
        binding.recyclerViewLocations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = locationAdapter
            doOnLayout {
                val recyclerHeight = height
                if (recyclerHeight > 0) {
                    locationAdapter.setItemHeight((recyclerHeight * 0.081).toInt())
                }
            }
        }
    }

    private fun setupSearchField() {
        val emptyDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_empty)
        val filledDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_filled)
        var searchJob: Job? = null
        binding.editTextFindLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.editTextFindLocation.background = if (s.isNullOrEmpty()) emptyDrawable else filledDrawable
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    filterLocations(s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadLocations() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                locationRepository.getLocations().collectLatest { locations ->
                    locationAdapter.submitList(locations)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load addresses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterLocations(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                locationRepository.getLocations(query).collectLatest { locations ->
                    locationAdapter.submitList(locations)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Addresses could not be found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}