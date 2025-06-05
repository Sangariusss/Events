package com.komiker.events.utils

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.komiker.events.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

class LocationManager(
    private val context: Context,
    private val lifecycleScope: CoroutineScope,
    private val onLocationResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.ENGLISH)

    fun requestLocation() {
        if (!hasLocationPermission()) {
            onError(context.getString(R.string.permission_denied))
            return
        }
        fetchLastLocation()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun fetchLastLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        getAddressFromLocation(location)
                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            onError(context.getString(R.string.location_not_available))
                        }
                    }
                }
                .addOnFailureListener {
                    lifecycleScope.launch(Dispatchers.Main) {
                        onError(context.getString(R.string.location_not_available))
                    }
                }
        } catch (e: SecurityException) {
            lifecycleScope.launch(Dispatchers.Main) {
                onError(context.getString(R.string.permission_denied))
            }
        }
    }

    private fun getAddressFromLocation(location: Location) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(location.latitude, location.longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        displayAddressFromLocation(addresses)
                    }
                }

                override fun onError(errorMessage: String?) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        onError(context.getString(R.string.location_not_available))
                    }
                }
            })
        } else {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                lifecycleScope.launch(Dispatchers.Main) {
                    displayAddressFromLocation(addresses)
                }
            } catch (e: IOException) {
                lifecycleScope.launch(Dispatchers.Main) {
                    onError(context.getString(R.string.location_not_available))
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
            onLocationResult("$city, $state, $country")
        } else {
            onError(context.getString(R.string.location_not_available))
        }
    }
}