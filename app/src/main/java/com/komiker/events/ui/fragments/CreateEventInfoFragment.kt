package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.komiker.events.R
import com.komiker.events.databinding.FragmentCreateEventInfoBinding
import com.komiker.events.viewmodels.CreateEventViewModel

class CreateEventInfoFragment : Fragment() {

    private var _binding: FragmentCreateEventInfoBinding? = null
    private val binding get() = _binding!!

    private val maxTitleLength = 100
    private val maxDescriptionLength = 500
    private val viewModel: CreateEventViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreState()
        setupTextFields()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun restoreState() {
        viewModel.title?.let { binding.edittextNameOfTheEvent.setText(it) }
        viewModel.description?.let { binding.edittextDescription.setText(it) }
    }

    private fun setupTextFields() {
        val emptyDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_create_event_empty)
        val filledDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_create_event_filled)

        // Settings for title
        binding.textTitleCharCounter.text = getString(R.string.char_counter_format, 0, maxTitleLength)
        binding.edittextNameOfTheEvent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.edittextNameOfTheEvent.background = if (s.isNullOrEmpty()) emptyDrawable else filledDrawable
                viewModel.title = s?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            }
            override fun afterTextChanged(s: Editable?) {
                binding.textTitleCharCounter.text = getString(R.string.char_counter_format, s?.length ?: 0, maxTitleLength)
            }
        })

        // Settings for description
        binding.textDescriptionCharCounter.text = getString(R.string.char_counter_format, 0, maxDescriptionLength)
        binding.edittextDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.edittextDescription.background = if (s.isNullOrEmpty()) emptyDrawable else filledDrawable
                viewModel.description = s?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            }
            override fun afterTextChanged(s: Editable?) {
                binding.textDescriptionCharCounter.text = getString(R.string.char_counter_format, s?.length ?: 0, maxDescriptionLength)
            }
        })
    }
}