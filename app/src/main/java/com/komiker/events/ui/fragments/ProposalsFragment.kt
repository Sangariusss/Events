package com.komiker.events.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.ProposalLike
import com.komiker.events.data.database.models.Proposal
import com.komiker.events.data.database.models.ProposalResponse
import com.komiker.events.databinding.FragmentProposalsBinding
import com.komiker.events.glide.CircleCropTransformation
import com.komiker.events.ui.adapters.ProposalsAdapter
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProposalsFragment : Fragment() {

    private var _binding: FragmentProposalsBinding? = null
    private val binding get() = _binding!!

    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val profileViewModel: ProfileViewModel by activityViewModels { ProfileViewModelFactory(supabaseUserDao) }
    private lateinit var proposalsAdapter: ProposalsAdapter
    private lateinit var channel: RealtimeChannel
    private val likeCache = mutableMapOf<String, Boolean>()
    private val likesCountCache = mutableMapOf<String, Int>()
    private var heartbeatJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentProposalsBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonFilter()
        setupWriteProposalButton()
        setupUserProfile()
        setupRecyclerView()
        loadProposals()
        setupRealtimeUpdates()
        startHeartbeat()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        heartbeatJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            supabaseClient.realtime.removeChannel(channel)
            withContext(Dispatchers.Main) { _binding = null }
        }
    }

    private fun setupButtonFilter() {
        binding.buttonFilter.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_FilterFragment)
        }
    }

    private fun setupWriteProposalButton() {
        binding.buttonWriteProposal.setOnClickListener {
            findNavController().navigate(R.id.action_ProposalsFragment_to_CreateProposalFragment)
        }
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

    private fun setupRecyclerView() {
        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            val currentUserId = user?.user_id
            proposalsAdapter = ProposalsAdapter(
                currentUserId = currentUserId,
                onDeleteClicked = ::deleteProposal,
                navController = findNavController(),
                likeCache = likeCache,
                likesCountCache = likesCountCache,
                onLikeClicked = ::handleLike
            )
            binding.recyclerViewProposals.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = proposalsAdapter
            }
            loadProposals()
        }
    }

    private fun loadProposals() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = supabaseClient.from("proposals").select().decodeList<ProposalResponse>()
                val proposals = response.map { proposalResponse ->
                    Proposal(
                        id = proposalResponse.id,
                        userId = proposalResponse.userId,
                        username = proposalResponse.username,
                        userAvatar = proposalResponse.userAvatar,
                        content = proposalResponse.content,
                        createdAt = proposalResponse.createdAt,
                        likesCount = proposalResponse.likesCount
                    )
                }.sortedByDescending { it.createdAt }
                initializeCaches(proposals)
                proposalsAdapter.submitList(proposals)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun initializeCaches(proposals: List<Proposal>) {
        val currentUserId = profileViewModel.userLiveData.value?.user_id ?: return
        proposals.forEach { proposal ->
            if (!likeCache.containsKey(proposal.id)) {
                val isLiked = supabaseClient.from("proposal_likes")
                    .select { filter { eq("proposal_id", proposal.id); eq("user_id", currentUserId) } }
                    .decodeList<ProposalLike>()
                    .isNotEmpty()
                likeCache[proposal.id] = isLiked
            }
            likesCountCache[proposal.id] = proposal.likesCount
        }
    }

    private fun handleLike(proposalId: String, isLiked: Boolean, callback: (Boolean, Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isLiked) {
                    supabaseClient.from("proposal_likes").insert(
                        mapOf("proposal_id" to proposalId, "user_id" to profileViewModel.userLiveData.value?.user_id)
                    )
                } else {
                    supabaseClient.from("proposal_likes").delete {
                        filter { eq("proposal_id", proposalId); eq("user_id", profileViewModel.userLiveData.value?.user_id!!) }
                    }
                }
                val updatedProposal = supabaseClient.from("proposals")
                    .select { filter { eq("id", proposalId) } }
                    .decodeSingle<Proposal>()
                withContext(Dispatchers.Main) {
                    callback(true, updatedProposal.likesCount)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(false, 0)
                }
            }
        }
    }

    private fun deleteProposal(proposal: Proposal) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabaseClient.from("proposals").delete { filter { eq("id", proposal.id) } }
                withContext(Dispatchers.Main) {
                    val currentList = proposalsAdapter.currentList.toMutableList()
                    currentList.remove(proposal)
                    likeCache.remove(proposal.id)
                    likesCountCache.remove(proposal.id)
                    proposalsAdapter.submitList(currentList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupRealtimeUpdates() {
        channel = supabaseClient.channel("proposals-channel")
        val changeFlowProposals = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "proposals" }
        viewLifecycleOwner.lifecycleScope.launch {
            changeFlowProposals.collect { change ->
                val proposalResponse = change.decodeRecord<ProposalResponse>()
                val newProposal = Proposal(
                    id = proposalResponse.id,
                    userId = proposalResponse.userId,
                    username = proposalResponse.username,
                    userAvatar = proposalResponse.userAvatar,
                    content = proposalResponse.content,
                    createdAt = proposalResponse.createdAt,
                    likesCount = proposalResponse.likesCount
                )
                val currentList = proposalsAdapter.currentList.toMutableList()
                currentList.add(0, newProposal)
                initializeCaches(listOf(newProposal))
                proposalsAdapter.submitList(currentList)
            }
        }
        CoroutineScope(Dispatchers.IO).launch { channel.subscribe() }
    }

    private fun startHeartbeat() {
        heartbeatJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                delay(15000)
                updateLikesCountForAllProposals()
            }
        }
    }

    private suspend fun updateLikesCountForAllProposals() {
        withContext(Dispatchers.Main) {
            try {
                val currentList = proposalsAdapter.currentList
                if (currentList.isEmpty()) return@withContext

                val proposalIds = currentList.map { it.id }
                val updatedProposals = supabaseClient.from("proposals")
                    .select { filter { isIn("id", proposalIds) } }
                    .decodeList<Proposal>()

                updatedProposals.forEach { updatedProposal ->
                    val currentLikesCount = likesCountCache[updatedProposal.id] ?: return@forEach
                    if (currentLikesCount != updatedProposal.likesCount) {
                        likesCountCache[updatedProposal.id] = updatedProposal.likesCount
                        val index = currentList.indexOfFirst { it.id == updatedProposal.id }
                        if (index != -1) {
                            proposalsAdapter.notifyItemChanged(index, updatedProposal.likesCount)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}