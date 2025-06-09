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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProposalDetailFragment : Fragment() {

    private var _binding: FragmentProposalDetailBinding? = null
    private val binding get() = _binding!!
    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }
    private var currentProposal: Proposal? = null
    private val likeCache = mutableMapOf<String, Boolean>()
    private val likesCountCache = mutableMapOf<String, Int>()
    private var isProcessingLike = false
    private var likeJob: Job? = null

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
        setupButtonBack()
        setupOnBackPressedCallback()
    }

    override fun onDestroyView() {
        likeJob?.cancel()
        likeJob = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupSystemBars() {
        requireActivity().window.apply {
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        }
    }

    private fun handleArguments() {
        val proposal = BundleCompat.getParcelable(requireArguments(), "proposal", Proposal::class.java)
        val proposalId = arguments?.getString("proposalId")
        val isLiked = arguments?.getBoolean("isLiked", false) ?: false
        val likesCount = arguments?.getInt("likesCount", proposal?.likesCount ?: 0) ?: 0

        if (proposal != null) {
            currentProposal = proposal
            likeCache[proposal.id] = isLiked
            likesCountCache[proposal.id] = likesCount
            setupUI(proposal)
            setupLikeButton(proposal)
            viewLifecycleOwner.lifecycleScope.launch {
                initializeCaches(proposal)
                setupSocialButtons(proposal)
                setupShareButton()
            }
        } else if (proposalId != null) {
            loadProposalById(proposalId)
        }
    }

    private fun loadProposalById(proposalId: String) {
        profileViewModel.loadProposalById(proposalId).observe(viewLifecycleOwner) { proposal ->
            proposal?.let {
                currentProposal = it
                likesCountCache[proposal.id] = arguments?.getInt("likesCount", proposal.likesCount) ?: proposal.likesCount
                setupUI(it)
                viewLifecycleOwner.lifecycleScope.launch {
                    initializeCaches(it)
                    setupSocialButtons(it)
                    setupShareButton()
                    setupLikeButton(it)
                }
            } ?: run {
                if (isAdded) {
                    findNavController().navigate(R.id.MainMenuFragment)
                }
            }
        }
    }

    private suspend fun initializeCaches(proposal: Proposal) {
        val currentUserId = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: return
        if (!likeCache.containsKey(proposal.id)) {
            val isLiked = supabaseUserDao.isProposalLiked(proposal.id, currentUserId)
            likeCache[proposal.id] = isLiked
        }
    }

    private fun setupUI(proposal: Proposal) {
        binding.textUserName.text = proposal.username
        binding.textTime.text = formatTimeAgo(proposal.createdAt)
        binding.textContent.text = proposal.content
        binding.textLikesCount.text = formatLikesCount(likesCountCache[proposal.id] ?: proposal.likesCount)

        Glide.with(this)
            .load(proposal.userAvatar)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
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

    private fun setupLikeButton(proposal: Proposal) {
        binding.imageLike.setImageResource(
            if (likeCache[proposal.id] == true) R.drawable.ic_heart else R.drawable.ic_heart_outline
        )

        binding.imageLike.setOnClickListener {
            val currentUserId = supabaseClient.auth.currentSessionOrNull()?.user?.id
            if (currentUserId != null && !isProcessingLike) {
                isProcessingLike = true
                binding.imageLike.isEnabled = false
                binding.imageLike.alpha = 0.5f

                val isCurrentlyLiked = likeCache[proposal.id] ?: false
                val currentLikesCount = likesCountCache[proposal.id] ?: proposal.likesCount

                if (isCurrentlyLiked) {
                    val newLikesCount = currentLikesCount - 1
                    binding.textLikesCount.text = formatLikesCount(newLikesCount.coerceAtLeast(0))
                    binding.imageLike.setImageResource(R.drawable.ic_heart_outline)
                    likeCache[proposal.id] = false
                    likesCountCache[proposal.id] = newLikesCount

                    profileViewModel.unlikeProposal(proposal.id, currentUserId) { success, serverLikesCount ->
                        likeJob = lifecycleScope.launch {
                            isProcessingLike = false
                            if (isAdded) {
                                _binding?.let {
                                    it.imageLike.isEnabled = true
                                    it.imageLike.alpha = 1.0f
                                    if (!success) {
                                        it.textLikesCount.text = formatLikesCount(currentLikesCount)
                                        it.imageLike.setImageResource(R.drawable.ic_heart)
                                        likeCache[proposal.id] = true
                                        likesCountCache[proposal.id] = currentLikesCount
                                    } else {
                                        likesCountCache[proposal.id] = serverLikesCount
                                        it.textLikesCount.text = formatLikesCount(serverLikesCount)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val newLikesCount = currentLikesCount + 1
                    binding.textLikesCount.text = formatLikesCount(newLikesCount)
                    binding.imageLike.setImageResource(R.drawable.ic_heart)
                    likeCache[proposal.id] = true
                    likesCountCache[proposal.id] = newLikesCount

                    profileViewModel.likeProposal(proposal.id, currentUserId) { success, serverLikesCount ->
                        likeJob = lifecycleScope.launch {
                            isProcessingLike = false
                            if (isAdded) {
                                _binding?.let {
                                    it.imageLike.isEnabled = true
                                    it.imageLike.alpha = 1.0f
                                    if (!success) {
                                        it.textLikesCount.text = formatLikesCount(currentLikesCount)
                                        it.imageLike.setImageResource(R.drawable.ic_heart_outline)
                                        likeCache[proposal.id] = false
                                        likesCountCache[proposal.id] = currentLikesCount
                                    } else {
                                        likesCountCache[proposal.id] = serverLikesCount
                                        it.textLikesCount.text = formatLikesCount(serverLikesCount)
                                    }
                                }
                            }
                        }
                    }
                }
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

    private fun setupButtonBack() {
        binding.buttonBack.setOnClickListener {
            navigateBack()
        }
    }

    private fun setupOnBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, true) {
            navigateBack()
        }
    }

    private fun navigateBack() {
        val navController = findNavController()
        val isAuthenticated = supabaseClient.auth.currentSessionOrNull() != null

        val navOptions = navOptions {
            popUpTo(R.id.nav_graph) {
                inclusive = true
            }
        }

        if (isAuthenticated) {
            val bundle = Bundle().apply {
                putString("navigateTo", "proposals")
            }
            navController.navigate(R.id.MainMenuFragment, bundle, navOptions)
        } else {
            navController.navigate(R.id.WelcomeFragment, null, navOptions)
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

    private fun formatLikesCount(count: Int): String {
        return when {
            count >= 999_999_950 -> {
                val billions = count / 1_000_000_000.0
                String.format(Locale.US, "%.1fB", billions)
            }
            count >= 999_950 -> {
                val millions = count / 1_000_000.0
                String.format(Locale.US, "%.1fM", millions)
            }
            count >= 1_000 -> {
                val thousands = count / 1_000.0
                String.format(Locale.US, "%.1fK", thousands)
            }
            else -> count.toString()
        }
    }
}