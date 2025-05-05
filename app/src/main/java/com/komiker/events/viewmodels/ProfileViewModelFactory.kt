package com.komiker.events.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao

class ProfileViewModelFactory(private val supabaseUserDao: SupabaseUserDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(supabaseUserDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}