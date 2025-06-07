package com.komiker.events.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.komiker.events.R
import com.komiker.events.data.database.models.Event
import com.komiker.events.databinding.FragmentEventDetailDescriptionBinding

class EventDetailDescriptionFragment : Fragment() {

    private var _binding: FragmentEventDetailDescriptionBinding? = null
    private val binding get() = _binding!!
    private lateinit var event: Event

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
        binding.textTitle.text = event.title
        binding.textContent.text = event.description
        binding.titleStatus.text = getString(R.string.status_active)
        binding.titleStartDate.text = getString(R.string.start_time_format, event.eventTime ?: "Not specified")
        binding.titleStartDateContent.text = getString(R.string.event_date_range, event.startDate, event.endDate)
        binding.titleTagsContent.text = event.tags?.joinToString(", ") { "#${it.replace(" ", "")}" } ?: "No tags"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun initArguments() {
        arguments?.let { bundle ->
            event = BundleCompat.getParcelable(bundle, ARG_EVENT, Event::class.java)
                ?: throw IllegalArgumentException("Event is null")
        }
    }
}