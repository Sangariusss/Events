package com.komiker.events.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.Event
import com.komiker.events.databinding.FragmentEventDetailBinding
import com.komiker.events.glide.CircleCropTransformation
import com.komiker.events.ui.adapters.EventDetailPagerAdapter
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.gotrue.auth
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private val supabaseUserDao = SupabaseUserDao(SupabaseClientProvider.client)
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }
    private var currentEvent: Event? = null
    private var sourceFragmentTag: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSystemBars()
        handleArguments()
        initButtonBack()
        setupCustomOnBackPressed()
        setupShareButton()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupSystemBars() {
        requireActivity().window.navigationBarColor =
            ContextCompat.getColor(requireContext(), R.color.neutral_100)
    }

    private fun handleArguments() {
        val event = arguments?.let { BundleCompat.getParcelable(it, "event", Event::class.java) }
        val eventId = arguments?.getString("eventId")
        sourceFragmentTag = arguments?.getString("sourceFragment")

        if (event != null) {
            setupUI(event)
        } else if (!eventId.isNullOrEmpty()) {
            loadEventById(eventId)
        } else {
            findNavController().popBackStack()
        }
    }

    private fun loadEventById(eventId: String) {
        profileViewModel.loadEventById(eventId).observe(viewLifecycleOwner) { event ->
            if (event != null) {
                setupUI(event)
            } else {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun setupUI(event: Event) {
        currentEvent = event
        setTextFields(event)
        loadUserAvatar(event)
        setupTabsAndViewPager()
    }

    private fun setTextFields(event: Event) = with(binding) {
        textUserName.text = event.username
        textTime.text = formatTimeAgo(event.createdAt)
    }

    private fun loadUserAvatar(event: Event) {
        Glide.with(this)
            .load(event.userAvatar)
            .placeholder(R.drawable.img_profile_placeholder)
            .transform(CircleCropTransformation())
            .into(binding.imageProfile)
    }

    private fun setupTabsAndViewPager() {
        currentEvent?.let { event ->
            val adapter = EventDetailPagerAdapter(requireActivity(), event)
            binding.viewPager.adapter = adapter
            binding.viewPager.offscreenPageLimit = 3
            binding.viewPager.isUserInputEnabled = false

            updateTabSelection(0)

            binding.tabDescription.setOnClickListener {
                binding.viewPager.currentItem = 0
            }
            binding.tabImages.setOnClickListener {
                binding.viewPager.currentItem = 1
            }
            binding.tabLocation.setOnClickListener {
                binding.viewPager.currentItem = 2
            }

            binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateTabSelection(position)
                }
            })
        }
    }

    private fun updateTabSelection(position: Int) {
        binding.tabDescription.isSelected = position == 0
        binding.tabImages.isSelected = position == 1
        binding.tabLocation.isSelected = position == 2

        val selectedColor = ContextCompat.getColor(requireContext(), R.color.neutral_0)
        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.neutral_35)

        binding.tabDescription.setTextColor(if (position == 0) selectedColor else unselectedColor)
        binding.tabImages.setTextColor(if (position == 1) selectedColor else unselectedColor)
        binding.tabLocation.setTextColor(if (position == 2) selectedColor else unselectedColor)
    }

    private fun initButtonBack() {
        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupCustomOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, true) {
            val navController = findNavController()
            val mainTabs = listOf("home", "favorites", "proposals", "profile")

            if (sourceFragmentTag in mainTabs) {
                val bundle = Bundle().apply {
                    putString("navigateTo", sourceFragmentTag)
                }
                navController.navigate(R.id.action_EventDetailFragment_to_MainMenuFragment, bundle)
            }
            else if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            }
            else {
                val isAuthenticated = SupabaseClientProvider.client.auth.currentSessionOrNull() != null
                val navOptionsToRoot = navOptions {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
                if (isAuthenticated) {
                    navController.navigate(R.id.MainMenuFragment, null, navOptionsToRoot)
                } else {
                    navController.navigate(R.id.WelcomeFragment, null, navOptionsToRoot)
                }
            }
        }
    }

    private fun formatTimeAgo(createdAt: OffsetDateTime?) = createdAt?.let {
        val now = OffsetDateTime.now()
        when {
            ChronoUnit.MINUTES.between(it, now) < 1 -> "now"
            ChronoUnit.MINUTES.between(it, now) < 60 -> "${ChronoUnit.MINUTES.between(it, now)}m"
            ChronoUnit.HOURS.between(it, now) < 24 -> "${ChronoUnit.HOURS.between(it, now)}h"
            ChronoUnit.DAYS.between(it, now) < 7 -> "${ChronoUnit.DAYS.between(it, now)}d"
            else -> it.format(DateTimeFormatter.ofPattern("MM/dd"))
        }
    } ?: "Unknown"

    private fun setupShareButton() {
        binding.buttonShare.setOnClickListener {
            currentEvent?.let { event ->
                shareEvent(event)
            } ?: run {
                Toast.makeText(requireContext(), "Event data not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareEvent(event: Event) {
        val username = event.username.replace(" ", "_")
        val deepLink = "https://excito.netlify.app/@$username/event/${event.id}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, deepLink)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }
}