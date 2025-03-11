package com.komiker.events.ui.fragments

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.komiker.events.R
import com.komiker.events.databinding.FragmentCheckYourEmailBinding

class CheckYourEmailFragment : Fragment() {

    private var _binding: FragmentCheckYourEmailBinding? = null
    private val binding get() = _binding!!
    private var timer: CountDownTimer? = null
    private var isTimerRunning = false
    private var doubleBackToExitPressedOnce = false
    private var email: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckYourEmailBinding.inflate(inflater, container, false)
        setupClickableSpan()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSystemBars()
        setupContinueButton()
        setupInitialViewVisibility()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    private fun setupClickableSpan() {
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                if (!isTimerRunning) {
                    startTimer()
                }
            }
        }

        val spannableString = SpannableString(getString(R.string.click_to_resend))
        spannableString.setSpan(
            clickableSpan,
            0,
            spannableString.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.textClickToResend.text = spannableString
        binding.textClickToResend.isClickable = true
        binding.textClickToResend.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupContinueButton() {
        binding.buttonContinue.setOnClickListener {
            navigateToOtpEntryFragment()
        }
    }

    private fun navigateToOtpEntryFragment() {
        val fadeOutAnimation = R.anim.fade_out
        val fadeInAnimation = R.anim.fade_in

        email = arguments?.getString("email")

        val bundle = Bundle().apply {
            putString("email", email)
        }
        findNavController().navigate(
            R.id.action_CheckYourEmailFragment_to_OtpEntryFragment,
            bundle,
            NavOptions.Builder()
                .setEnterAnim(fadeInAnimation)
                .setExitAnim(fadeOutAnimation)
                .build()
        )
    }

    private fun setupInitialViewVisibility() {
        binding.textTimer.visibility = View.GONE
    }

    private fun startTimer() {
        isTimerRunning = true
        binding.textClickToResend.text = getString(R.string.click_to_resend)
        binding.textClickToResend.isClickable = false
        binding.textTimer.visibility = View.VISIBLE
        binding.textTimer.text = getString(R.string._60)

        timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerText(millisUntilFinished)
            }

            override fun onFinish() {
                resetTimerView()
                showEmailCheckMessage()
            }
        }
        timer?.start()
    }

    private fun updateTimerText(millisUntilFinished: Long) {
        val secondsLeft = millisUntilFinished / 1000
        binding.textTimer.text = "$secondsLeft"
    }

    private fun resetTimerView() {
        binding.textTimer.visibility = View.GONE
        binding.textClickToResend.text = getString(R.string.click_to_resend)
        binding.textClickToResend.isClickable = true
        isTimerRunning = false
    }

    private fun showEmailCheckMessage() {
        binding.textClickToResend.text = getString(R.string.check_the_correctness_of_the_email)
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