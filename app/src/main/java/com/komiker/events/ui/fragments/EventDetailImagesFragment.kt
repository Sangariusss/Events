package com.komiker.events.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.models.Event
import com.komiker.events.databinding.FragmentEventDetailImagesBinding
import com.komiker.events.ui.adapters.EventImageAdapter
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class EventDetailImagesFragment : Fragment() {

    private var _binding: FragmentEventDetailImagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var event: Event
    private lateinit var imageAdapter: EventImageAdapter

    companion object {
        private const val ARG_EVENT = "event"

        fun newInstance(event: Event): EventDetailImagesFragment {
            val fragment = EventDetailImagesFragment()
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
        _binding = FragmentEventDetailImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadImages()
    }

    private fun initArguments() {
        arguments?.let { bundle ->
            event = BundleCompat.getParcelable(bundle, ARG_EVENT, Event::class.java)
                ?: throw IllegalArgumentException("Event is null")
        }
    }

    private fun setupRecyclerView() {
        imageAdapter = EventImageAdapter()
        binding.imageRecyclerView.adapter = imageAdapter
    }

    private fun loadImages() {
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.imageRecyclerView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val urls = withContext(Dispatchers.IO) {
                event.images?.map { path ->
                    SupabaseClientProvider.client.storage
                        .from("event-images")
                        .createSignedUrl(path, 60.seconds)
                } ?: emptyList()
            }

            imageAdapter.submitList(urls)

            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.visibility = View.GONE
            binding.imageRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}