package com.komiker.events.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.komiker.events.R
import com.komiker.events.databinding.FragmentFilterBinding
import com.shawnlin.numberpicker.NumberPicker
import java.util.Calendar
import java.util.Locale

class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
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
                locationText.text = "Permission denied"
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
            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            findNavController().navigate(
                R.id.action_FilterFragment_to_MainMenuFragment,
                null,
                NavOptions.Builder()
                    .setEnterAnim(fadeInAnimation)
                    .setExitAnim(fadeOutAnimation)
                    .build()
            )
        }
    }

    private fun setupNumberPickers(view: View) {
        val monthPicker = view.findViewById<NumberPicker>(R.id.number_picker_month)
        val dayPicker = view.findViewById<NumberPicker>(R.id.number_picker_day)
        val yearPicker = view.findViewById<NumberPicker>(R.id.number_picker_year)

        val customFont: Typeface? = ResourcesCompat.getFont(requireContext(), R.font.fixel_bold)
        monthPicker.typeface = customFont
        dayPicker.typeface = customFont
        yearPicker.typeface = customFont
        monthPicker.setSelectedTypeface(customFont)
        dayPicker.setSelectedTypeface(customFont)
        yearPicker.setSelectedTypeface(customFont)

        // Month Picker
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        monthPicker.minValue = 0
        monthPicker.maxValue = months.size - 1
        monthPicker.displayedValues = months
        monthPicker.value = Calendar.getInstance().get(Calendar.MONTH)

        // Day Picker
        dayPicker.minValue = 1
        dayPicker.maxValue = 31
        dayPicker.value = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        // Year Picker
        val year = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = year
        yearPicker.maxValue = year + 10
        yearPicker.value = year

        val customFormatter = NumberPicker.Formatter { value -> value.toString() }
        yearPicker.formatter = customFormatter

        // Listen for changes and update the day picker max value
        monthPicker.setOnValueChangedListener { _, _, newVal ->
            val maxDay = when (newVal) {
                0, 2, 4, 6, 7, 9, 11 -> 31
                3, 5, 8, 10 -> 30
                else -> if (isLeapYear(yearPicker.value)) 29 else 28
            }
            dayPicker.maxValue = maxDay
        }

        yearPicker.setOnValueChangedListener { _, _, newVal ->
            val maxDay = if (monthPicker.value == 1 && isLeapYear(newVal)) 29 else 28
            dayPicker.maxValue = if (monthPicker.value == 1) maxDay else dayPicker.maxValue
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun setupButtonLocation() {
        binding.buttonLocation.setOnClickListener {
            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            findNavController().navigate(
                R.id.action_FilterFragment_to_LocationFragment,
                null,
                NavOptions.Builder()
                    .setEnterAnim(fadeInAnimation)
                    .setExitAnim(fadeOutAnimation)
                    .build()
            )
        }
    }

    private fun setupButtonAllCategory() {
        binding.buttonAllCategory.setOnClickListener {
            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            findNavController().navigate(
                R.id.action_FilterFragment_to_CategoryFragment,
                null,
                NavOptions.Builder()
                    .setEnterAnim(fadeInAnimation)
                    .setExitAnim(fadeOutAnimation)
                    .build()
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
                    val geocoder = Geocoder(requireContext(), Locale.ENGLISH)
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses!!.isNotEmpty()) {
                        val address = addresses[0]
                        val city = address.locality ?: "Unknown city"
                        val state = address.adminArea ?: "Unknown state"
                        val country = address.countryName ?: "Unknown country"
                        val addressString = "$city, $state, $country"
                        locationText.text = addressString
                    } else {
                        locationText.text = "Location not available"
                    }
                } else {
                    locationText.text = "Location not available"
                }
            }
        } else {
            locationText.text = "Permission denied"
        }
    }
}