package com.komiker.events.viewmodels

import androidx.lifecycle.ViewModel

class CreateEventViewModel : ViewModel() {

    var startDate: String? = null
    var endDate: String? = null
    var hour: String? = null
    var minute: String? = null
    var isAmSelected: Boolean = true
}