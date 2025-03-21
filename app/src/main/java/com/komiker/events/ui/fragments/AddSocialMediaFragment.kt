package com.komiker.events.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.databinding.FragmentAddSocialMediaBinding

class AddSocialMediaFragment : Fragment() {

    private var _binding: FragmentAddSocialMediaBinding? = null
    private val binding get() = _binding!!

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

    private fun setupOnBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToMainMenuWithProfile()
            }
        })
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