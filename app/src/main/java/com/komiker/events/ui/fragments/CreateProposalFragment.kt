package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.SupabaseUserDao
import com.komiker.events.databinding.FragmentCreateProposalBinding
import com.komiker.events.glide.CircleCropTransformation
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreateProposalFragment : Fragment() {

    private var _binding: FragmentCreateProposalBinding? = null
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
        _binding = FragmentCreateProposalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSystemBars()
        setupCloseButton()
        setupPublishButton()
        setupProposalTextWatcher()
        setupOnBackPressed()
        setupUserProfile()
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

    private fun setupCloseButton() {
        binding.buttonClose.setOnClickListener {
            navigateToMainMenuWithProposals()
        }
    }

    private fun setupPublishButton() {
        binding.buttonPublishProposal.setOnClickListener {
            val proposalText = binding.editTextProposal.text.toString()
            if (proposalText.isNotEmpty()) {
                saveProposal(proposalText)
                navigateToMainMenuWithProposals()
            }
        }
    }

    private fun setupProposalTextWatcher() {
        binding.editTextProposal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()
                binding.buttonPublishProposal.isEnabled = text.length >= 10
            }
        })
    }

    private fun setupOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToMainMenuWithProposals()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setupUserProfile() {
        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.textUsername.text = user.username
                Glide.with(this)
                    .load(user.avatar)
                    .override(400, 400)
                    .signature(ObjectKey(System.currentTimeMillis().toString()))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .placeholder(R.drawable.img_profile_placeholder)
                    .transform(CircleCropTransformation())
                    .into(binding.imageProfile)
            } else {
                binding.textUsername.text = getString(R.string.example_name)
                binding.imageProfile.setImageResource(R.drawable.img_profile_placeholder)
            }
        }
    }

    private fun saveProposal(proposalText: String) {
        profileViewModel.userLiveData.value?.let { user ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
                    if (userId != null) {
                        val trimmedText = proposalText.trimEnd()
                        supabaseClient.from("proposals").insert(
                            mapOf(
                                "user_id" to userId,
                                "user_name" to user.username,
                                "user_avatar" to user.avatar,
                                "content" to trimmedText
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun navigateToMainMenuWithProposals() {
        val bundle = Bundle().apply {
            putString("navigateTo", "proposals")
        }
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.MainMenuFragment, false)
            .build()
        findNavController().navigate(
            R.id.action_CreateProposalFragment_to_MainMenuFragment,
            bundle,
            navOptions
        )
    }
}