package com.komiker.events.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.komiker.events.R
import com.komiker.events.databinding.FragmentMainMenuBinding


class MainMenuFragment : Fragment() {

    private var _binding: FragmentMainMenuBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private var currentLottieDrawable: LottieDrawable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigationView: BottomNavigationView = view.findViewById(R.id.bottom_navigation)

        setupIconSize(bottomNavigationView)
        disableLongClickTooltips(bottomNavigationView)
        setupSystemBars()

        if (savedInstanceState == null) {
            handleNavigationArguments()
        }

        setupNavigationItemSelectedListener(bottomNavigationView)
        setupButtonCreateEvent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSystemBars() {
        requireActivity().window.apply {
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_98)
        }
    }

    private fun setupIconSize(bottomNavigationView: BottomNavigationView) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()

        val iconSize = (screenWidth * 0.083f).toInt()
        bottomNavigationView.itemIconSize = iconSize
    }

    private fun disableLongClickTooltips(bottomNavigationView: BottomNavigationView) {
        val menuView = bottomNavigationView.getChildAt(0) as ViewGroup
        for (i in 0 until menuView.childCount) {
            val itemView = menuView.getChildAt(i)
            itemView.setOnLongClickListener { true }
            itemView.isHapticFeedbackEnabled = false
        }
    }

    private fun handleNavigationArguments() {
        val navigateTo = arguments?.getString("navigateTo")
        when (navigateTo) {
            "profile" -> {
                binding.bottomNavigation.selectedItemId = R.id.navigation_profile
                replaceFragment(ProfileFragment())
            }
            else -> {
                binding.bottomNavigation.selectedItemId = R.id.navigation_home
                replaceFragment(HomeFragment())
            }
        }
    }

    private fun setupNavigationItemSelectedListener(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    stopAllAnimations()
                    playAnimation(R.id.navigation_home, R.raw.anim_navigation_home_icon, R.drawable.ic_home)
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navigation_calendar -> {
                    stopAllAnimations()
                    playAnimation(R.id.navigation_calendar, R.raw.anim_navigation_calendar_icon, R.drawable.ic_calendar)
                    replaceFragment(CalendarFragment())
                    true
                }
                R.id.navigation_compass -> {
                    stopAllAnimations()
                    playAnimation(R.id.navigation_compass, R.raw.anim_navigation_compass_icon, R.drawable.ic_compass)
                    replaceFragment(CompassFragment())
                    true
                }
                R.id.navigation_profile -> {
                    stopAllAnimations()
                    playAnimation(R.id.navigation_profile, R.raw.anim_navigation_user_icon, R.drawable.ic_profile)
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun playAnimation(menuItemId: Int, animationResId: Int, defaultIconResId: Int) {
        val menuItem = binding.bottomNavigation.menu.findItem(menuItemId)

        currentLottieDrawable?.cancelAnimation()

        val lottieDrawable = LottieDrawable()

        LottieCompositionFactory.fromRawRes(requireContext(), animationResId)
            .addListener { result ->
                lottieDrawable.composition = result
                lottieDrawable.repeatCount = 0
                lottieDrawable.playAnimation()

                menuItem.icon = lottieDrawable
            }
            .addFailureListener {
                menuItem.icon = ContextCompat.getDrawable(requireContext(), defaultIconResId)
            }

        currentLottieDrawable = lottieDrawable
    }

    private fun stopAllAnimations() {
        currentLottieDrawable?.cancelAnimation()
        resetMenuIcons()
    }

    private fun resetMenuIcons() {
        binding.bottomNavigation.menu.findItem(R.id.navigation_home).icon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_home)
        binding.bottomNavigation.menu.findItem(R.id.navigation_calendar).icon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_calendar)
        binding.bottomNavigation.menu.findItem(R.id.navigation_compass).icon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_compass)
        binding.bottomNavigation.menu.findItem(R.id.navigation_profile).icon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_profile)
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun setupButtonCreateEvent() {
        binding.buttonCreateEvent.setOnClickListener {
            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            findNavController().navigate(
                R.id.action_MainMenuFragment_to_CreateEventFragment,
                null,
                NavOptions.Builder()
                    .setEnterAnim(fadeInAnimation)
                    .setExitAnim(fadeOutAnimation)
                    .build()
            )
        }
    }
}