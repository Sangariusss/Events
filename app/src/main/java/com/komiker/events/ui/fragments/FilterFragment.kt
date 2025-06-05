package com.komiker.events.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.databinding.FragmentFilterBinding
import com.komiker.events.utils.DatePickerManager
import com.komiker.events.utils.LocationManager
import com.komiker.events.utils.addChip
import com.komiker.events.viewmodels.CreateEventViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateEventViewModel by activityViewModels()
    private lateinit var datePickerManager: DatePickerManager
    private lateinit var locationManager: LocationManager
    private val saveFiltersJob = Job()

    private val locationPermissionRequest = this.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                locationManager.requestLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                locationManager.requestLocation()
            }
            else -> {
                addLocationChip(getString(R.string.permission_denied))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSystemBars()
        setupTitleWithRedAsterisk()
        setupOnBackPressed()
        setupButtonBack()
        setupDatePickers(savedInstanceState)
        setupLocationManager()
        setupButtonLocation()
        setupButtonTags()
        setupButtonCheckmark()
        observeViewModel()

        if (viewModel.location.value.isNullOrEmpty()) {
            handleLocationRequest()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        datePickerManager.saveFilters()
        outState.putInt("savedMonth", binding.numberPickerMonth.value)
        outState.putInt("savedDay", binding.numberPickerDay.value)
        outState.putInt("savedYear", binding.numberPickerYear.value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveFiltersJob.cancel()
        _binding = null
    }

    private fun setupSystemBars() {
        requireActivity().window.apply {
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        }
    }

    private fun setupTitleWithRedAsterisk() {
        val fullText = getString(R.string.set_the_date)
        val asteriskIndex = fullText.indexOf('*')
        if (asteriskIndex == -1) {
            binding.textSetTheDateTitle.text = fullText
            return
        }

        val spannable = SpannableString(fullText)
        val redColor = ContextCompat.getColor(requireContext(), R.color.red_50)
        spannable.setSpan(
            ForegroundColorSpan(redColor),
            asteriskIndex,
            asteriskIndex + 1,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.textSetTheDateTitle.text = spannable
    }

    private fun setupOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setupButtonBack() {
        binding.buttonBack.setOnClickListener {
            navigateBack()
        }
    }

    private fun setupButtonCheckmark() {
        binding.buttonCheckmark.setOnClickListener {
            navigateBack()
        }
    }

    private fun setupDatePickers(savedInstanceState: Bundle?) {
        datePickerManager = DatePickerManager(
            monthPicker = binding.numberPickerMonth,
            dayPicker = binding.numberPickerDay,
            yearPicker = binding.numberPickerYear,
            viewModel = viewModel,
            context = requireContext(),
            onDateChanged = { debounceSaveFilters() }
        )
        datePickerManager.restoreState(savedInstanceState)
    }

    private fun setupLocationManager() {
        locationManager = LocationManager(
            context = requireContext(),
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            onLocationResult = { address ->
                if (viewModel.location.value.isNullOrEmpty()) {
                    viewModel.setLocation(address)
                }
            },
            onError = { error ->
                addLocationChip(error)
            }
        )
    }

    private fun setupButtonLocation() {
        binding.buttonLocation.setOnClickListener {
            navigateToFragmentWithSource(R.id.action_FilterFragment_to_LocationFragment)
        }
    }

    private fun setupButtonTags() {
        binding.buttonTags.setOnClickListener {
            navigateToFragmentWithSource(R.id.action_FilterFragment_to_TagsFragment)
        }
    }

    private fun handleLocationRequest() {
        if (!hasLocationPermission()) {
            locationPermissionRequest.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            return
        }
        locationManager.requestLocation()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun observeViewModel() {
        viewModel.location.observe(viewLifecycleOwner) { location ->
            binding.chipGroupLocation.removeAllViews()
            if (!location.isNullOrEmpty()) {
                addLocationChip(location)
            }
        }

        viewModel.tags.observe(viewLifecycleOwner) { tags ->
            binding.chipGroupTags.removeAllViews()
            tags?.forEach { tag ->
                addTagChip(tag)
            }
        }
    }

    private fun addLocationChip(location: String) {
        binding.chipGroupLocation.addChip(location) { chip ->
            viewModel.setLocation(null)
            binding.chipGroupLocation.removeView(chip)
        }
    }

    private fun addTagChip(tag: String) {
        binding.chipGroupTags.addChip(tag) { chip ->
            val currentTags = viewModel.tags.value?.toMutableList() ?: mutableListOf()
            currentTags.remove(tag)
            viewModel.setTags(currentTags)
            binding.chipGroupTags.removeView(chip)
        }
    }

    private fun navigateBack() {
        datePickerManager.saveFilters()
        navigateToMainMenuWithSection()
    }

    private fun navigateToMainMenuWithSection() {
        viewModel.clear()
        findNavController().navigate(
            R.id.action_FilterFragment_to_MainMenuFragment,
            Bundle().apply {
                putString("navigateTo", "home")
            }
        )
    }

    private fun navigateToFragmentWithSource(destinationId: Int) {
        datePickerManager.saveFilters()
        val bundle = Bundle().apply { putInt("sourceFragmentId", R.id.FilterFragment) }
        findNavController().navigate(destinationId, bundle)
    }

    private fun debounceSaveFilters() {
        lifecycleScope.launch(saveFiltersJob) {
            delay(300)
            datePickerManager.saveFilters()
        }
    }
}