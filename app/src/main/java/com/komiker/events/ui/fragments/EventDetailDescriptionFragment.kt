package com.komiker.events.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.models.Event
import com.komiker.events.data.database.models.EventView
import com.komiker.events.databinding.FragmentEventDetailDescriptionBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.util.Locale
import java.util.UUID

class EventDetailDescriptionFragment : Fragment() {

    private var _binding: FragmentEventDetailDescriptionBinding? = null
    private val binding get() = _binding!!
    private lateinit var event: Event
    private var currentViewsCount: Int = 0
    private val supabaseClient = SupabaseClientProvider.client

    companion object {
        private const val ARG_EVENT = "event"

        fun newInstance(event: Event): EventDetailDescriptionFragment {
            val fragment = EventDetailDescriptionFragment()
            val args = Bundle().apply { putParcelable(ARG_EVENT, event) }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initArguments()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
        incrementViewCountIfNeeded()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun initArguments() {
        arguments?.let { bundle ->
            val eventFromBundle = BundleCompat.getParcelable(bundle, ARG_EVENT, Event::class.java)
                ?: throw IllegalArgumentException("Event object is null in arguments")
            event = eventFromBundle

            currentViewsCount = bundle.getInt("viewsCount", event.viewsCount)
        }
    }

    private fun updateUI() {
        binding.textTitle.text = event.title
        binding.textContent.text = event.description
        binding.titleStatus.text = getString(R.string.status_active)
        binding.titleStartDate.text = getString(R.string.start_time_format, event.eventTime ?: "Not specified")
        binding.titleStartDateContent.text = getString(R.string.event_date_range, event.startDate, event.endDate)
        binding.titleTagsContent.text = event.tags?.joinToString(", ") { "#${it.replace(" ", "")}" } ?: "No tags"
        binding.contentReviewed.text = formatCount(currentViewsCount)
    }

    private fun incrementViewCountIfNeeded() {
        val currentUserId = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existingView = supabaseClient.from("event_views")
                    .select {
                        filter { eq("event_id", event.id); eq("user_id", currentUserId) }
                    }
                    .decodeSingleOrNull<EventView>()

                if (existingView == null) {
                    val newViewsCount = currentViewsCount + 1

                    withContext(Dispatchers.Main) {
                        if(isAdded) {
                            binding.contentReviewed.text = formatCount(newViewsCount)
                        }
                    }

                    supabaseClient.from("event_views").insert(
                        EventView(
                            id = UUID.randomUUID().toString(),
                            eventId = event.id,
                            userId = currentUserId,
                            viewedAt = OffsetDateTime.now()
                        )
                    )

                    supabaseClient.from("events").update(
                        mapOf("views_count" to newViewsCount)
                    ) {
                        filter { eq("id", UUID.fromString(event.id)) }
                    }
                }
            } catch (e: Exception) {
                Log.e("EventDetail", "Error updating views_count: ${e.message}", e)
            }
        }
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 999_999_950 -> {
                val billions = count / 1_000_000_000.0
                String.format(Locale.US, "%.1fB", billions)
            }
            count >= 999_950 -> {
                val millions = count / 1_000_000.0
                String.format(Locale.US, "%.1fM", millions)
            }
            count >= 1_000 -> {
                val thousands = count / 1_000.0
                String.format(Locale.US, "%.1fK", thousands)
            }
            else -> count.toString()
        }
    }
}