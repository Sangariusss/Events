package com.komiker.events.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.databinding.FragmentFavoritesBinding
import com.komiker.events.ui.adapters.EventsAdapter
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(SupabaseUserDao(SupabaseClientProvider.client))
    }
    private lateinit var eventsAdapter: EventsAdapter

    private val likeCache = mutableMapOf<String, Boolean>()
    private val likesCountCache = mutableMapOf<String, Int>()
    private val viewsCountCache = mutableMapOf<String, Int>()
    private val TAG = "FavoritesFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeLikedEvents()
        loadData()
    }

    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapter(
            currentUserId = profileViewModel.userLiveData.value?.user_id,
            onDeleteClicked = { event ->
                val currentList = eventsAdapter.currentList.toMutableList()
                currentList.remove(event)
                eventsAdapter.submitList(currentList)
                handleLike(event.id, false) { _, _ -> }
            },
            likeCache = likeCache,
            likesCountCache = likesCountCache,
            onLikeClicked = ::handleLike,
            onItemClicked = { event ->
                val bundle = Bundle().apply {
                    putParcelable("event", event)
                    putBoolean("isLiked", likeCache[event.id] ?: false)
                    putInt("likesCount", likesCountCache[event.id] ?: event.likesCount)
                    putInt("viewsCount", viewsCountCache[event.id] ?: event.viewsCount)
                    putString("sourceFragment", "favorites")
                }
                findNavController().navigate(R.id.action_MainMenuFragment_to_EventDetailFragment, bundle)
            }
        )
        binding.recyclerViewFavorites.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventsAdapter
        }
    }

    private fun observeLikedEvents() {
        profileViewModel.likedEvents.observe(viewLifecycleOwner) { events ->
            binding.progressBarFavorites.visibility = View.GONE

            if (events.isNullOrEmpty()) {
                binding.textEmptyFavorites.visibility = View.VISIBLE
                binding.recyclerViewFavorites.visibility = View.GONE
            } else {
                binding.textEmptyFavorites.visibility = View.GONE
                binding.recyclerViewFavorites.visibility = View.VISIBLE

                events.forEach { event ->
                    likeCache[event.id] = true
                    likesCountCache[event.id] = event.likesCount
                    viewsCountCache[event.id] = event.viewsCount
                }
                eventsAdapter.submitList(events)
            }
        }
    }

    private fun handleLike(eventId: String, isLiked: Boolean, callback: (Boolean, Int) -> Unit) {
        val userId = profileViewModel.getUserId()
        if (userId.isEmpty()) {
            callback(false, likesCountCache[eventId] ?: 0)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (isLiked) {
                    profileViewModel.likeEvent(eventId, userId, callback)
                } else {
                    profileViewModel.unlikeEvent(eventId, userId, callback)

                    profileViewModel.removeEventFromFavorites(eventId)
                }
                likeCache[eventId] = isLiked
            } catch (e: Exception) {
                Log.e(TAG, "Error handling like: ${e.message}")
                callback(false, likesCountCache[eventId] ?: 0)
            }
        }
    }

    private fun loadData() {
        binding.progressBarFavorites.visibility = View.VISIBLE
        binding.textEmptyFavorites.visibility = View.GONE
        binding.recyclerViewFavorites.visibility = View.GONE

        profileViewModel.userLiveData.value?.user_id?.let { userId ->
            profileViewModel.loadLikedEvents(userId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}