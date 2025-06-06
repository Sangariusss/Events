package com.komiker.events.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.komiker.events.ui.adapters.ImageAdapter

class CreateEventViewModel : ViewModel() {

    var title: String? = null
    var description: String? = null
    var images: MutableList<ImageAdapter.ImageItem> = mutableListOf()
    var startDate: String? = null
    var endDate: String? = null
    var hour: String? = null
    var minute: String? = null
    var isAmSelected: Boolean = true
    private var isCleared: Boolean = false

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

    fun shouldRequestLocation(): Boolean {
        return shouldRequestLocation
    }

    fun disableLocationRequest() {
        shouldRequestLocation = false
    }

    fun enableLocationRequest() {
        shouldRequestLocation = true
    }

    fun clear() {
        title = null
        description = null
        images.clear()
        startDate = null
        endDate = null
        hour = null
        minute = null
        isAmSelected = true
        selectedMonth = null
        selectedDay = null
        selectedYear = null
        setLocation(null)
        setTags(null)
        resetFiltersApplied()
        isCleared = true
    }

    fun isCleared(): Boolean = isCleared

    fun resetCleared() {
        isCleared = false
    }
}