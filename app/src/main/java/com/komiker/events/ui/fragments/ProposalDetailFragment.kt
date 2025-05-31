package com.komiker.events.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.bumptech.glide.Glide
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.Proposal
import com.komiker.events.databinding.FragmentProposalDetailBinding
import com.komiker.events.glide.CircleCropTransformation
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.gotrue.auth
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
    private var currentProposal: Proposal? = null

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
        setupSystemBars()
        handleArguments()
        initButtonBack()
        setupOnBackPressedCallback()
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

    private fun handleArguments() {
        val proposal = BundleCompat.getParcelable(requireArguments(), "proposal", Proposal::class.java)
        val proposalId = arguments?.getString("proposalId")

        if (proposal != null) {
            currentProposal = proposal
            setupUI(proposal)
            setupSocialButtons(proposal)
            setupShareButton()
        } else if (proposalId != null) {
            loadProposalById(proposalId)
        }
    }

    private fun loadProposalById(proposalId: String) {
        profileViewModel.loadProposalById(proposalId).observe(viewLifecycleOwner) { proposal ->
            proposal?.let {
                currentProposal = it
                setupUI(it)
                setupSocialButtons(it)
                setupShareButton()
            } ?: run {
                if (isAdded) {
                    findNavController().navigate(R.id.MainMenuFragment)
                }
            }
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

    private fun setupShareButton() {
        binding.buttonShare.setOnClickListener {
            currentProposal?.let { proposal ->
                shareProposal(proposal)
            }
        }
    }

    private fun shareProposal(proposal: Proposal) {
        val username = proposal.username.replace(" ", "_")
        val deepLink = "https://excito.netlify.app/@$username/proposal/${proposal.id}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, deepLink)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun initButtonBack() {
        binding.buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupOnBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, true) {
            val isAuthenticated = supabaseClient.auth.currentSessionOrNull() != null
            val navController = findNavController()

            val navOptionsToRoot = navOptions {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }

            if (isAuthenticated) {
                val bundle = Bundle().apply {
                    putString("navigateTo", "proposals")
                }
                navController.navigate(R.id.MainMenuFragment, bundle, navOptionsToRoot)
            } else {
                navController.navigate(R.id.WelcomeFragment, null, navOptionsToRoot)
            }
        }
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