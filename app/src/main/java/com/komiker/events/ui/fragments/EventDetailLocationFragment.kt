package com.komiker.events.ui.fragments

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.komiker.events.R
import com.komiker.events.data.database.models.Event
import com.komiker.events.databinding.FragmentEventDetailLocationBinding

class EventDetailLocationFragment : Fragment() {

    private var _binding: FragmentEventDetailLocationBinding? = null
    private val binding get() = _binding!!
    private lateinit var event: Event
    private var currentAnimator: ValueAnimator? = null

    companion object {
        private const val ARG_EVENT = "event"

        fun newInstance(event: Event): EventDetailLocationFragment {
            val fragment = EventDetailLocationFragment()
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
        _binding = FragmentEventDetailLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
    }

    override fun onDestroyView() {
        currentAnimator?.cancel()
        _binding = null
        super.onDestroyView()
    }

    private fun initArguments() {
        arguments?.let { bundle ->
            event = BundleCompat.getParcelable(bundle, ARG_EVENT, Event::class.java)
                ?: throw IllegalArgumentException("Event is null")
        }
    }

    private fun setupViews() {
        binding.titleAddressContent.text = event.location ?: "Not specified"
    }

    private fun setupClickListeners() {
        binding.titleAddressContent.setOnLongClickListener {
            handleCopyLocation()
            true
        }

        binding.buttonCheckLocation.setOnClickListener {
            openLocationInMaps(binding.titleAddressContent.text.toString())
        }
    }

    private fun handleCopyLocation() {
        val locationText = binding.titleAddressContent.text.toString()
        if (locationText.isNotBlank() && locationText != "Not specified") {
            copyToClipboard(locationText)
            currentAnimator?.cancel()
            animateCopyHighlightEffect()
        } else {
            Toast.makeText(requireContext(), "No location to copy", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Location", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun animateCopyHighlightEffect() {
        val highlightColor = ContextCompat.getColor(requireContext(), R.color.neutral_95)
        currentAnimator = ValueAnimator.ofInt(0, 100, 0).apply {
            duration = 600
            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Int
                val currentAlpha = (alpha * 255) / 100
                val animatedColor = (currentAlpha shl 24) or (highlightColor and 0x00FFFFFF)
                binding.titleAddressContent.setBackgroundColor(animatedColor)
            }
        }
        currentAnimator?.start()
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
                Toast.makeText(requireContext(), "No map application found", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}