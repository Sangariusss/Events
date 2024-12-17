package com.komiker.events.ui.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.databinding.FragmentRegistrationBinding
import com.komiker.events.services.auth.SignInWithEmail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistrationFragment : Fragment() {

    private var _binding: FragmentRegistrationBinding? = null
    private val binding get() = _binding!!

    private var emailEditText: EditText? = null
    private var progressBar: LottieAnimationView? = null
    private lateinit var darkOverlay: View
    private val supabaseClient = SupabaseClientProvider.client

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonContinue = view.findViewById<Button>(R.id.button_continue)
        progressBar = view.findViewById(R.id.lottie_progress_bar)
        darkOverlay = view.findViewById(R.id.view_dark_overlay)
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        setupEmailEditText(buttonContinue)
        setupButtonBack(imm)
        setupButtonContinue(buttonContinue, imm)
        setupKeyboardVisibilityListener(view)

        emailEditText?.postDelayed({
            emailEditText?.requestFocus()
            imm.showSoftInput(emailEditText, InputMethodManager.SHOW_IMPLICIT)
        }, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupEmailEditText(buttonContinue: Button) {
        emailEditText = requireView().findViewById(R.id.edit_text_email)
        emailEditText!!.filters = arrayOf(noSpaceFilter)
        emailEditText!!.requestFocus()

        emailEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do here
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Nothing to do here
            }

            override fun afterTextChanged(s: Editable) {
                val hint = emailEditText!!.hint

                if (s.isEmpty() || s == hint) {
                    setEditTextState(emailEditText!!, true)
                    buttonContinue.setBackgroundResource(R.drawable.bg_btn_rounded_black_inactive)
                    buttonContinue.isEnabled = false
                } else if (!isValidEmail(s)) {
                    setEditTextState(emailEditText!!, false)
                    buttonContinue.setBackgroundResource(R.drawable.bg_btn_rounded_black_inactive)
                    buttonContinue.isEnabled = false
                } else {
                    setEditTextState(emailEditText!!, true)
                    buttonContinue.setBackgroundResource(R.drawable.bg_btn_rounded_black)
                    buttonContinue.isEnabled = true
                }
            }
        })
    }

    private fun setupButtonBack(imm: InputMethodManager) {
        binding.buttonBack.setOnClickListener {
            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            imm.hideSoftInputFromWindow(requireView().windowToken, 0)

            findNavController().navigate(
                R.id.action_RegistrationFragment_to_WelcomeFragment,
                null,
                NavOptions.Builder()
                    .setEnterAnim(fadeInAnimation)
                    .setExitAnim(fadeOutAnimation)
                    .build()
            )
        }
    }

    private fun setupButtonContinue(buttonContinue: Button, imm: InputMethodManager) {
        binding.buttonContinue.setOnClickListener {
            buttonContinue.isEnabled = false
            buttonContinue.setBackgroundResource(R.drawable.bg_btn_rounded_black_inactive)
            emailEditText?.isEnabled = false

            startLottieAnimation()

            if (!isNetworkAvailable(requireContext())) {
                findNavController().navigate(
                    R.id.action_RegistrationFragment_to_InternetErrorFragment,
                    null,
                    NavOptions.Builder()
                        .setEnterAnim(R.anim.fade_in)
                        .setExitAnim(R.anim.fade_out)
                        .build()
                )
                buttonContinue.isEnabled = true
                buttonContinue.setBackgroundResource(R.drawable.bg_btn_rounded_black)
                emailEditText?.isEnabled = true
                startLottieAnimation()
                return@setOnClickListener
            }

            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            imm.hideSoftInputFromWindow(requireView().windowToken, 0)

            CoroutineScope(Dispatchers.Main).launch {
                val signInSuccess = withContext(Dispatchers.IO) {
                    signingIn()
                }

                startLottieAnimation()

                if (signInSuccess) {
                    val bundle = Bundle().apply {
                        putString("email", emailEditText!!.text.toString())
                    }

                    findNavController().navigate(
                        R.id.action_RegistrationFragment_to_CheckYourEmailFragment,
                        bundle,
                        NavOptions.Builder()
                            .setEnterAnim(fadeInAnimation)
                            .setExitAnim(fadeOutAnimation)
                            .build()
                    )
                } else {
                    findNavController().navigate(
                        R.id.action_RegistrationFragment_to_RegistrationErrorFragment,
                        null,
                        NavOptions.Builder()
                            .setEnterAnim(fadeInAnimation)
                            .setExitAnim(fadeOutAnimation)
                            .build()
                    )
                }

                buttonContinue.isEnabled = true
                buttonContinue.setBackgroundResource(R.drawable.bg_btn_rounded_black)
                emailEditText?.isEnabled = true
            }
        }
    }

    private fun startLottieAnimation() {
        setupSystemBars()

        progressBar?.visibility = View.VISIBLE
        darkOverlay.visibility = View.VISIBLE

        progressBar?.setAnimation(R.raw.progress_bar)
        progressBar?.playAnimation()
    }

    private fun setupSystemBars() {
        requireActivity().window.apply {
            statusBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_30_70_percent)
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_30_70_percent)
        }
    }

    private fun setupKeyboardVisibilityListener(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            if (imeVisible) {
                val offset = imeHeight / 1.07
                val progressBarOffset = imeHeight / 2
                binding.buttonContinue.translationY = -offset.toFloat()
                binding.textTermsAndPrivacy.translationY = -offset.toFloat()
                progressBar?.translationY = -progressBarOffset.toFloat()
            } else {
                binding.buttonContinue.translationY = 0f
                binding.textTermsAndPrivacy.translationY = 0f
                progressBar?.translationY = 0f
            }

            insets
        }
    }

    private fun setEditTextState(emailEditText: EditText, isValid: Boolean) {
        if (!isValid) {
            emailEditText.setBackgroundResource(R.drawable.bg_et_registration_error)
            emailEditText.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_text_edit_error,
                0
            )
            emailEditText.setTextAppearance(R.style.EditTextRegistrationError)
        } else {
            if (emailEditText.text.toString().isEmpty()) {
                emailEditText.setBackgroundResource(R.drawable.bg_et_registration)
            } else {
                emailEditText.setBackgroundResource(R.drawable.bg_et_registration_correct)
            }
            emailEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            emailEditText.setTextAppearance(R.style.EditTextRegistrationCustom)
        }
    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target!!).matches()
    }

    private val noSpaceFilter = InputFilter { source, _, _, _, _, _ ->
        for (i in source.indices) {
            if (Character.isWhitespace(source[i])) {
                return@InputFilter ""
            }
        }
        null
    }

    private suspend fun signingIn(): Boolean {
        emailEditText = requireView().findViewById(R.id.edit_text_email)
        val signInWithEmail = SignInWithEmail(supabaseClient, emailEditText!!.text.toString())
        return signInWithEmail.signIn()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}