package com.komiker.events.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.SupabaseUserDao
import com.komiker.events.databinding.FragmentProposalsBinding
import com.komiker.events.glide.CircleCropTransformation
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory

class ProposalsFragment : Fragment() {

    private var _binding: FragmentProposalsBinding? = null
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
        _binding = FragmentProposalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonFilter()
        setupWriteProposalButton()
        setupUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                binding.textName.text = user.name

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
                binding.textName.text = getString(R.string.example_name)
                binding.imageProfile.setImageResource(R.drawable.img_profile_placeholder)
            }
        }
    }
}