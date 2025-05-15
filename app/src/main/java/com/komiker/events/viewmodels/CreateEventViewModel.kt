package com.komiker.events.viewmodels

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
    var tags: List<String>? = null
    var location: String? = null
}