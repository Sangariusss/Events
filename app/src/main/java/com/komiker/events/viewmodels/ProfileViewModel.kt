package com.komiker.events.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.Event
import com.komiker.events.data.database.models.EventResponse
import com.komiker.events.data.database.models.Proposal
import com.komiker.events.data.database.models.User
import kotlinx.coroutines.launch

class ProfileViewModel(private val supabaseUserDao: SupabaseUserDao) : ViewModel() {

    val userLiveData = MutableLiveData<User?>()
    val proposalAuthorLiveData = MutableLiveData<User?>()
    private val eventLiveData = MutableLiveData<Event?>()
    private val proposalLiveData = MutableLiveData<Proposal?>()
    private val _likedEvents = MutableLiveData<List<Event>>()
    val likedEvents: LiveData<List<Event>> = _likedEvents
    private val _myEventsResponse = MutableLiveData<List<EventResponse>>()
    val myEventsResponse: LiveData<List<EventResponse>> = _myEventsResponse

    private fun loadUserInternal(userId: String, liveData: MutableLiveData<User?>) {
        viewModelScope.launch {
            try {
                val result = supabaseUserDao.getUserById(userId)
                val dataList = parseData(result.data)
                liveData.value = dataList?.firstOrNull()
            } catch (e: Exception) {
                liveData.value = null
                e.printStackTrace()
            }
        }
    }

    fun loadUser(userId: String) = loadUserInternal(userId, userLiveData)
    fun loadProposalAuthor(userId: String) = loadUserInternal(userId, proposalAuthorLiveData)

    fun loadEventById(eventId: String): LiveData<Event?> {
        viewModelScope.launch {
            try {
                val event = supabaseUserDao.getEventById(eventId)
                eventLiveData.postValue(event)
            } catch (e: Exception) {
                eventLiveData.postValue(null)
                e.printStackTrace()
            }
        }
        return eventLiveData
    }

    fun loadProposalById(proposalId: String): LiveData<Proposal?> {
        viewModelScope.launch {
            try {
                val proposal = supabaseUserDao.getProposalById(proposalId)
                proposalLiveData.postValue(proposal)
            } catch (e: Exception) {
                proposalLiveData.postValue(null)
                e.printStackTrace()
            }
        }
        return proposalLiveData
    }

    fun loadMyEvents(authorId: String) {
        viewModelScope.launch {
            try {
                val response = supabaseUserDao.getEventsByAuthor(authorId, authorId)
                _myEventsResponse.postValue(response)
            } catch (e: Exception) {
                e.printStackTrace()
                _myEventsResponse.postValue(emptyList())
            }
        }
    }

    fun loadLikedEvents(userId: String) {
        viewModelScope.launch {
            try {
                val response = supabaseUserDao.getLikedEvents(userId)
                val events = response.map { eventResponse ->
                    Event(
                        id = eventResponse.id!!,
                        userId = eventResponse.userId,
                        username = eventResponse.username,
                        userAvatar = eventResponse.userAvatar,
                        title = eventResponse.title,
                        description = eventResponse.description,
                        startDate = eventResponse.startDate,
                        endDate = eventResponse.endDate,
                        eventTime = eventResponse.eventTime,
                        tags = eventResponse.tags,
                        location = eventResponse.location,
                        images = eventResponse.images,
                        createdAt = eventResponse.createdAt,
                        likesCount = eventResponse.likesCount,
                        viewsCount = eventResponse.viewsCount
                    )
                }
                _likedEvents.postValue(events)
            } catch (e: Exception) {
                e.printStackTrace()
                _likedEvents.postValue(emptyList())
            }
        }
    }

    fun removeEventFromFavorites(eventId: String) {
        val currentList = _likedEvents.value?.toMutableList() ?: return
        currentList.removeAll { it.id == eventId }
        _likedEvents.value = currentList
    }

    fun likeProposal(proposalId: String, userId: String, callback: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                supabaseUserDao.insertProposalLike(proposalId, userId)
                val likesCount = supabaseUserDao.getProposalLikesCount(proposalId)
                callback(true, likesCount)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false, 0)
            }
        }
    }

    fun unlikeProposal(proposalId: String, userId: String, callback: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                supabaseUserDao.deleteProposalLike(proposalId, userId)
                val likesCount = supabaseUserDao.getProposalLikesCount(proposalId)
                callback(true, likesCount)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false, 0)
            }
        }
    }

    fun likeEvent(eventId: String, userId: String, callback: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                supabaseUserDao.insertEventLike(eventId, userId)
                val likesCount = supabaseUserDao.getEventLikesCount(eventId)
                callback(true, likesCount)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false, 0)
            }
        }
    }

    fun unlikeEvent(eventId: String, userId: String, callback: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                supabaseUserDao.deleteEventLike(eventId, userId)
                val likesCount = supabaseUserDao.getEventLikesCount(eventId)
                callback(true, likesCount)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false, 0)
            }
        }
    }

    suspend fun isProposalLiked(proposalId: String, userId: String): Boolean {
        return supabaseUserDao.isProposalLiked(proposalId, userId)
    }

    suspend fun isEventLiked(eventId: String, userId: String): Boolean {
        return supabaseUserDao.isEventLiked(eventId, userId)
    }

    private fun updateUserField(updateAction: User.() -> Unit) {
        viewModelScope.launch {
            val user = userLiveData.value
            if (user != null) {
                user.updateAction()
                supabaseUserDao.updateUser(user)
                userLiveData.value = user
            }
        }
    }

    fun updateName(newName: String) = updateUserField { name = newName }
    fun updateUsername(newUsername: String) = updateUserField { username = newUsername }
    fun updateUserAvatar(newAvatarUrl: String) = updateUserField { avatar = newAvatarUrl }

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

    private fun parseData(jsonString: String?): List<User>? = try {
        Gson().fromJson(jsonString, object : TypeToken<List<User>>() {}.type)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    fun getUserId(): String = userLiveData.value?.user_id ?: ""
}