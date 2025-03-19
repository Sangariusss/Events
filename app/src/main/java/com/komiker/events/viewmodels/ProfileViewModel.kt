package com.komiker.events.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.komiker.events.data.database.dao.SupabaseUserDao
import com.komiker.events.data.database.entities.User
import kotlinx.coroutines.launch

class ProfileViewModel(private val supabaseUserDao: SupabaseUserDao) : ViewModel() {

    val userLiveData = MutableLiveData<User?>()

    fun loadUser(userId: String) {
        viewModelScope.launch {
            try {
                val result = supabaseUserDao.getUserById(userId)
                val dataList = parseData(result.data)

                userLiveData.value = if (!dataList.isNullOrEmpty()) dataList[0] else null
            } catch (e: Exception) {
                userLiveData.value = null
                e.printStackTrace()
            }
        }
    }

    fun updateName(newName: String) {
        viewModelScope.launch {
            val user = userLiveData.value
            if (user != null) {
                user.name = newName
                supabaseUserDao.updateUser(user)
                userLiveData.value = user
            }
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            val user = userLiveData.value
            if (user != null) {
                user.username = newUsername
                supabaseUserDao.updateUser(user)
                userLiveData.value = user
            }
        }
    }

    fun updateUserAvatar(newAvatarUrl: String) {
        viewModelScope.launch {
            val user = userLiveData.value
            if (user != null) {
                user.avatar = newAvatarUrl
                supabaseUserDao.updateUser(user)
                userLiveData.value = user
            }
        }
    }

    private fun parseData(jsonString: String?): List<User>? {
        return try {
            val gson = Gson()
            val userType = object : TypeToken<List<User>>() {}.type
            gson.fromJson(jsonString, userType)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUserId(): String {
        return userLiveData.value?.user_id ?: ""
    }
}