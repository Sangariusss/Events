package com.komiker.events.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.komiker.events.R
import com.komiker.events.databinding.FragmentCreateEventBinding
import com.komiker.events.ui.adapters.CreateEventViewPagerAdapter
import com.komiker.events.viewmodels.CreateEventViewModel

class CreateEventFragment : Fragment() {

    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var pagerAdapter: CreateEventViewPagerAdapter
    private var currentStep = 1
    private lateinit var darkOverlay: View
    private lateinit var progressBar: LottieAnimationView
    private val viewModel: CreateEventViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventBinding.inflate(inflater, container, false)
        if (savedInstanceState == null) {
            viewModel.clear()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        darkOverlay = binding.viewDarkOverlay
        progressBar = binding.lottieProgressBar
        restoreCurrentStep(savedInstanceState)
        viewModel.resetCleared()
        setupUi()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentStep", currentStep)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun restoreCurrentStep(savedInstanceState: Bundle?) {
        currentStep = savedInstanceState?.getInt("currentStep") ?: 1
    }

    private fun setupUi() {
        setupSystemBars()
        setupOnBackPressed()
        setupButtonClose()
        setupButtonBack()
        setupViewPager()
        setupProgressBar()
    }

    private fun setupViewPager() {
        initializeViewPager()
        configureViewPager()
    }

    private fun initializeViewPager() {
        pagerAdapter = CreateEventViewPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.setCurrentItem(currentStep - 1, false)
    }

    private fun configureViewPager() {
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
                binding.viewPager.setCurrentItem(binding.viewPager.currentItem - 1, true)
            } else {
                navigateToMainMenu()
            }
        }
    }

    private fun setupButtonClose() {
        binding.buttonClose.setOnClickListener {
            navigateToMainMenu()
        }
    }

    private fun setupSystemBars() {
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        requireActivity().window.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
    }

    private fun setupOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToMainMenu()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun navigateToMainMenu() {
        viewModel.images.forEach { it.file.delete() }
        viewModel.clear()
        findNavController().navigate(R.id.action_CreateEventFragment_to_MainMenuFragment)
    }

    fun startLottieAnimation() {
        requireActivity().window.apply {
            statusBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_0_30_percent)
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_0_30_percent)
        }
        progressBar.apply {
            visibility = View.VISIBLE
            setAnimation(R.raw.progress_bar)
            playAnimation()
        }
        darkOverlay.visibility = View.VISIBLE
    }

    fun stopLottieAnimation() {
        progressBar.apply {
            visibility = View.GONE
            cancelAnimation()
        }
        darkOverlay.visibility = View.GONE
        setupSystemBars()
    }
}