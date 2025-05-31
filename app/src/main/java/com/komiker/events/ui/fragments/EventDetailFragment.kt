package com.komiker.events.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.bumptech.glide.Glide
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.Event
import com.komiker.events.databinding.FragmentEventDetailBinding
import com.komiker.events.glide.CircleCropTransformation
import com.komiker.events.ui.adapters.EventImageAdapter
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private val supabaseClientProvider = SupabaseClientProvider
    private val supabaseUserDao = SupabaseUserDao(supabaseClientProvider.client)
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }
    private var currentEvent: Event? = null

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
        setupButtonCheckLocation()
        setupCopyAddressButton()
        setupShareButton()
    }

    override fun onStop() {
        super.onStop()
        _binding?.shimmerLayout?.stopShimmer()
        _binding?.shimmerLayout?.visibility = View.GONE
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
        val event = BundleCompat.getParcelable(requireArguments(), "event", Event::class.java)
        val eventId = arguments?.getString("eventId")

        if (event != null) {
            currentEvent = event
            viewLifecycleOwner.lifecycleScope.launch { setupUI(event) }
        } else if (eventId != null) {
            loadEventById(eventId)
        }
    }

    private fun loadEventById(eventId: String) {
        profileViewModel.loadEventById(eventId).observe(viewLifecycleOwner) { event ->
            event?.let {
                currentEvent = it
                viewLifecycleOwner.lifecycleScope.launch {
                    setupUI(it)
                }
            } ?: run {
                if (isAdded) {
                    findNavController().navigate(R.id.MainMenuFragment)
                }
            }
        }
    }

    private suspend fun setupUI(event: Event) {
        setTextFields(event)
        styleStatusText()
        loadUserAvatar(event)
        setupImagePager(event)
    }

    private fun setTextFields(event: Event) = with(binding) {
        textUserName.text = event.username
        textTime.text = formatTimeAgo(event.createdAt)
        textTitle.text = event.title
        textContent.text = event.description
        titleStartDateContent.text = getString(R.string.event_date_range, event.startDate, event.endDate)
        titleStartDate.text = getString(R.string.start_time_format, event.eventTime ?: "Not specified")
        titleAddressContent.text = event.location ?: "Not specified"
        titleTagsContent.text = event.tags?.joinToString(", ") ?: "No tags"
    }

    private fun styleStatusText() {
        val statusText = getString(R.string.status_active)
        val spannable = SpannableString(statusText)
        statusText.indexOf("Active").takeIf { it != -1 }?.let { start ->
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.green_60)),
                start,
                start + "Active".length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.titleStatus.text = spannable
    }

    private fun loadUserAvatar(event: Event) {
        Glide.with(this)
            .load(event.userAvatar)
            .placeholder(R.drawable.img_profile_placeholder)
            .transform(CircleCropTransformation())
            .into(binding.imageProfile)
    }

    private suspend fun setupImagePager(event: Event) {
        if (!isAdded || event.images.isNullOrEmpty()) {
            return
        }

        val urls = withContext(Dispatchers.IO) {
            event.images.map {
                supabaseClientProvider.client.storage
                    .from("event-images")
                    .createSignedUrl(it, 60.seconds)
            }
        }

        if (!isAdded || _binding == null) return

        binding.apply {
            imageEventPager.visibility = View.VISIBLE
            shimmerLayout.visibility = View.VISIBLE
            shimmerLayout.startShimmer()

            val radius = (20 * resources.displayMetrics.density).roundToInt()
            listOf(imageEventPager, shimmerLayout).forEach { view ->
                view.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radius.toFloat())
                    }
                }
                view.clipToOutline = true
            }

            imageEventPager.adapter = EventImageAdapter(urls) {
                if (isAdded && _binding != null) {
                    shimmerLayout.post {
                        shimmerLayout.stopShimmer()
                        shimmerLayout.visibility = View.GONE
                    }
                }
            }
            imageEventPager.offscreenPageLimit = urls.size
        }
    }

    private fun initButtonBack() {
        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupCustomOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, true) {
            val isAuthenticated = SupabaseClientProvider.client.auth.currentSessionOrNull() != null
            val navController = findNavController()

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

    private fun setupButtonCheckLocation() {
        binding.buttonCheckLocation.setOnClickListener {
            openLocationInMaps(binding.titleAddressContent.text.toString())
        }
    }

    private fun setupCopyAddressButton() {
        binding.buttonCopyAddress.setOnClickListener {
            val address = binding.titleAddressContent.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Address", address)
            clipboard.setPrimaryClip(clip)
        }
    }

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

    private fun openLocationInMaps(address: String) {
        if (address.isBlank() || address == "Not specified") return

        val uri = "geo:0,0?q=${Uri.encode(address)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            val fallbackUri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
            if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                Toast.makeText(requireContext(), "No map application found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}