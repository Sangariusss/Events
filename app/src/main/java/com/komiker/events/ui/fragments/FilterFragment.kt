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
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.komiker.events.R
import com.komiker.events.databinding.FragmentFilterBinding
import com.shawnlin.numberpicker.NumberPicker
import java.io.IOException
import java.util.Calendar
import java.util.Locale

class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationText: TextView

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
                locationText.text = getString(R.string.permission_denied)
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
        setupNumberPickers(view)
        setupButtonLocation()
        setupButtonAllCategory()

        locationText = view.findViewById(R.id.text_location)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        handleLocationRequest()
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
                findNavController().navigate(R.id.action_FilterFragment_to_MainMenuFragment)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setupButtonBack() {
        binding.buttonBack.setOnClickListener {
            findNavController().navigate(R.id.action_FilterFragment_to_MainMenuFragment)
        }
    }

    private fun setupNumberPickers(view: View) {
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

        monthPicker.setOnValueChangedListener { _, _, _ ->
            updateDayPickerMaxValue(monthPicker.value, yearPicker.value)
        }

        yearPicker.setOnValueChangedListener { _, _, newVal ->
            updateDayPickerMaxValue(monthPicker.value, newVal)
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
                    locationText.text = getString(R.string.location_not_available)
                }
            }

        } else {
            locationText.text = getString(R.string.permission_denied)
        }
    }

    private fun getAddressFromLocation(location: Location) {
        val geocoder = Geocoder(requireContext(), Locale.ENGLISH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(location.latitude, location.longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    displayAddressFromLocation(addresses)
                }

                override fun onError(errorMessage: String?) {
                    locationText.text = getString(R.string.location_not_available)
                }
            })
        } else {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                displayAddressFromLocation(addresses)
            } catch (e: IOException) {
                locationText.text = getString(R.string.location_not_available)
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
            locationText.text = addressString
        } else {
            locationText.text = getString(R.string.location_not_available)
        }
    }
}
