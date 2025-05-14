package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.databinding.FragmentAddSocialMediaBinding
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory

class AddSocialMediaFragment : Fragment() {

    private var _binding: FragmentAddSocialMediaBinding? = null
    private val binding get() = _binding!!

    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)

    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSocialMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSystemBars()
        initButtonBack()
        setupOnBackPressedCallback()
        loadSavedLinks()
        setupEditTextListeners()
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
            saveSocialMediaLinks()
            navigateToMainMenuWithProfile()
        }
    }

    private fun setupOnBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveSocialMediaLinks()
                navigateToMainMenuWithProfile()
            }
        })
    }

    private fun loadSavedLinks() {
        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.editSocialMedia1.setText(it.telegram_link ?: "")
                binding.editSocialMedia2.setText(it.instagram_link ?: "")
            }
        }
    }

    private fun setupEditTextListeners() {
        setupEditTextValidation(binding.editSocialMedia1, "telegram")
        setupEditTextValidation(binding.editSocialMedia2, "instagram")
    }

    private fun setupEditTextValidation(editText: EditText, platform: String) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()
                if (text.length >= 4) {
                    if (isValidUrl(text, platform)) {
                        editText.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
                    } else {
                        editText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_50))
                    }
                } else {
                    editText.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
                }
            }
        })
    }

    private fun isValidUrl(url: String, platform: String): Boolean {
        return when (platform) {
            "telegram" -> url.matches(Regex("(https?://(www\\.)?)?t\\.me/[a-zA-Z0-9_]+"))
            "instagram" -> url.matches(Regex("(https?://(www\\.)?)?instagram\\.com/[a-zA-Z0-9_./-]+"))
            else -> false
        }
    }

    private fun saveSocialMediaLinks() {
        var telegramLink = binding.editSocialMedia1.text.toString().trim()
        var instagramLink = binding.editSocialMedia2.text.toString().trim()

        if (telegramLink.isNotEmpty() && !telegramLink.startsWith("https://")) {
            telegramLink = "https://$telegramLink"
        }
        if (instagramLink.isNotEmpty() && !instagramLink.startsWith("https://")) {
            instagramLink = "https://$instagramLink"
        }

        profileViewModel.updateSocialLinks(
            telegramLink.takeIf { isValidUrl(it, "telegram") || it.isEmpty() },
            instagramLink.takeIf { isValidUrl(it, "instagram") || it.isEmpty() }
        )
    }

    private fun navigateToMainMenuWithProfile() {
        findNavController().navigate(
            R.id.action_AddSocialMediaFragment_to_MainMenuFragment,
            Bundle().apply {
                putString("navigateTo", "profile")
            }
        )
    }
}