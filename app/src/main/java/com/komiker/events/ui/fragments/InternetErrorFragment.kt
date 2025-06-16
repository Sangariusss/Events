package com.komiker.events.ui.fragments

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.databinding.FragmentInternetErrorBinding

class InternetErrorFragment : Fragment() {

    private var _binding: FragmentInternetErrorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInternetErrorBinding.inflate(inflater, container, false)
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
        binding.buttonRetry.setOnClickListener {
            if (isInternetAvailable()) {
                val fadeOutAnimation = R.anim.fade_out
                val fadeInAnimation = R.anim.fade_in

                findNavController().navigate(
                    R.id.action_InternetErrorFragment_to_CheckYourEmailFragment,
                    null,
                    NavOptions.Builder()
                        .setEnterAnim(fadeInAnimation)
                        .setExitAnim(fadeOutAnimation)
                        .build()
                )
            } else {
                Toast.makeText(requireContext(), R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
            }
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

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}