package com.komiker.events.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FilterViewModel : ViewModel() {

    var selectedMonth: Int? = null
    var selectedDay: Int? = null
    var selectedYear: Int? = null

    private val _location = MutableLiveData<String?>()
    val location: LiveData<String?> = _location

    private val _tags = MutableLiveData<List<String>?>()
    val tags: LiveData<List<String>?> = _tags

    private val _filtersApplied = MutableLiveData(false)
    val filtersApplied: LiveData<Boolean> = _filtersApplied

    private var shouldRequestLocation: Boolean = true

    fun setLocation(newLocation: String?) {
        _location.value = newLocation
    }

    fun setTags(newTags: List<String>?) {
        _tags.value = newTags
    }

    fun applyFilters() {
        _filtersApplied.value = true
    }

    fun resetFiltersApplied() {
        _filtersApplied.value = false
    }

    fun clearAll() {
        selectedMonth = null
        selectedDay = null
        selectedYear = null
        _location.value = null
        _tags.value = null
        resetFiltersApplied()
    }

    fun shouldRequestLocation(): Boolean = shouldRequestLocation
    fun disableLocationRequest() { shouldRequestLocation = false }
    fun enableLocationRequest() { shouldRequestLocation = true }
}