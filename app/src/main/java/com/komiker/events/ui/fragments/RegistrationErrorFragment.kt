package com.komiker.events.ui.fragments

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.databinding.FragmentRegistrationErrorBinding

class RegistrationErrorFragment : Fragment() {

    private var _binding: FragmentRegistrationErrorBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationErrorBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSystemBars()

        setupButtonTryAgain()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupButtonTryAgain() {
        binding.buttonTryAgain.setOnClickListener {
            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            findNavController().navigate(
                R.id.action_RegistrationErrorFragment_to_RegistrationFragment,
                null,
                NavOptions.Builder()
                    .setEnterAnim(fadeInAnimation)
                    .setExitAnim(fadeOutAnimation)
                    .build()
            )
        }
    }

    private fun setupSystemBars() {
        val startStatusBarColor = requireActivity().window.statusBarColor
        val startNavigationBarColor = requireActivity().window.navigationBarColor

        val endStatusBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        val endNavigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)

        val navigationBarColorAnimator = ValueAnimator().apply {
            setObjectValues(startNavigationBarColor, endNavigationBarColor)
            setEvaluator(ArgbEvaluator())
            addUpdateListener { animator ->
                val animatedValue = animator.animatedValue as Int
                requireActivity().window.navigationBarColor = animatedValue
            }
            duration = 200
        }

        val statusBarColorAnimator = ValueAnimator().apply {
            setObjectValues(startStatusBarColor, endStatusBarColor)
            setEvaluator(ArgbEvaluator())
            addUpdateListener { animator ->
                val animatedValue = animator.animatedValue as Int
                requireActivity().window.statusBarColor = animatedValue
            }
            duration = 200
        }

        navigationBarColorAnimator.start()
        statusBarColorAnimator.start()
    }
}