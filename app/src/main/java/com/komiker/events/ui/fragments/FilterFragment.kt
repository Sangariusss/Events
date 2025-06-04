package com.komiker.events.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.chip.Chip
import com.komiker.events.R
import com.komiker.events.databinding.FragmentFilterBinding
import com.komiker.events.viewmodels.CreateEventViewModel
import com.shawnlin.numberpicker.NumberPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Calendar
import java.util.Locale

class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel: CreateEventViewModel by activityViewModels()

    private val locationPermissionRequest = this.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                fetchLastLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                fetchLastLocation()
            }
            else -> {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    addLocationChip(getString(R.string.permission_denied))
                }
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
        setupNumberPickers(view, savedInstanceState)
        setupButtonLocation()
        setupButtonAllCategory()
        setupButtonCheckmark()
        observeViewModel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (viewModel.location.value.isNullOrEmpty()) {
            handleLocationRequest()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveFilters()
        outState.putInt("savedMonth", binding.numberPickerMonth.value)
        outState.putInt("savedDay", binding.numberPickerDay.value)
        outState.putInt("savedYear", binding.numberPickerYear.value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
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

        val spannable = android.text.SpannableString(fullText)
        val redColor = ContextCompat.getColor(requireContext(), R.color.red_50)

        spannable.setSpan(
            android.text.style.ForegroundColorSpan(redColor),
            asteriskIndex,
            asteriskIndex + 1,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.textSetTheDateTitle.text = spannable
    }

    private fun setupOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveFilters()
                viewModel.clear()
                findNavController().navigate(R.id.action_FilterFragment_to_MainMenuFragment)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setupButtonBack() {
        binding.buttonBack.setOnClickListener {
            saveFilters()
            viewModel.clear()
            findNavController().navigate(R.id.action_FilterFragment_to_MainMenuFragment)
        }
    }

    private fun setupButtonCheckmark() {
        binding.buttonCheckmark.setOnClickListener {
            saveFilters()
            viewModel.clear()
            findNavController().navigate(R.id.action_FilterFragment_to_MainMenuFragment)
        }
    }

    private fun saveFilters() {
        val monthPicker = binding.numberPickerMonth
        val dayPicker = binding.numberPickerDay
        val yearPicker = binding.numberPickerYear

        val month = monthPicker.displayedValues[monthPicker.value]
        val day = dayPicker.value
        val year = yearPicker.value

        viewModel.startDate = "$month $day, $year"
        viewModel.selectedMonth = monthPicker.value
        viewModel.selectedDay = dayPicker.value
        viewModel.selectedYear = yearPicker.value
    }

    private fun setupNumberPickers(view: View, savedInstanceState: Bundle?) {
        val monthPicker = view.findViewById<NumberPicker>(R.id.number_picker_month)
        val dayPicker = view.findViewById<NumberPicker>(R.id.number_picker_day)
        val yearPicker = view.findViewById<NumberPicker>(R.id.number_picker_year)

        setNumberPickerFont(monthPicker, dayPicker, yearPicker)

        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        setupNumberPicker(monthPicker, 0, months.size - 1, months)
        setupNumberPicker(dayPicker, 1, 31)
        setupNumberPicker(yearPicker, currentYear, currentYear + 10)

        val customFormatter = NumberPicker.Formatter { value -> value.toString() }
        yearPicker.formatter = customFormatter

        if (savedInstanceState != null) {
            val savedMonthValue = savedInstanceState.getInt("savedMonth", -1)
            val savedDayValue = savedInstanceState.getInt("savedDay", -1)
            val savedYearValue = savedInstanceState.getInt("savedYear", -1)

            if (savedMonthValue != -1 && savedDayValue != -1 && savedYearValue != -1) {
                monthPicker.value = savedMonthValue.coerceIn(0, 11)
                dayPicker.value = savedDayValue.coerceIn(1, 31)
                yearPicker.value = savedYearValue.coerceIn(currentYear, currentYear + 10)
                updateDayPickerMaxValue(savedMonthValue, savedYearValue)
            }
        } else {
            val vmMonth = viewModel.selectedMonth
            val vmDay = viewModel.selectedDay
            val vmYear = viewModel.selectedYear

            if (vmMonth != null && vmDay != null && vmYear != null) {
                monthPicker.value = vmMonth.coerceIn(0, 11)
                dayPicker.value = vmDay.coerceIn(1, 31)
                yearPicker.value = vmYear.coerceIn(currentYear, currentYear + 10)
                updateDayPickerMaxValue(vmMonth, vmYear)
            } else {
                setCurrentDateInPickers(monthPicker, dayPicker, yearPicker)
            }
        }

        monthPicker.setOnValueChangedListener { _, _, newVal ->
            updateDayPickerMaxValue(newVal, yearPicker.value)
            saveFilters()
        }

        dayPicker.setOnValueChangedListener { _, _, _ ->
            saveFilters()
        }

        yearPicker.setOnValueChangedListener { _, _, newVal ->
            updateDayPickerMaxValue(monthPicker.value, newVal)
            saveFilters()
        }
    }

    private fun setNumberPickerFont(vararg pickers: NumberPicker) {
        val customFont: Typeface? = ResourcesCompat.getFont(requireContext(), R.font.fixel_bold)
        pickers.forEach {
            it.typeface = customFont
            it.setSelectedTypeface(customFont)
        }
    }

    private fun setupNumberPicker(picker: NumberPicker, minValue: Int, maxValue: Int, values: Array<String>? = null) {
        picker.minValue = minValue
        picker.maxValue = maxValue
        values?.let { picker.displayedValues = it }
    }

    private fun setCurrentDateInPickers(monthPicker: NumberPicker, dayPicker: NumberPicker, yearPicker: NumberPicker) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        yearPicker.value = currentYear
        monthPicker.value = currentMonth
        updateDayPickerMaxValue(currentMonth, currentYear)
        dayPicker.value = currentDay
        saveFilters()
    }

    private fun updateDayPickerMaxValue(month: Int, year: Int) {
        val maxDay = when (month) {
            0, 2, 4, 6, 7, 9, 11 -> 31
            3, 5, 8, 10 -> 30
            else -> if (isLeapYear(year)) 29 else 28
        }
        binding.numberPickerDay.maxValue = maxDay
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun setupButtonLocation() {
        binding.buttonLocation.setOnClickListener {
            saveFilters()
            val bundle = Bundle().apply {
                putInt("sourceFragmentId", R.id.FilterFragment)
            }
            findNavController().navigate(
                R.id.action_FilterFragment_to_LocationFragment,
                bundle
            )
        }
    }

    private fun setupButtonAllCategory() {
        binding.buttonTags.setOnClickListener {
            saveFilters()
            val bundle = Bundle().apply {
                putInt("sourceFragmentId", R.id.FilterFragment)
            }
            findNavController().navigate(
                R.id.action_FilterFragment_to_TagsFragment,
                bundle
            )
        }
    }

    private fun handleLocationRequest() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            return
        }
        fetchLastLocation()
    }

    private fun fetchLastLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            val task: Task<Location> = fusedLocationClient.lastLocation
            task.addOnSuccessListener { location ->
                if (location != null) {
                    getAddressFromLocation(location)
                } else {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        addLocationChip(getString(R.string.location_not_available))
                    }
                }
            }

        } else {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                addLocationChip(getString(R.string.permission_denied))
            }
        }
    }

    private fun getAddressFromLocation(location: Location) {
        val geocoder = Geocoder(requireContext(), Locale.ENGLISH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(location.latitude, location.longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        displayAddressFromLocation(addresses)
                    }
                }

                override fun onError(errorMessage: String?) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        addLocationChip(getString(R.string.location_not_available))
                    }
                }
            })
        } else {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    displayAddressFromLocation(addresses)
                }
            } catch (e: IOException) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    addLocationChip(getString(R.string.location_not_available))
                }
            }
        }
    }

    private fun displayAddressFromLocation(addresses: List<android.location.Address>?) {
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val city = address.locality ?: "Unknown city"
            val state = address.adminArea ?: "Unknown state"
            val country = address.countryName ?: "Unknown country"
            val addressString = "$city, $state, $country"
            if (viewModel.location.value.isNullOrEmpty()) {
                viewModel.setLocation(addressString)
            }
        } else {
            addLocationChip(getString(R.string.location_not_available))
        }
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
        binding.chipGroupLocation.removeAllViews()
        val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
            text = location
            isCloseIconVisible = true
            configureChip()
            setOnCloseIconClickListener {
                viewModel.setLocation(null)
                binding.chipGroupLocation.removeView(this)
            }
        }
        binding.chipGroupLocation.addView(chip)
    }

    private fun addTagChip(tag: String) {
        val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
            text = tag
            isCloseIconVisible = true
            configureChip()
            setOnCloseIconClickListener {
                val currentTags = viewModel.tags.value?.toMutableList() ?: mutableListOf()
                currentTags.remove(tag)
                viewModel.setTags(currentTags)
                binding.chipGroupTags.removeView(this)
            }
        }
        binding.chipGroupTags.addView(chip)
    }

    private fun Chip.configureChip() {
        chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.neutral_98)
        setTextColor(ContextCompat.getColorStateList(context, R.color.neutral_0))
        setCloseIconResource(R.drawable.ic_close)
        closeIconTint = ContextCompat.getColorStateList(context, R.color.neutral_0)
        chipStrokeColor = ContextCompat.getColorStateList(context, R.color.neutral_95)
        chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width)
        typeface = ResourcesCompat.getFont(context, R.font.fixel_semibold)
        val contentPadding = resources.getDimension(R.dimen.chip_content_padding)
        chipStartPadding = contentPadding
        chipEndPadding = contentPadding
        textStartPadding = contentPadding
        textEndPadding = contentPadding
        iconStartPadding = contentPadding
    }
}