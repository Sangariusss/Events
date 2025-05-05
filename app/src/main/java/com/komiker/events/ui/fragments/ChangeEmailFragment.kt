package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.databinding.FragmentChangeEmailBinding
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import kotlinx.coroutines.launch

class ChangeEmailFragment : Fragment() {

    private var _binding: FragmentChangeEmailBinding? = null
    private val binding get() = _binding!!
    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)

    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }

    private var currentEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangeEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSystemBars()
        initButtonBack()
        setupEmailEditText()
        setupButtonConfirm()
        observeUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSystemBars() {
        requireActivity().window.apply {
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        }
    }

    private fun initButtonBack() {
        binding.buttonBack.setOnClickListener {
            navigateToMainMenuWithProfile()
        }
    }

    private fun setupEmailEditText() {
        binding.editTextEmail.filters = arrayOf(noSpaceFilter)
        binding.editTextEmail.requestFocus()

        binding.editTextEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val email = s.toString().trim()
                if (email.isEmpty()) {
                    setEditTextState(binding.editTextEmail, true)
                    binding.buttonConfirm.setBackgroundResource(R.drawable.bg_btn_rounded_black_inactive)
                    binding.buttonConfirm.isEnabled = false
                } else if (!isValidEmail(email) || email == currentEmail) {
                    setEditTextState(binding.editTextEmail, false)
                    binding.buttonConfirm.setBackgroundResource(R.drawable.bg_btn_rounded_black_inactive)
                    binding.buttonConfirm.isEnabled = false
                } else {
                    setEditTextState(binding.editTextEmail, true)
                    binding.buttonConfirm.setBackgroundResource(R.drawable.bg_btn_rounded_black)
                    binding.buttonConfirm.isEnabled = true
                }
            }
        })
    }

    private fun setupButtonConfirm() {
        binding.buttonConfirm.setOnClickListener {
            val newEmail = binding.editTextEmail.text.toString().trim()
            if (isValidEmail(newEmail) && newEmail != currentEmail) {
                binding.buttonConfirm.isEnabled = false
                binding.buttonConfirm.setBackgroundResource(R.drawable.bg_btn_rounded_black_inactive)
                binding.editTextEmail.isEnabled = false

                lifecycleScope.launch {
                    updateEmail(newEmail)
                    binding.buttonConfirm.isEnabled = true
                    binding.buttonConfirm.setBackgroundResource(R.drawable.bg_btn_rounded_black)
                    binding.editTextEmail.isEnabled = true
                }
            }
        }
    }

    private fun observeUserData() {
        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            val email = user?.email?.trim() ?: ""
            currentEmail = email
            binding.textCurrentEmail.text = email
            binding.editTextEmail.setText(email)
            binding.editTextEmail.tag = email
            setEditTextState(binding.editTextEmail, true)
            binding.buttonConfirm.setBackgroundResource(R.drawable.bg_btn_rounded_black_inactive)
            binding.buttonConfirm.isEnabled = false
        }
    }

    private fun updateEmail(newEmail: String) {
        profileViewModel.updateEmail(newEmail)
        navigateToCheckYourNewEmail()
    }

    private fun navigateToCheckYourNewEmail() {
        findNavController().navigate(R.id.action_ChangeEmailFragment_to_CheckYourNewEmailFragment)
    }

    private fun navigateToMainMenuWithProfile() {
        findNavController().navigate(
            R.id.action_ChangeEmailFragment_to_MainMenuFragment,
            Bundle().apply {
                putString("navigateTo", "profile")
            }
        )
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    private fun setEditTextState(editText: EditText, isValid: Boolean) {
        if (!isValid) {
            editText.setBackgroundResource(R.drawable.bg_et_registration_error)
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_text_edit_error, 0)
            editText.setTextAppearance(R.style.EditTextRegistrationError)
        } else {
            if (editText.text.toString().isEmpty()) {
                editText.setBackgroundResource(R.drawable.bg_et_registration)
            } else {
                editText.setBackgroundResource(R.drawable.bg_et_registration_correct)
            }
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            editText.setTextAppearance(R.style.EditTextRegistrationCustom)
        }
    }

    private val noSpaceFilter = InputFilter { source, _, _, _, _, _ ->
        if (source.any { it.isWhitespace() }) "" else null
    }
}