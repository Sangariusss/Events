package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.User
import com.komiker.events.databinding.FragmentEditNameBinding
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditNameFragment : Fragment() {

    private var _binding: FragmentEditNameBinding? = null
    private val binding get() = _binding!!
    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private var isTextChanged = false
    private var isNameAvailable = false

    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }

    private val verticalBiasDefault = 0.188F
    private val verticalBiasShifted = 0.234F

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initButtonBack()
        setupNameInput()
        setupButtonCheckmark()
        observeUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initButtonBack() {
        binding.buttonBack.setOnClickListener {
            navigateToEditProfile()
        }
    }

    private fun setupNameInput() {
        binding.editNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString()?.trim() ?: ""
                if (text.length >= 5) checkNameAvailability(text)
                else resetAvailability()
            }
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().trim() != binding.editNameInput.tag?.toString()?.trim()) isTextChanged = true
            }
        })
    }

    private fun setupButtonCheckmark() {
        binding.buttonCheckmark.setOnClickListener {
            val newName = binding.editNameInput.text.toString().trim()
            if (newName.length < 5) {
                resetAvailability()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                checkNameAvailability(newName)
                _binding?.let {
                    val currentName = profileViewModel.userLiveData.value?.name?.trim() ?: ""
                    if (isNameAvailable || newName == currentName) {
                        updateName(newName)
                    } else {
                        Log.d("EditNameFragment", "Cannot update: name '$newName' is not available")
                    }
                }
            }
        }
    }

    private fun observeUserData() {
        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            val name = user?.name?.trim() ?: ""
            binding.editNameInput.setText(name)
            binding.editNameInput.tag = name
            isTextChanged = false
            checkNameAvailability(name)
        }
    }

    private fun checkNameAvailability(name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentName = profileViewModel.userLiveData.value?.name?.trim() ?: ""
            val currentUserId = profileViewModel.userLiveData.value?.user_id

            val isValidSyntax = name.matches(Regex("^[a-zA-Z0-9_.\\s]+$"))
            val hasMultipleSpaces = name.indexOf(' ') != name.lastIndexOf(' ')
            val isOnlySpecialChars = name.matches(Regex("^[._\\s]+$"))

            val isNameTaken = checkNameTakenInSupabase(name, currentUserId)

            withContext(Dispatchers.Main) {
                _binding?.let { binding ->
                    isNameAvailable = !isNameTaken && isValidSyntax && !hasMultipleSpaces && !isOnlySpecialChars

                    binding.textNameAvailability.text = when {
                        name == currentName -> ""
                        !isNameAvailable -> "$name ${getString(R.string.not_available)}"
                        else -> "$name ${getString(R.string.available)}"
                    }

                    binding.textNameAvailability.setTextColor(
                        ContextCompat.getColor(requireContext(), if (!isNameAvailable) R.color.red_50 else R.color.green_60)
                    )
                    binding.buttonCheckmark.isEnabled = isNameAvailable || name == currentName
                    adjustLayoutOnAvailability()
                } ?: Log.w("EditNameFragment", "Binding is null, skipping UI update")
            }
        }
    }

    private suspend fun checkNameTakenInSupabase(name: String, currentUserId: String?): Boolean {
        return try {
            val response: PostgrestResult = supabaseClient.from("users").select {
                filter {
                    eq("name", name)
                    if (currentUserId != null) {
                        neq("user_id", currentUserId)
                    }
                }
            }
            val usersList = response.decodeList<User>()
            usersList.isNotEmpty()
        } catch (e: Exception) {
            Log.e("CheckName", "Error checking name availability: ${e.message}")
            true
        }
    }

    private fun resetAvailability() {
        _binding?.let { binding ->
            binding.textNameAvailability.text = ""
            binding.buttonCheckmark.isEnabled = false
            isNameAvailable = false
            adjustLayoutOnAvailability()
        } ?: Log.w("EditNameFragment", "Binding is null, skipping reset")
    }

    private fun adjustLayoutOnAvailability() {
        _binding?.let { binding ->
            val layoutParams = binding.textNameRequirements.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.verticalBias = if (binding.textNameAvailability.text.isNotEmpty()) verticalBiasShifted else verticalBiasDefault
            binding.textNameRequirements.layoutParams = layoutParams
        } ?: Log.w("EditNameFragment", "Binding is null, skipping layout adjustment")
    }

    private fun updateName(newName: String) {
        profileViewModel.updateName(newName)
        navigateToEditProfile()
    }

    private fun navigateToEditProfile() {
        findNavController().popBackStack()
    }
}