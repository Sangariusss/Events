package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.models.Event
import com.komiker.events.databinding.FragmentEventDetailBinding
import com.komiker.events.glide.CircleCropTransformation
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.komiker.events.ui.adapters.EventImageAdapter

class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!

    private val supabaseClientProvider = SupabaseClientProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val event: Event? = BundleCompat.getParcelable(requireArguments(), "event", Event::class.java)
        if (event != null) {
            setupSystemBars()
            setupUI(event)
            initButtonBack()
            setupOnBackPressedCallback()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSystemBars() {
        requireActivity().window.apply {
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        }
    }

    private fun setupUI(event: Event) {
        setTextFields(event)
        styleStatusText()
        loadUserAvatar(event)
        setupImagePager(event)
    }

    private fun setTextFields(event: Event) {
        binding.textUserName.text = event.username
        binding.textTime.text = formatTimeAgo(event.createdAt)
        binding.textTitle.text = event.title
        binding.textContent.text = event.description
        binding.titleStartDateContent.text = getString(R.string.event_date_range, event.startDate, event.endDate)
        binding.titleStartDate.text = getString(R.string.start_time_format, event.eventTime ?: "Not specified")
        binding.titleAddressContent.text = event.location ?: "Not specified"
        binding.titleTagsContent.text = event.tags?.joinToString(", ") ?: "No tags"
        binding.textLikesCount.text = event.likesCount.toString()
    }

    private fun styleStatusText() {
        val statusText = getString(R.string.status_active)
        val spannableString = SpannableString(statusText)
        val activeStartIndex = statusText.indexOf("Active")
        if (activeStartIndex != -1) {
            val activeEndIndex = activeStartIndex + "Active".length
            spannableString.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.green_60)),
                activeStartIndex,
                activeEndIndex,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.titleStatus.text = spannableString
    }

    private fun loadUserAvatar(event: Event) {
        Glide.with(this)
            .load(event.userAvatar)
            .placeholder(R.drawable.img_profile_placeholder)
            .transform(CircleCropTransformation())
            .into(binding.imageProfile)
    }

    private fun setupImagePager(event: Event) {
        if (!event.images.isNullOrEmpty()) {
            binding.imageEventPager.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.Main).launch {
                val adapter = EventImageAdapter(event.images, supabaseClientProvider)
                binding.imageEventPager.adapter = adapter
            }
        } else {
            binding.imageEventPager.visibility = View.GONE
        }
    }

    private fun initButtonBack() {
        binding.buttonBack.setOnClickListener {
            navigateBackToMainMenu()
        }
    }

    private fun setupOnBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToMainMenu()
            }
        })
    }

    private fun navigateBackToMainMenu() {
        val bundle = Bundle().apply {
            putString("navigateTo", "events")
        }
        findNavController().navigate(R.id.MainMenuFragment, bundle)
    }

    private fun formatTimeAgo(createdAt: OffsetDateTime?): String {
        if (createdAt == null) return "Unknown"

        val now = OffsetDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(createdAt, now)
        val hours = ChronoUnit.HOURS.between(createdAt, now)
        val days = ChronoUnit.DAYS.between(createdAt, now)

        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MM/dd")
                createdAt.format(formatter)
            }
        }
    }
}