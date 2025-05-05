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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProposalsFragment : Fragment() {

    private var _binding: FragmentProposalsBinding? = null
    private val binding get() = _binding!!

    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)

    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }

    private lateinit var proposalsAdapter: ProposalsAdapter
    private lateinit var channel: RealtimeChannel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProposalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonFilter()
        setupWriteProposalButton()
        setupUserProfile()
        setupRecyclerView()
        loadProposals()
        setupRealtimeUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        CoroutineScope(Dispatchers.IO).launch {
            supabaseClient.realtime.removeChannel(channel)
            withContext(Dispatchers.Main) {
                _binding = null
            }
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
        proposalsAdapter = ProposalsAdapter()
        binding.recyclerViewProposals.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = proposalsAdapter
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
                proposalsAdapter.submitList(proposals)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupRealtimeUpdates() {
        channel = supabaseClient.channel("proposals-channel")

        val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "proposals"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            changeFlow.collect { change ->
                println("Received Realtime update: $change")
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
                proposalsAdapter.submitList(currentList)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            channel.subscribe()
        }
    }
}