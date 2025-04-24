package com.komiker.events.ui.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.komiker.events.R
import com.komiker.events.databinding.FragmentCreateEventInfoBinding

class CreateEventInfoFragment : Fragment() {

    private var _binding: FragmentCreateEventInfoBinding? = null
    private val binding get() = _binding!!

    private val maxTitleLength = 255
    private val maxDescriptionLength = 255

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEditTextBackgroundChange()
        setupDescriptionEditTextBackgroundChange()
        setupTitleCharCounter()
        setupDescriptionCharCounter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupEditTextBackgroundChange() {
        val editText = binding.edittextNameOfTheEvent
        val emptyDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_create_event_empty)
        val filledDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_create_event_filled)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    editText.background = emptyDrawable
                } else {
                    editText.background = filledDrawable
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupDescriptionEditTextBackgroundChange() {
        val editText = binding.edittextDescription
        val emptyDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_create_event_empty)
        val filledDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_create_event_filled)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    editText.background = emptyDrawable
                } else {
                    editText.background = filledDrawable
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupTitleCharCounter() {
        binding.textTitleCharCounter.text =
            getString(R.string.char_counter_format, 0, maxTitleLength)

        binding.edittextNameOfTheEvent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val currentLength = s?.length ?: 0
                binding.textTitleCharCounter.text =
                    getString(R.string.char_counter_format, currentLength, maxTitleLength)
            }
        })
    }

    private fun setupDescriptionCharCounter() {
        binding.textDescriptionCharCounter.text =
            getString(R.string.char_counter_format, 0, maxDescriptionLength)

        binding.edittextDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val currentLength = s?.length ?: 0
                binding.textDescriptionCharCounter.text =
                    getString(R.string.char_counter_format, currentLength, maxDescriptionLength)
            }
        })
    }
}