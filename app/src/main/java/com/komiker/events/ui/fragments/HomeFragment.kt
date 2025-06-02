package com.komiker.events.ui.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.Event
import com.komiker.events.data.database.models.EventLike
import com.komiker.events.data.database.models.EventResponse
import com.komiker.events.databinding.FragmentHomeBinding
import com.komiker.events.ui.adapters.EventsAdapter
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var channel: RealtimeChannel
    private val likeCache = mutableMapOf<String, Boolean>()
    private val likesCountCache = mutableMapOf<String, Int>()
    private var heartbeatJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEditTextBackgroundChange()
        setupButtonFilter()
        setupRecyclerView()
        loadEvents()
        setupRealtimeUpdates()
        startHeartbeat()
    }

    override fun onDestroyView() {
        heartbeatJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            supabaseClient.realtime.removeChannel(channel)
            withContext(Dispatchers.Main) {
                _binding = null
            }
        }
        super.onDestroyView()
    }

    private fun setupEditTextBackgroundChange() {
        val editText = binding.editTextFindEvents
        val emptyDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_empty)
        val filledDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_filled)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    editText.background = emptyDrawable
                } else {
                    editText.background = filledDrawable
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupButtonFilter() {
        binding.buttonFilter.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_FilterFragment)
        }
    }

    private fun setupRecyclerView() {
        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            val currentUserId = user?.user_id
            eventsAdapter = EventsAdapter(
                currentUserId = currentUserId,
                onDeleteClicked = { event -> deleteEvent(event) },
                navController = findNavController(),
                likeCache = likeCache,
                likesCountCache = likesCountCache,
                onLikeClicked = ::handleLike
            )
            binding.recyclerViewEvents.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = eventsAdapter
            }
            loadEvents()
        }
    }

    private fun loadEvents() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = supabaseClient.from("events").select().decodeList<EventResponse>()
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
                        likesCount = eventResponse.likesCount
                    )
                }.sortedByDescending { it.createdAt }
                initializeCaches(events)
                eventsAdapter.submitList(events)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun initializeCaches(events: List<Event>) {
        val currentUserId = profileViewModel.userLiveData.value?.user_id ?: return
        events.forEach { event ->
            if (!likeCache.containsKey(event.id)) {
                val isLiked = supabaseClient.from("event_likes")
                    .select { filter { eq("event_id", event.id); eq("user_id", currentUserId) } }
                    .decodeList<EventLike>()
                    .isNotEmpty()
                likeCache[event.id] = isLiked
            }
            likesCountCache[event.id] = event.likesCount
        }
    }

    private fun handleLike(eventId: String, isLiked: Boolean, callback: (Boolean, Int) -> Unit) {
        val userId = profileViewModel.userLiveData.value?.user_id ?: return
        if (isLiked) {
            profileViewModel.likeEvent(eventId, userId, callback)
        } else {
            profileViewModel.unlikeEvent(eventId, userId, callback)
        }
    }

    private fun deleteEvent(event: Event) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabaseClient.from("events").delete {
                    filter { eq("id", event.id) }
                }
                withContext(Dispatchers.Main) {
                    val currentList = eventsAdapter.currentList.toMutableList()
                    currentList.remove(event)
                    likeCache.remove(event.id)
                    likesCountCache.remove(event.id)
                    eventsAdapter.submitList(currentList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupRealtimeUpdates() {
        channel = supabaseClient.channel("events-channel")
        val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "events" }
        viewLifecycleOwner.lifecycleScope.launch {
            changeFlow.collect { change ->
                val eventResponse = change.decodeRecord<EventResponse>()
                val newEvent = Event(
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
                    likesCount = eventResponse.likesCount
                )
                val currentList = eventsAdapter.currentList.toMutableList()
                currentList.add(0, newEvent)
                initializeCaches(listOf(newEvent))
                eventsAdapter.submitList(currentList)
            }
        }
        CoroutineScope(Dispatchers.IO).launch { channel.subscribe() }
    }

    private fun startHeartbeat() {
        heartbeatJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                delay(15000)
                updateLikesCountForAllEvents()
            }
        }
    }

    private suspend fun updateLikesCountForAllEvents() {
        withContext(Dispatchers.Main) {
            try {
                val currentList = eventsAdapter.currentList
                if (currentList.isEmpty()) return@withContext

                val eventIds = currentList.map { it.id }
                val updatedEvents = supabaseClient.from("events")
                    .select { filter { isIn("id", eventIds) } }
                    .decodeList<Event>()

                updatedEvents.forEach { updatedEvent ->
                    val currentLikesCount = likesCountCache[updatedEvent.id] ?: return@forEach
                    if (currentLikesCount != updatedEvent.likesCount) {
                        likesCountCache[updatedEvent.id] = updatedEvent.likesCount
                        val index = currentList.indexOfFirst { it.id == updatedEvent.id }
                        if (index != -1) {
                            eventsAdapter.notifyItemChanged(index, updatedEvent.likesCount)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}