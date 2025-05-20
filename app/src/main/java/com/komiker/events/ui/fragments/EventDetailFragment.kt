package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
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
import com.komiker.events.ui.adapters.EventImageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.storage.storage

class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private val supabaseClientProvider = SupabaseClientProvider

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BundleCompat.getParcelable(requireArguments(), "event", Event::class.java)?.let { event ->
            setupSystemBars()
            viewLifecycleOwner.lifecycleScope.launch { setupUI(event) }
            initButtonBack()
            setupOnBackPressedCallback()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupSystemBars() {
        requireActivity().window.navigationBarColor =
            ContextCompat.getColor(requireContext(), R.color.neutral_100)
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
        textLikesCount.text = event.likesCount.toString()
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
        binding.buttonBack.setOnClickListener { navigateBackToMainMenu() }
    }

    private fun setupOnBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = navigateBackToMainMenu()
        })
    }

    private fun navigateBackToMainMenu() {
        findNavController().navigate(
            R.id.MainMenuFragment,
            Bundle().apply { putString("navigateTo", "events") }
        )
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
}
