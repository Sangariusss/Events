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

    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(SupabaseUserDao(SupabaseClientProvider.client))
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
        setupNavigation()
        loadSavedLinks()
        setupEditTextListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSystemBars() {
        requireActivity().window.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
    }

    private fun setupNavigation() {
        binding.buttonBack.setOnClickListener { navigateWithSave() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateWithSave()
            }
        })
    }

    private fun navigateWithSave() {
        saveSocialMediaLinks()
        findNavController().navigate(
            R.id.action_AddSocialMediaFragment_to_MainMenuFragment,
            Bundle().apply { putString("navigateTo", "profile") }
        )
    }

    private fun loadSavedLinks() {
        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.editSocialMedia1.apply {
                    setText(it.telegram_link ?: "")
                    updateValidationState(this, "telegram")
                }
                binding.editSocialMedia2.apply {
                    setText(it.instagram_link ?: "")
                    updateValidationState(this, "instagram")
                }
            }
        }
    }

    private fun setupEditTextListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val editText = s?.let { editable ->
                    binding.editSocialMedia1.takeIf { it.text === editable }
                        ?: binding.editSocialMedia2.takeIf { it.text === editable }
                } ?: return
                val platform = if (editText == binding.editSocialMedia1) "telegram" else "instagram"
                updateValidationState(editText, platform)
            }
        }

        binding.editSocialMedia1.addTextChangedListener(textWatcher)
        binding.editSocialMedia2.addTextChangedListener(textWatcher)
    }

    private fun updateValidationState(editText: EditText, platform: String) {
        val text = editText.text.toString().trim()

        if (text.length >= 4 && isValidUrl(text, platform)) {
            editText.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_green, 0)
        } else {
            editText.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_0))
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

            if (text.length >= 4) {
                editText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_50))
            }
        }
    }

    private fun isValidUrl(url: String, platform: String): Boolean {
        return when (platform) {
            "telegram" -> url.matches(TELEGRAM_REGEX)
            "instagram" -> url.matches(INSTAGRAM_REGEX)
            else -> false
        }
    }

    private fun saveSocialMediaLinks() {
        val telegramLink = normalizeUrl(binding.editSocialMedia1.text.toString().trim(), "telegram")
        val instagramLink = normalizeUrl(binding.editSocialMedia2.text.toString().trim(), "instagram")

        profileViewModel.updateSocialLinks(telegramLink, instagramLink)
    }

    private fun normalizeUrl(url: String, platform: String): String? {
        if (url.isEmpty()) return null
        val normalizedUrl = if (!url.startsWith("https://")) "https://$url" else url
        return normalizedUrl.takeIf { isValidUrl(it, platform) }
    }

    companion object {
        private val TELEGRAM_REGEX = Regex("(https?://(www\\.)?)?t\\.me/[a-zA-Z0-9_]+")
        private val INSTAGRAM_REGEX = Regex("(https?://(www\\.)?)?instagram\\.com/[a-zA-Z0-9_./-]+")
    }
}