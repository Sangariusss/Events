package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.komiker.events.R
import com.komiker.events.data.database.AppDatabase
import com.komiker.events.data.models.LocationItem
import com.komiker.events.data.repository.LocationRepository
import com.komiker.events.databinding.FragmentLocationBinding
import com.komiker.events.ui.adapters.LocationAdapter
import com.komiker.events.viewmodels.CreateEventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationFragment : Fragment() {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationAdapter: LocationAdapter
    private val locationRepository: LocationRepository by lazy { LocationRepository(AppDatabase.getDatabase(requireContext())) }
    private val viewModel: CreateEventViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchField()
        setupButtonBack()
        loadLocations()
        locationRepository.setupRealtimeUpdates(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        locationRepository.cleanupRealtime()
        super.onDestroyView()
        _binding = null
    }

    private fun setupButtonBack() {
        binding.buttonBack.setOnClickListener {
            val sourceFragmentId = arguments?.getInt("sourceFragmentId") ?: R.id.FilterFragment
            if (sourceFragmentId == R.id.CreateEventFragment) {
                findNavController().popBackStack(R.id.CreateEventFragment, false)
            } else {
                findNavController().navigate(R.id.action_LocationFragment_to_FilterFragment)
            }
        }
    }

    private fun setupSearchField() {
        val emptyDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_empty)
        val filledDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_filled)
        binding.editTextFindLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.editTextFindLocation.background = if (s.isNullOrEmpty()) emptyDrawable else filledDrawable
                if (s.isNullOrEmpty()) loadLocations() else filterLocations(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        locationAdapter = LocationAdapter { selectedLocation ->
            viewModel.setLocation(selectedLocation.address)
            val sourceFragmentId = arguments?.getInt("sourceFragmentId") ?: R.id.FilterFragment
            if (sourceFragmentId == R.id.CreateEventFragment) {
                findNavController().popBackStack(R.id.CreateEventFragment, false)
            } else {
                findNavController().navigate(R.id.action_LocationFragment_to_FilterFragment)
            }
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

    private fun loadLocations() {
        viewLifecycleOwner.lifecycleScope.launch {
            locationRepository.getLocations().collectLatest { locations ->
                if (locations.isEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO) { locationRepository.syncLocations() }
                    }
                } else {
                    locationAdapter.submitList(locations.map { LocationItem(it.id, it.address, it.updatedAt) })
                }
            }
        }
    }

    private fun filterLocations(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            locationRepository.getLocations().collectLatest { locations ->
                locationAdapter.submitList(locations.filter { it.address.contains(query, ignoreCase = true) }
                    .map { LocationItem(it.id, it.address, it.updatedAt) })
            }
        }
    }
}