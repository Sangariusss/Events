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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        CoroutineScope(Dispatchers.IO).launch {
            supabaseClient.realtime.removeChannel(channel)
            withContext(Dispatchers.Main) {
                _binding = null
            }
        }
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
                onDeleteClicked = { event ->
                    deleteEvent(event)
                },
                navController = findNavController()
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
                        id = eventResponse.id,
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
                eventsAdapter.submitList(events)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun deleteEvent(event: Event) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabaseClient.from("events").delete {
                    filter { eq("id", event.id!!) }
                }
                withContext(Dispatchers.Main) {
                    val currentList = eventsAdapter.currentList.toMutableList()
                    currentList.remove(event)
                    eventsAdapter.submitList(currentList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupRealtimeUpdates() {
        channel = supabaseClient.channel("events-channel")

        val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "events"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            changeFlow.collect { change ->
                println("Received Realtime update: $change")
                val eventResponse = change.decodeRecord<EventResponse>()
                val newEvent = Event(
                    id = eventResponse.id,
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
                eventsAdapter.submitList(currentList)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            channel.subscribe()
        }
    }
}