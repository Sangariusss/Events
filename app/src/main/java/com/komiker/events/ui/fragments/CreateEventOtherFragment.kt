package com.komiker.events.ui.fragments

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.EventResponse
import com.komiker.events.databinding.FragmentCreateEventOtherBinding
import com.komiker.events.ui.adapters.ImageAdapter
import com.komiker.events.viewmodels.CreateEventViewModel
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class CreateEventOtherFragment : Fragment() {

    private var _binding: FragmentCreateEventOtherBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val viewModel: CreateEventViewModel by activityViewModels()
    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentCreateEventOtherBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.resetCleared()
        restoreState()
        setupUi()
        setupCreateEventButton()
        observeViewModel()
    }

    override fun onDestroyView() {
        saveState()
        _binding = null
        super.onDestroyView()
    }

    private fun restoreState() {
        with(binding) {
            textStartDate.text = viewModel.startDate ?: ""
            textEndDate.text = viewModel.endDate ?: ""
            editTextInputHour.setText(viewModel.hour ?: getString(R.string._08))
            editTextInputMinute.setText(viewModel.minute ?: getString(R.string._00))
            buttonAm.isSelected = viewModel.isAmSelected
            buttonPm.isSelected = !viewModel.isAmSelected
        }
    }

    private fun saveState() {
        if (!viewModel.isCleared()) {
            with(viewModel) {
                startDate = binding.textStartDate.text.toString().takeIf { it.isNotEmpty() }
                endDate = binding.textEndDate.text.toString().takeIf { it.isNotEmpty() }
                hour = binding.editTextInputHour.text.toString().takeIf { it.isNotEmpty() }
                minute = binding.editTextInputMinute.text.toString().takeIf { it.isNotEmpty() }
                isAmSelected = binding.buttonAm.isSelected
            }
        }
    }

    private fun setupUi() {
        setupDatePickers()
        setupNotNecessaryText()
        setupTimePicker()
        initButtonAddTags()
        initButtonLocation()
    }

    private fun setupCreateEventButton() {
        binding.buttonCreateEvent.setOnClickListener {
            if (supabaseClient.auth.currentSessionOrNull() == null) return@setOnClickListener
            (parentFragment as? CreateEventFragment)?.startLottieAnimation()
            saveEvent()
        }
    }

    private suspend fun uploadImagesToStorage(images: List<ImageAdapter.ImageItem>): List<String> {
        val bucketName = "event-images"
        return images.mapNotNull { imageItem ->
            try {
                val bitmap = BitmapFactory.decodeFile(imageItem.file.absolutePath)
                val maxWidth = 1000
                val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val newWidth = if (bitmap.width > maxWidth) maxWidth else bitmap.width
                val newHeight = (newWidth / aspectRatio).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val fileName = "${UUID.randomUUID()}_${imageItem.file.name}"
                supabaseClient.storage.from(bucketName).upload(fileName, outputStream.toByteArray(), upsert = true)
                fileName
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun saveEvent() {
        profileViewModel.userLiveData.value?.let { user ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
                    if (userId != null && viewModel.title != null) {
                        saveState()
                        val eventTime = "${viewModel.hour}:${viewModel.minute} ${if (viewModel.isAmSelected) "AM" else "PM"}"
                        val imageNames = if (viewModel.images.isNotEmpty()) uploadImagesToStorage(viewModel.images) else emptyList()
                        val event = EventResponse(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            username = user.username,
                            userAvatar = user.avatar,
                            title = viewModel.title!!,
                            description = viewModel.description,
                            startDate = viewModel.startDate,
                            endDate = viewModel.endDate,
                            eventTime = eventTime,
                            tags = viewModel.tags.value,
                            location = viewModel.location.value,
                            images = imageNames,
                            createdAt = OffsetDateTime.now(),
                            likesCount = 0
                        )
                        supabaseClient.from("events").insert(event)
                        viewModel.images.forEach { it.file.delete() }
                        withContext(Dispatchers.Main) {
                            (parentFragment as? CreateEventFragment)?.stopLottieAnimation()
                            viewModel.clear()
                            navigateToMainMenuWithHome()
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        (parentFragment as? CreateEventFragment)?.stopLottieAnimation()
                    }
                }
            }
        }
    }

    private fun navigateToMainMenuWithHome() {
        val bundle = Bundle().apply { putString("navigateTo", "home") }
        val navOptions = NavOptions.Builder().setPopUpTo(R.id.MainMenuFragment, false).build()
        findNavController().navigate(R.id.action_CreateEventFragment_to_MainMenuFragment, bundle, navOptions)
    }

    private fun setupDatePickers() {
        binding.textStartDate.setOnClickListener { showDatePicker { date -> binding.textStartDate.text = date; viewModel.startDate = date } }
        binding.textEndDate.setOnClickListener { showDatePicker { date -> binding.textEndDate.text = date; viewModel.endDate = date } }
    }

    private fun showDatePicker(onDateSet: (String) -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                onDateSet(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupNotNecessaryText() {
        val text = getString(R.string.not_necessary)
        binding.textNotNecessary.text = SpannableString(text).apply {
            setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.red_50)), text.length - 1, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
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
            override fun afterTextChanged(s: Editable?) {
                validateHour(s.toString())
            }
        })
        binding.editTextInputHour.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) formatHourInput() }
    }

    private fun validateHour(hourStr: String) {
        binding.editTextInputHour.setTextColor(ContextCompat.getColor(requireContext(), if (hourStr.isEmpty()) R.color.neutral_0 else {
            val hour = hourStr.toIntOrNull()
            if (hour == null || hour < 1 || hour > 12) R.color.red_50 else R.color.neutral_0
        }))
    }

    private fun formatHourInput() {
        val hour = binding.editTextInputHour.text.toString().toIntOrNull() ?: 1
        binding.editTextInputHour.setText(String.format("%02d", hour.coerceIn(1, 12)))
        binding.editTextInputHour.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
    }

    private fun setupMinutePicker() {
        binding.editTextInputMinute.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateMinute(s.toString())
            }
        })
        binding.editTextInputMinute.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) formatMinuteInput() }
    }

    private fun validateMinute(minuteStr: String) {
        binding.editTextInputMinute.setTextColor(ContextCompat.getColor(requireContext(), if (minuteStr.isEmpty()) R.color.neutral_0 else {
            val minute = minuteStr.toIntOrNull()
            if (minute == null || minute < 0 || minute > 59 || minuteStr == "0") R.color.red_50 else R.color.neutral_0
        }))
    }

    private fun formatMinuteInput() {
        val minute = binding.editTextInputMinute.text.toString().toIntOrNull() ?: 0
        binding.editTextInputMinute.setText(String.format("%02d", minute.coerceIn(0, 59)))
        binding.editTextInputMinute.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
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
            findNavController().navigate(
                R.id.action_CreateEventFragment_to_TagsFragment,
                Bundle().apply { putInt("sourceFragmentId", R.id.CreateEventFragment) }
            )
        }
    }

    private fun initButtonLocation() {
        binding.constraintLocationButtonLayout.setOnClickListener {
            findNavController().navigate(
                R.id.action_CreateEventFragment_to_LocationFragment,
                Bundle().apply { putInt("sourceFragmentId", R.id.CreateEventFragment) }
            )
        }
    }

    private fun observeViewModel() {
        viewModel.location.observe(viewLifecycleOwner) {}
        viewModel.tags.observe(viewLifecycleOwner) {}
    }
}