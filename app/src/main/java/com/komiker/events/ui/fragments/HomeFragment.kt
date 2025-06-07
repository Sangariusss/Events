package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.komiker.events.data.database.models.EventResponse
import com.komiker.events.databinding.FragmentHomeBinding
import com.komiker.events.ui.adapters.EventsAdapter
import com.komiker.events.viewmodels.CreateEventViewModel
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }
    private val createEventViewModel: CreateEventViewModel by activityViewModels()
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var channel: RealtimeChannel
    private val likeCache = mutableMapOf<String, Boolean>()
    private val likesCountCache = mutableMapOf<String, Int>()
    private var heartbeatJob: Job? = null
    private val TAG = "HomeFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchField()
        setupButtonFilter()
        setupRecyclerView()
        setupRealtimeUpdates()
        startHeartbeat()
        observeFilters()
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

    private fun observeFilters() {
        createEventViewModel.filtersApplied.observe(viewLifecycleOwner) { applied ->
            if (applied) {
                profileViewModel.userLiveData.value?.user_id?.let { userId ->
                    val query = binding.editTextFindEvents.text.toString()
                    if (query.isEmpty()) {
                        loadEvents(userId)
                    } else {
                        filterEvents(userId, query)
                    }
                }
                createEventViewModel.resetFiltersApplied()
            }
        }
    }

    private fun setupSearchField() {
        val emptyDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_empty)
        val filledDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_filled)
        var searchJob: Job? = null
        binding.editTextFindEvents.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.editTextFindEvents.background = if (s.isNullOrEmpty()) emptyDrawable else filledDrawable
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    val userId = profileViewModel.userLiveData.value?.user_id ?: return@launch
                    if (s.isNullOrEmpty()) loadEvents(userId) else filterEvents(userId, s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupButtonFilter() {
        binding.buttonFilter.setOnClickListener {
            val bundle = Bundle().apply { putInt("sourceFragmentId", R.id.HomeFragment) }
            findNavController().navigate(R.id.action_MainMenuFragment_to_FilterFragment, bundle)
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
            if (user != null) {
                loadEvents(user.user_id)
            }
        }
    }

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    private fun calculateMatchScore(text: String, query: String): Int {
        val normalizedText = normalizeText(text)
        val normalizedQuery = normalizeText(query)
        return when {
            normalizedText == normalizedQuery -> 3
            normalizedText.startsWith(normalizedQuery) -> 2
            normalizedText.contains(normalizedQuery) -> 1
            else -> 0
        }
    }

    private fun applyFilters(events: List<EventResponse>): List<EventResponse> {
        var filteredEvents = events

        val selectedYear = createEventViewModel.selectedYear
        val selectedMonth = createEventViewModel.selectedMonth
        val selectedDay = createEventViewModel.selectedDay
        val location = createEventViewModel.location.value
        val tags = createEventViewModel.tags.value

        if (selectedYear != null && selectedMonth != null && selectedDay != null) {
            val filterDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            filteredEvents = filteredEvents.filter { event ->
                val startDateStr = event.startDate
                if (startDateStr.isNullOrEmpty()) {
                    false
                } else {
                    try {
                        val startDate = LocalDate.parse(startDateStr, dateFormatter)
                        startDate.isEqual(filterDate)
                    } catch (e: Exception) {
                        false
                    }
                }
            }
        }

        location?.let { loc ->
            if (loc.isNotEmpty()) {
                filteredEvents = filteredEvents.filter { event ->
                    event.location?.contains(loc, ignoreCase = true) == true
                }
            }
        }

        tags?.let { tagList ->
            if (tagList.isNotEmpty()) {
                filteredEvents = filteredEvents.filter { event ->
                    val eventTags = event.tags
                    eventTags?.any { tag -> tagList.contains(tag) } == true
                }
            }
        }

        return filteredEvents
    }

    private fun loadEvents(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = supabaseClient.postgrest.rpc(
                    "get_events_with_likes",
                    mapOf("user_id_input" to userId)
                ).decodeList<EventResponse>()

                val filteredResponse = applyFilters(response)

                val events = filteredResponse.map { eventResponse ->
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
                    ).also {
                        likeCache[eventResponse.id] = eventResponse.isLiked ?: false
                        likesCountCache[eventResponse.id] = eventResponse.likesCount
                    }
                }.sortedByDescending { it.createdAt }
                eventsAdapter.submitList(events)
            } catch (e: Exception) {
              Log.e(TAG, "Error loading events: ${e.message}")
            }
        }
    }

    private fun filterEvents(userId: String, query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = supabaseClient.postgrest.rpc(
                    "get_events_with_likes",
                    mapOf("user_id_input" to userId)
                ).decodeList<EventResponse>()

                var filteredResponse = applyFilters(response)

                filteredResponse = filteredResponse
                    .asSequence()
                    .map { eventResponse ->
                        val titleScore = calculateMatchScore(eventResponse.title, query)
                        val descriptionScore = calculateMatchScore(eventResponse.description ?: "", query)
                        val usernameScore = calculateMatchScore(eventResponse.username, query)
                        Pair(
                            eventResponse,
                            maxOf(titleScore, descriptionScore, usernameScore)
                        )
                    }
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }
                    .map { it.first }
                    .toList()

                val filteredEvents = filteredResponse.map { eventResponse ->
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
                    ).also {
                        likeCache[eventResponse.id] = eventResponse.isLiked ?: false
                        likesCountCache[eventResponse.id] = eventResponse.likesCount
                    }
                }.sortedByDescending { it.createdAt }
                eventsAdapter.submitList(filteredEvents)
            } catch (e: Exception) {
               Log.e(TAG, "Error filtering events: ${e.message}")
            }
        }
    }

    private fun handleLike(eventId: String, isLiked: Boolean, callback: (Boolean, Int) -> Unit) {
        val userId = profileViewModel.userLiveData.value?.user_id ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (isLiked) {
                    profileViewModel.likeEvent(eventId, userId, callback)
                } else {
                    profileViewModel.unlikeEvent(eventId, userId, callback)
                }
                likeCache[eventId] = isLiked
            } catch (e: Exception) {
                Log.e(TAG, "Error handling like: ${e.message}")
                callback(false, likesCountCache[eventId] ?: 0)
            }
        }
    }

    private fun deleteEvent(event: Event) {
        viewLifecycleOwner.lifecycleScope.launch {
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
                Log.e(TAG, "Error deleting event: ${e.message}")
            }
        }
    }

    private fun setupRealtimeUpdates() {
        channel = supabaseClient.channel("events-channel")
        val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "events" }
        viewLifecycleOwner.lifecycleScope.launch {
            changeFlow.collect { change ->
                try {
                    val eventResponse = change.decodeRecord<EventResponse>()
                    val newEventResponse = listOf(eventResponse)
                    val filteredResponse = applyFilters(newEventResponse)

                    if (filteredResponse.isNotEmpty()) {
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
                        val userId = profileViewModel.userLiveData.value?.user_id ?: return@collect
                        val isLiked = profileViewModel.isEventLiked(newEvent.id, userId)
                        likeCache[newEvent.id] = isLiked
                        likesCountCache[newEvent.id] = newEvent.likesCount
                        val currentList = eventsAdapter.currentList.toMutableList()
                        val query = binding.editTextFindEvents.text.toString()
                        val matchesQuery = query.isEmpty() ||
                                normalizeText(newEvent.title!!).contains(normalizeText(query)) ||
                                normalizeText(newEvent.description!!).contains(normalizeText(query)) ||
                                normalizeText(newEvent.username).contains(normalizeText(query))
                        if (matchesQuery) {
                            currentList.add(0, newEvent)
                            eventsAdapter.submitList(currentList.sortedByDescending { it.createdAt })
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing realtime update: ${e.message}")
                }
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
                if (currentList.isEmpty()) {
                    return@withContext
                }
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
                Log.e(TAG, "Error updating likes count: ${e.message}")
            }
        }
    }
}