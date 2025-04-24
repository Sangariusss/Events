package com.komiker.events.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.komiker.events.R
import com.komiker.events.databinding.FragmentCreateEventBinding
import com.komiker.events.ui.adapters.CreateEventViewPagerAdapter

class CreateEventFragment : Fragment() {

    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: CreateEventViewPagerAdapter
    private var currentStep = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSystemBars()
        setupOnBackPressed()
        setupButtonClose()
        setupButtonBack()
        setupViewPager()
        setupProgressBar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViewPager() {
        pagerAdapter = CreateEventViewPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentStep = position + 1
                updateProgressBar()
            }
        })
    }

    private fun setupProgressBar() {
        updateProgressBar()
    }

    private fun updateProgressBar() {
        binding.progressSegment1.isActivated = currentStep == 1
        binding.progressSegment2.isActivated = currentStep == 2
        binding.progressSegment3.isActivated = currentStep == 3
    }

    private fun setupButtonBack() {
        binding.buttonBack.setOnClickListener {
            if (currentStep > 1) {
                val previousPage = binding.viewPager.currentItem - 1
                binding.viewPager.currentItem = previousPage
            } else {
                findNavController().navigate(R.id.action_CreateEventFragment_to_MainMenuFragment)
            }
        }
    }

    private fun setupSystemBars() {
        requireActivity().window.apply {
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        }
    }

    private fun setupOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_CreateEventFragment_to_MainMenuFragment)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setupButtonClose() {
        binding.buttonClose.setOnClickListener {
            findNavController().navigate(R.id.action_CreateEventFragment_to_MainMenuFragment)
        }
    }
}