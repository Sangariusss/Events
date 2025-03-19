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
import com.komiker.events.data.database.dao.SupabaseUserDao
import com.komiker.events.data.database.entities.User
import com.komiker.events.databinding.FragmentEditUsernameBinding
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditUsernameFragment : Fragment() {

    private var _binding: FragmentEditUsernameBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private var isTextChanged = false
    private var isUsernameAvailable = false

    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }

    private val verticalBiasDefault = 0.188F
    private val verticalBiasShifted = 0.234F

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditUsernameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initButtonBack()
        setupUsernameInput()
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

    private fun setupUsernameInput() {
        binding.editUsernameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString()?.trim() ?: ""
                if (text.length >= 5) checkUsernameAvailability(text)
                else resetAvailability()
            }
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().trim() != binding.editUsernameInput.tag?.toString()?.trim()) isTextChanged = true
            }
        })
    }

    private fun setupButtonCheckmark() {
        binding.buttonCheckmark.setOnClickListener {
            val newUsername = binding.editUsernameInput.text.toString().trim()
            if (newUsername.length < 5) {
                resetAvailability()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                checkUsernameAvailability(newUsername)
                _binding?.let {
                    val currentName = profileViewModel.userLiveData.value?.username?.trim() ?: ""
                    if (isUsernameAvailable || newUsername == currentName) {
                        updateUsername(newUsername)
                    } else {
                        Log.d("EditUsernameFragment", "Cannot update: username '$newUsername' is not available")
                    }
                }
            }
        }
    }

    private fun observeUserData() {
        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            val username = user?.username?.trim() ?: ""
            binding.editUsernameInput.setText(username)
            binding.editUsernameInput.tag = username
            isTextChanged = false
            checkUsernameAvailability(username)
        }
    }

    private fun checkUsernameAvailability(username: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentUsername = profileViewModel.userLiveData.value?.username?.trim() ?: ""
            val currentUserId = profileViewModel.userLiveData.value?.user_id

            val isValidSyntax = username.matches(Regex("^[a-zA-Z0-9_.\\s]+$"))
            val hasMultipleSpaces = username.indexOf(' ') != username.lastIndexOf(' ')
            val isOnlySpecialChars = username.matches(Regex("^[._\\s]+$"))

            val isNameTaken = checkUsernameTakenInSupabase(username, currentUserId)

            withContext(Dispatchers.Main) {
                _binding?.let { binding ->
                    isUsernameAvailable = !isNameTaken && isValidSyntax && !hasMultipleSpaces && !isOnlySpecialChars

                    binding.textUsernameAvailability.text = when {
                        username == currentUsername -> ""
                        !isUsernameAvailable -> "$username ${getString(R.string.not_available)}"
                        else -> "$username ${getString(R.string.available)}"
                    }

                    binding.textUsernameAvailability.setTextColor(
                        ContextCompat.getColor(requireContext(), if (!isUsernameAvailable) R.color.red_50 else R.color.green_60)
                    )
                    binding.buttonCheckmark.isEnabled = isUsernameAvailable || username == currentUsername
                    adjustLayoutOnAvailability()
                } ?: Log.w("EditUsernameFragment", "Binding is null, skipping UI update")
            }
        }
    }

    private suspend fun checkUsernameTakenInSupabase(username: String, currentUserId: String?): Boolean {
        return try {
            val response: PostgrestResult = supabaseClient.from("users").select {
                filter {
                    eq("username", username)
                    if (currentUserId != null) {
                        neq("user_id", currentUserId)
                    }
                }
            }
            val usersList = response.decodeList<User>()
            Log.d("CheckUsername", "Response for '$username': $usersList")
            usersList.isNotEmpty()
        } catch (e: Exception) {
            Log.e("CheckUsername", "Error checking username availability: ${e.message}")
            true
        }
    }

    private fun resetAvailability() {
        _binding?.let { binding ->
            binding.textUsernameAvailability.text = ""
            binding.buttonCheckmark.isEnabled = false
            isUsernameAvailable = false
            adjustLayoutOnAvailability()
        } ?: Log.w("EditUsernameFragment", "Binding is null, skipping reset")
    }

    private fun adjustLayoutOnAvailability() {
        _binding?.let { binding ->
            val layoutParams = binding.textUsernameRequirements.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.verticalBias = if (binding.textUsernameAvailability.text.isNotEmpty()) verticalBiasShifted else verticalBiasDefault
            binding.textUsernameRequirements.layoutParams = layoutParams
        } ?: Log.w("EditUsernameFragment", "Binding is null, skipping layout adjustment")
    }

    private fun updateUsername(newUsername: String) {
        profileViewModel.updateUsername(newUsername)
        navigateToEditProfile()
    }

    private fun navigateToEditProfile() {
        findNavController().navigate(R.id.action_EditUsernameFragment_to_EditProfileFragment)
    }
}