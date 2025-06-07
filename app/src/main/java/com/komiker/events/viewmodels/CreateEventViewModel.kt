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

    private val _location = MutableLiveData<String?>()
    val location: LiveData<String?> = _location

    private val _tags = MutableLiveData<List<String>?>()
    val tags: LiveData<List<String>?> = _tags

    fun setLocation(newLocation: String?) {
        _location.value = newLocation
    }

    fun setTags(newTags: List<String>?) {
        _tags.value = newTags
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
        _location.value = null
        _tags.value = null
        isCleared = true
    }

    fun isCleared(): Boolean = isCleared
    fun resetCleared() { isCleared = false }
}