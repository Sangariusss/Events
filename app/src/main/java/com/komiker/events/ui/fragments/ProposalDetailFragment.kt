package com.komiker.events.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.Proposal
import com.komiker.events.databinding.FragmentProposalDetailBinding
import com.komiker.events.glide.CircleCropTransformation
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

class ProposalDetailFragment : Fragment() {

    private var _binding: FragmentProposalDetailBinding? = null
    private val binding get() = _binding!!

    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)

    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProposalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val proposal: Proposal? = BundleCompat.getParcelable(requireArguments(), "proposal", Proposal::class.java)
        if (proposal != null) {
            setupSystemBars()
            setupUI(proposal)
            setupSocialButtons(proposal)
            initButtonBack()
            setupOnBackPressedCallback()
        }
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

    private fun setupUI(proposal: Proposal) {
        binding.textUserName.text = proposal.username
        binding.textTime.text = formatTimeAgo(proposal.createdAt)
        binding.textContent.text = proposal.content

        Glide.with(this)
            .load(proposal.userAvatar)
            .placeholder(R.drawable.img_profile_placeholder)
            .transform(CircleCropTransformation())
            .into(binding.imageProfile)
    }

    private fun setupSocialButtons(proposal: Proposal) {
        profileViewModel.loadProposalAuthor(proposal.userId)
        profileViewModel.proposalAuthorLiveData.observe(viewLifecycleOwner) { user ->
            val telegramLink = user?.telegram_link ?: ""
            val instagramLink = user?.instagram_link ?: ""

            binding.buttonTelegram.setOnClickListener {
                if (telegramLink.isNotEmpty()) {
                    openCustomTab(telegramLink)
                }
            }

            binding.buttonInstagram.setOnClickListener {
                if (instagramLink.isNotEmpty()) {
                    openCustomTab(instagramLink)
                }
            }
        }
    }

    private fun initButtonBack() {
        binding.buttonBack.setOnClickListener {
            navigateBackToMainMenu()
        }
    }

    private fun setupOnBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToMainMenu()
            }
        })
    }

    private fun navigateBackToMainMenu() {
        val bundle = Bundle().apply {
            putString("navigateTo", "proposals")
        }
        findNavController().navigate(R.id.MainMenuFragment, bundle)
    }

    private fun openCustomTab(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
    }

    private fun formatTimeAgo(createdAt: OffsetDateTime): String {
        val now = OffsetDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(createdAt, now)
        val hours = ChronoUnit.HOURS.between(createdAt, now)
        val days = ChronoUnit.DAYS.between(createdAt, now)

        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MM/dd")
                createdAt.format(formatter)
            }
        }
    }
}