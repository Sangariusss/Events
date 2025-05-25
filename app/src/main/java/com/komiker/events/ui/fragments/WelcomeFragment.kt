package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.databinding.FragmentWelcomeBinding
import com.komiker.events.services.auth.FacebookAuthManager
import com.komiker.events.services.auth.GoogleAuthManager
import com.komiker.events.services.auth.TwitterAuthManager

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private val googleSignInManager: GoogleAuthManager by lazy {
        GoogleAuthManager()
    }

    private val facebookAuthManager: FacebookAuthManager by lazy {
        FacebookAuthManager()
    }

    private val twitterAuthManager: TwitterAuthManager by lazy {
        TwitterAuthManager()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {
        setupSystemBars()
        setupEmailButton()
        setupGoogleSignInButton()
        setupFacebookSignInButton()
        setupTwitterSignInButton()
        setupClickableAndUnderlinedText(binding.textTermsAndPrivacy)
    }

    private fun setupSystemBars() {
        requireActivity().window.apply {
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        }
    }

    private fun setupEmailButton() {
        binding.buttonContinueWithEmail.setOnClickListener {
            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            findNavController().navigate(
                R.id.action_WelcomeFragment_to_RegistrationFragment,
                null,
                NavOptions.Builder()
                    .setEnterAnim(fadeInAnimation)
                    .setExitAnim(fadeOutAnimation)
                    .build()
            )
        }
    }

    private fun setupGoogleSignInButton() {
        val googleSignInButton: Button = binding.buttonGoogle
        googleSignInManager.setupGoogleSignInButton(
            requireContext(),
            googleSignInButton,
            viewLifecycleOwner.lifecycleScope,
            findNavController()
        )
    }

    private fun setupFacebookSignInButton() {
        val facebookSignInButton: Button = binding.buttonFacebook
        facebookAuthManager.setupFacebookSignInButton(
            requireContext(),
            facebookSignInButton,
            viewLifecycleOwner.lifecycleScope,
        )
    }

    private fun setupTwitterSignInButton() {
        val twitterSignInButton: Button = binding.buttonX
        twitterAuthManager.setupTwitterSignInButton(
            requireContext(),
            twitterSignInButton,
            viewLifecycleOwner.lifecycleScope
        )
    }

    private fun setupClickableAndUnderlinedText(textView: TextView) {
        val text = "By continuing, you agree to Terms of Use and Privacy Policy"
        val spannableString = SpannableString(text)

        val termsColor = ContextCompat.getColor(requireContext(), R.color.red_60)
        val privacyColor = ContextCompat.getColor(requireContext(), R.color.red_60)

        val termsClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {

            }
        }

        val privacyClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {

            }
        }

        val termsStartIndex = text.indexOf("Terms of Use")
        val termsEndIndex = termsStartIndex + "Terms of Use".length
        spannableString.setSpan(termsClickableSpan, termsStartIndex, termsEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(UnderlineSpan(), termsStartIndex, termsEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(termsColor), termsStartIndex, termsEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val privacyStartIndex = text.indexOf("Privacy Policy")
        val privacyEndIndex = privacyStartIndex + "Privacy Policy".length
        spannableString.setSpan(privacyClickableSpan, privacyStartIndex, privacyEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(UnderlineSpan(), privacyStartIndex, privacyEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(privacyColor), privacyStartIndex, privacyEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString

        textView.movementMethod = LinkMovementMethod.getInstance()

        textView.highlightColor = ContextCompat.getColor(requireContext(), android.R.color.transparent)
    }
}