package com.komiker.events.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.User
import kotlinx.coroutines.launch

class ProfileViewModel(private val supabaseUserDao: SupabaseUserDao) : ViewModel() {

    val userLiveData = MutableLiveData<User?>()
    val proposalAuthorLiveData = MutableLiveData<User?>()
    val eventAuthorLiveData = MutableLiveData<User?>()

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

    fun loadProposalAuthor(userId: String) {
        viewModelScope.launch {
            try {
                val result = supabaseUserDao.getUserById(userId)
                val dataList = parseData(result.data)

                proposalAuthorLiveData.value = if (!dataList.isNullOrEmpty()) dataList[0] else null
            } catch (e: Exception) {
                proposalAuthorLiveData.value = null
                e.printStackTrace()
            }
        }
    }

    fun loadEventAuthor(userId: String) {
        viewModelScope.launch {
            try {
                val result = supabaseUserDao.getUserById(userId)
                val dataList = parseData(result.data)

                eventAuthorLiveData.value = if (!dataList.isNullOrEmpty()) dataList[0] else null
            } catch (e: Exception) {
                eventAuthorLiveData.value = null
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

    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            val user = userLiveData.value
            if (user != null && user.user_id.isNotEmpty()) {
                try {
                    supabaseUserDao.updateEmail(user.user_id, newEmail)
                    user.email = newEmail
                    userLiveData.value = user
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateSocialLinks(telegramLink: String?, instagramLink: String?) {
        viewModelScope.launch {
            val userId = userLiveData.value?.user_id ?: return@launch
            supabaseUserDao.updateUserSocialLinks(userId, telegramLink, instagramLink)
            loadUser(userId)
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