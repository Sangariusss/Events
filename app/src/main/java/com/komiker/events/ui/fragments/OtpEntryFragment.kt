package com.komiker.events.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.databinding.FragmentOtpEntryBinding
import com.komiker.events.services.auth.SignInWithEmail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtpEntryFragment : Fragment() {

    private var _binding: FragmentOtpEntryBinding? = null
    private val binding get() = _binding!!

    private lateinit var otpEditTexts: Array<EditText>
    private lateinit var verifyOtpButton: Button
    private lateinit var email: String
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtonBack()
        initializeViews()
        setupVerifyOtpButton()
        setupOtpInputs()
        setupKeyboardListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        globalLayoutListener?.let {
            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(it)
            globalLayoutListener = null
        }
        _binding = null
    }

    private fun setupButtonBack() {
        binding.buttonBack.setOnClickListener {
            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            findNavController().navigate(
                R.id.action_OtpEntryFragment_to_CheckYourEmailFragment,
                null,
                NavOptions.Builder()
                    .setEnterAnim(fadeInAnimation)
                    .setExitAnim(fadeOutAnimation)
                    .build()
            )
        }
    }

    private fun initializeViews() {
        otpEditTexts = arrayOf(
            binding.root.findViewById(R.id.edit_text_digit_1),
            binding.root.findViewById(R.id.edit_text_digit_2),
            binding.root.findViewById(R.id.edit_text_digit_3),
            binding.root.findViewById(R.id.edit_text_digit_4),
            binding.root.findViewById(R.id.edit_text_digit_5),
            binding.root.findViewById(R.id.edit_text_digit_6)
        )
        verifyOtpButton = binding.root.findViewById(R.id.button_confirm)
        email = arguments?.getString("email") ?: ""
    }

    private fun setupVerifyOtpButton() {
        verifyOtpButton.setOnClickListener {
            val otp = otpEditTexts.joinToString("") { it.text.toString() }
            if (otp.length == 6) {
                verifyOtp(otp)
            } else {
                Toast.makeText(requireContext(), "Please enter the 6-digit OTP", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setupOtpInputs() {
        for (i in otpEditTexts.indices) {
            otpEditTexts[i].apply {
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (s?.length == 1) {
                            setBackgroundResource(R.drawable.bg_digit_box_focused)
                            if (i < otpEditTexts.size - 1) {
                                otpEditTexts[i + 1].requestFocus()
                            }
                        } else if (s?.isEmpty() == true) {
                            setBackgroundResource(R.drawable.bg_digit_box_unfocused)
                            if (i > 0) {
                                otpEditTexts[i - 1].requestFocus()
                            }
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })

                onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        setBackgroundResource(R.drawable.bg_digit_box_focused)
                    } else if (text.isEmpty()) {
                        setBackgroundResource(R.drawable.bg_digit_box_unfocused)
                    }
                }
            }
        }

        for (i in otpEditTexts.indices) {
            otpEditTexts[i].setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    if (otpEditTexts[i].text.isEmpty() && i > 0) {
                        otpEditTexts[i - 1].requestFocus()
                    }
                }
                false
            }
        }
    }

    private fun verifyOtp(token: String) {
        val supabaseClient = SupabaseClientProvider.client
        CoroutineScope(Dispatchers.Main).launch {
            val signInWithEmail = SignInWithEmail(supabaseClient, email)
            val success = withContext(Dispatchers.IO) {
                signInWithEmail.verifyOtp(token)
            }
            if (success) {
                findNavController().navigate(R.id.action_OtpEntryFragment_to_RegistrationSuccessFragment)
            } else {
                otpEditTexts.forEach { editText ->
                    editText.setBackgroundResource(R.drawable.bg_digit_box_error)
                }
                Toast.makeText(requireContext(), "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupKeyboardListener() {
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            _binding?.let { binding ->
                val rect = android.graphics.Rect()
                binding.root.getWindowVisibleDisplayFrame(rect)
                val screenHeight = binding.root.height
                val keypadHeight = screenHeight - rect.bottom
                val isKeyboardShown = keypadHeight > screenHeight * 0.15
                adjustDigitsForKeyboard(isKeyboardShown, keypadHeight)
            }
        }
        _binding?.root?.viewTreeObserver?.addOnGlobalLayoutListener(globalLayoutListener)
    }

    private fun adjustDigitsForKeyboard(isKeyboardShown: Boolean, keypadHeight: Int) {
        val screenHeight = binding.root.height.toFloat()
        val offset = screenHeight * 0.075f

        val translationY = if (isKeyboardShown) {
            val keypadHeightFloat = keypadHeight.toFloat()
            val digitsBottom = otpEditTexts.last().bottom.toFloat()
            val keyboardTop = (screenHeight - keypadHeightFloat)
            val requiredShift = digitsBottom - keyboardTop + offset
            if (requiredShift > 0) {
                -requiredShift
            } else {
                0f
            }
        } else {
            0f
        }

        binding.imageSayMyCode.animate()
            .translationY(translationY)
            .setDuration(100)
            .start()

        binding.textCodeTitle.animate()
            .translationY(translationY)
            .setDuration(100)
            .start()

        binding.textCodeDescription.animate()
            .translationY(translationY)
            .setDuration(100)
            .start()

        otpEditTexts.forEach { editText ->
            editText.animate()
                .translationY(translationY)
                .setDuration(100)
                .start()
        }
    }
}