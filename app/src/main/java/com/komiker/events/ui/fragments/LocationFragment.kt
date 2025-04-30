package com.komiker.events.ui.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.databinding.FragmentLocationBinding

class LocationFragment : Fragment() {

    private var _binding: FragmentLocationBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtonBack()
        setupEditTextBackgroundChange()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupButtonBack() {
        binding.buttonBack.setOnClickListener {
            val sourceFragmentId = arguments?.getInt("sourceFragmentId") ?: R.id.FilterFragment
            if (sourceFragmentId == R.id.CreateEventFragment) {
                findNavController().popBackStack(R.id.CreateEventFragment, false)
            } else {
                val actionId = when (sourceFragmentId) {
                    R.id.FilterFragment -> R.id.action_LocationFragment_to_FilterFragment
                    else -> R.id.action_LocationFragment_to_FilterFragment
                }
                findNavController().navigate(
                    actionId,
                    null
                )
            }
        }
    }

    private fun setupEditTextBackgroundChange() {
        val editText = requireView().findViewById<EditText>(R.id.edit_text_find_location)

        val emptyDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_empty)
        val filledDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et_find_filled)

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
}