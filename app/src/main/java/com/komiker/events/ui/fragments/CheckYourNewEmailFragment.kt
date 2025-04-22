package com.komiker.events.ui.fragments

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.komiker.events.R
import com.komiker.events.databinding.FragmentCheckYourNewEmailBinding

class CheckYourNewEmailFragment : Fragment() {

    private var _binding: FragmentCheckYourNewEmailBinding? = null
    private val binding get() = _binding!!
    private var doubleBackToExitPressedOnce = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckYourNewEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSystemBars()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleDoubleBackPress()
        }
    }

    private fun handleDoubleBackPress() {
        if (doubleBackToExitPressedOnce) {
            requireActivity().finish()
            return
        }

        doubleBackToExitPressedOnce = true
        showExitSnackbar()

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

    private fun showExitSnackbar() {
        val customSnackbar =
            Snackbar.make(
                binding.root,
                getString(R.string.press_back_again_to_exit),
                Snackbar.LENGTH_SHORT
            )
        customizeSnackbarAppearance(customSnackbar)
        customSnackbar.show()
    }

    private fun customizeSnackbarAppearance(customSnackbar: Snackbar) {
        val snackbarView = customSnackbar.view
        val snackbarTextView: TextView =
            snackbarView.findViewById(com.google.android.material.R.id.snackbar_text)
        snackbarTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        snackbarTextView.setTextAppearance(R.style.SnackbarCustom)
        snackbarTextView.setTextColor(Color.BLACK)
        snackbarView.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun setupSystemBars() {
        val startStatusBarColor = requireActivity().window.statusBarColor
        val startNavigationBarColor = requireActivity().window.navigationBarColor

        val endStatusBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        val endNavigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)

        animateSystemBarColorChange(startStatusBarColor, endStatusBarColor) { color ->
            requireActivity().window.statusBarColor = color
        }

        animateSystemBarColorChange(startNavigationBarColor, endNavigationBarColor) { color ->
            requireActivity().window.navigationBarColor = color
        }
    }

    private fun animateSystemBarColorChange(
        startColor: Int,
        endColor: Int,
        onUpdate: (Int) -> Unit
    ) {
        ValueAnimator().apply {
            setObjectValues(startColor, endColor)
            setEvaluator(ArgbEvaluator())
            addUpdateListener { animator ->
                onUpdate(animator.animatedValue as Int)
            }
            duration = 200
            start()
        }
    }
}