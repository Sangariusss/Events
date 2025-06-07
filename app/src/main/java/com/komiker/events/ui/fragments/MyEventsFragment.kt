package com.komiker.events.ui.fragments

import android.os.Bundle
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
import com.komiker.events.databinding.FragmentMyEventsBinding
import com.komiker.events.ui.adapters.EventsAdapter
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyEventsFragment : Fragment() {

    private var _binding: FragmentMyEventsBinding? = null
    private val binding get() = _binding!!

    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }
    private lateinit var eventsAdapter: EventsAdapter

    private val likeCache = mutableMapOf<String, Boolean>()
    private val likesCountCache = mutableMapOf<String, Int>()
    private val viewsCountCache = mutableMapOf<String, Int>()
    private val TAG = "MyEventsFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSystemBars()
        initButtonBack()
        setupRecyclerView()
        observeMyEvents()
        loadData()
    }

    private fun setupSystemBars() {
        requireActivity().window.apply {
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        }
    }

    private fun initButtonBack() {
        binding.buttonBack.setOnClickListener {
            navigateToMainMenuWithProfile()
        }
    }

    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapter(
            currentUserId = profileViewModel.getUserId(),
            onDeleteClicked = { event -> deleteEvent(event) },
            onItemClicked = { event ->
                val bundle = Bundle().apply {
                    putParcelable("event", event)
                    putBoolean("isLiked", likeCache[event.id] ?: false)
                    putInt("likesCount", likesCountCache[event.id] ?: event.likesCount)
                    putInt("viewsCount", viewsCountCache[event.id] ?: event.viewsCount)
                    putString("sourceFragment", "my_events")
                }
                findNavController().navigate(R.id.action_MyEventsFragment_to_EventDetailFragment, bundle)
            },
            likeCache = likeCache,
            likesCountCache = likesCountCache,
            onLikeClicked = ::handleLike
        )
        binding.recyclerViewMyEvents.adapter = eventsAdapter
        binding.recyclerViewMyEvents.layoutManager = LinearLayoutManager(context)
    }

    private fun observeMyEvents() {
        profileViewModel.myEventsResponse.observe(viewLifecycleOwner) { eventResponses ->
            binding.progressBarMyEvents.visibility = View.GONE
            if (eventResponses.isNullOrEmpty()) {
                binding.textEmptyMyEvents.visibility = View.VISIBLE
                binding.recyclerViewMyEvents.visibility = View.GONE
            } else {
                binding.textEmptyMyEvents.visibility = View.GONE
                binding.recyclerViewMyEvents.visibility = View.VISIBLE

                val events = eventResponses.map { eventResponse ->
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
                    ).also {
                        likeCache[eventResponse.id] = eventResponse.isLiked ?: false
                        likesCountCache[eventResponse.id] = eventResponse.likesCount
                        viewsCountCache[eventResponse.id] = eventResponse.viewsCount
                    }
                }
                eventsAdapter.submitList(events)
            }
        }
    }

    private fun handleLike(eventId: String, isLiked: Boolean, callback: (Boolean, Int) -> Unit) {
        val userId = profileViewModel.getUserId()
        if (userId.isEmpty()) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (isLiked) {
                    profileViewModel.likeEvent(eventId, userId, callback)
                } else {
                    profileViewModel.unlikeEvent(eventId, userId, callback)
                }
                likeCache[eventId] = isLiked
            } catch (e: Exception) {
                Log.e(TAG, "Error handling like: ${e.message}", e)
                callback(false, likesCountCache[eventId] ?: 0)
            }
        }
    }

    private fun loadData() {
        binding.progressBarMyEvents.visibility = View.VISIBLE
        binding.textEmptyMyEvents.visibility = View.GONE
        binding.recyclerViewMyEvents.visibility = View.GONE

        profileViewModel.getUserId().let { userId ->
            if (userId.isNotEmpty()) {
                profileViewModel.loadMyEvents(userId)
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

    private fun navigateToMainMenuWithProfile() {
        findNavController().navigate(
            R.id.action_MyEventsFragment_to_MainMenuFragment,
            Bundle().apply {
                putString("navigateTo", "profile")
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}