package com.komiker.events.ui.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.databinding.FragmentCreateEventOtherBinding
import com.komiker.events.viewmodels.CreateEventViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateEventOtherFragment : Fragment() {

    private var _binding: FragmentCreateEventOtherBinding? = null
    private val binding get() = _binding!!

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private val viewModel: CreateEventViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventOtherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreState()
        setupUi()
    }

    override fun onDestroyView() {
        saveState()
        super.onDestroyView()
        _binding = null
    }

    private fun restoreState() {
        viewModel.startDate?.let { binding.textStartDate.text = it }
        viewModel.endDate?.let { binding.textEndDate.text = it }
        viewModel.hour?.let { binding.editTextInputHour.setText(it) } ?: binding.editTextInputHour.setText(R.string._08)
        viewModel.minute?.let { binding.editTextInputMinute.setText(it) } ?: binding.editTextInputMinute.setText(R.string._00)
        binding.buttonAm.isSelected = viewModel.isAmSelected
        binding.buttonPm.isSelected = !viewModel.isAmSelected
    }

    private fun saveState() {
        viewModel.startDate = binding.textStartDate.text.toString()
        viewModel.endDate = binding.textEndDate.text.toString()
        viewModel.hour = binding.editTextInputHour.text.toString()
        viewModel.minute = binding.editTextInputMinute.text.toString()
        viewModel.isAmSelected = binding.buttonAm.isSelected
    }

    private fun setupUi() {
        setupDatePickers()
        setupNotNecessaryText()
        setupTimePicker()
        initButtonAddTags()
        initButtonLocation()
    }

    private fun setupDatePickers() {
        binding.textStartDate.setOnClickListener { showDatePicker { binding.textStartDate.text = it; viewModel.startDate = it } }
        binding.textEndDate.setOnClickListener { showDatePicker { binding.textEndDate.text = it; viewModel.endDate = it } }
    }

    private fun showDatePicker(onDateSet: (String) -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                onDateSet(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupNotNecessaryText() {
        val text = getString(R.string.not_necessary)
        val spannable = SpannableString(text).apply {
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.red_50)),
                text.length - 1,
                text.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.textNotNecessary.text = spannable
    }

    private fun setupTimePicker() {
        setupHourPicker()
        setupMinutePicker()
        setupAmPmButtons()
    }

    private fun setupHourPicker() {
        binding.editTextInputHour.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = validateHour(s.toString())
        })

        binding.editTextInputHour.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) formatHourInput()
        }
    }

    private fun validateHour(hourStr: String) {
        if (hourStr.isEmpty()) {
            binding.editTextInputHour.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
        } else {
            val hour = hourStr.toIntOrNull()
            val color = if (hour == null || hour < 1 || hour > 12) R.color.red_50 else R.color.neutral_0
            binding.editTextInputHour.setTextColor(ContextCompat.getColor(requireContext(), color))
        }
    }

    private fun formatHourInput() {
        val hourStr = binding.editTextInputHour.text.toString()
        if (hourStr.isEmpty()) {
            binding.editTextInputHour.setText(R.string._01)
            binding.editTextInputMinute.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
        } else {
            val hour = hourStr.toIntOrNull() ?: 1
            val correctedHour = when {
                hour < 1 -> 1
                hour > 12 -> 12
                else -> hour
            }
            binding.editTextInputHour.setText(String.format("%02d", correctedHour))
            binding.editTextInputHour.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
        }
    }

    private fun setupMinutePicker() {
        binding.editTextInputMinute.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = validateMinute(s.toString())
        })

        binding.editTextInputMinute.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) formatMinuteInput()
        }
    }

    private fun validateMinute(minuteStr: String) {
        if (minuteStr.isEmpty()) {
            binding.editTextInputMinute.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
        } else {
            val minute = minuteStr.toIntOrNull()
            val color = if (minute == null || minute < 0 || minute > 59 || minuteStr == "0") R.color.red_50 else R.color.neutral_0
            binding.editTextInputMinute.setTextColor(ContextCompat.getColor(requireContext(), color))
        }
    }

    private fun formatMinuteInput() {
        val minuteStr = binding.editTextInputMinute.text.toString()
        if (minuteStr.isEmpty()) {
            binding.editTextInputMinute.setText(R.string._00)
            binding.editTextInputMinute.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
        } else {
            val minute = minuteStr.toIntOrNull() ?: 0
            val correctedMinute = when {
                minute < 0 -> 0
                minute > 59 -> 59
                else -> minute
            }
            binding.editTextInputMinute.setText(String.format("%02d", correctedMinute))
            binding.editTextInputMinute.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
        }
    }

    private fun setupAmPmButtons() {
        binding.buttonAm.setOnClickListener {
            if (!binding.buttonAm.isSelected) {
                binding.buttonAm.isSelected = true
                binding.buttonPm.isSelected = false
            }
        }
        binding.buttonPm.setOnClickListener {
            if (!binding.buttonPm.isSelected) {
                binding.buttonPm.isSelected = true
                binding.buttonAm.isSelected = false
            }
        }
    }

    private fun initButtonAddTags() {
        binding.constraintAddTagsButtonLayout.setOnClickListener {
            val bundle = Bundle().apply { putInt("sourceFragmentId", R.id.CreateEventFragment) }
            findNavController().navigate(R.id.action_CreateEventFragment_to_TagsFragment, bundle)
        }
    }

    private fun initButtonLocation() {
        binding.constraintLocationButtonLayout.setOnClickListener {
            val bundle = Bundle().apply { putInt("sourceFragmentId", R.id.CreateEventFragment) }
            findNavController().navigate(R.id.action_CreateEventFragment_to_LocationFragment, bundle)
        }
    }
}