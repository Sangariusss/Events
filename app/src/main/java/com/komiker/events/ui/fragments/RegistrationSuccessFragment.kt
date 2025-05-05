package com.komiker.events.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.databinding.FragmentRegistrationSuccessBinding
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.gotrue.auth

class RegistrationSuccessFragment : Fragment() {

    private var _binding: FragmentRegistrationSuccessBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val profileViewModel: ProfileViewModel by activityViewModels  {
        ProfileViewModelFactory(supabaseUserDao)
    }
    private var doubleBackToExitPressedOnce = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtonGetStarted()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
        userId?.let { profileViewModel.loadUser(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupButtonGetStarted() {
        binding.buttonGetStarted.setOnClickListener {
            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            findNavController().navigate(
                R.id.action_RegistrationSuccessFragment_to_MainMenuFragment,
                null,
                NavOptions.Builder()
                    .setEnterAnim(fadeInAnimation)
                    .setExitAnim(fadeOutAnimation)
                    .build()
            )
        }
    }

    private fun showExitSnackbar() {
        val customSnackbar = Snackbar.make(binding.root, "Press back again to exit", Snackbar.LENGTH_SHORT)
        val snackbarView = customSnackbar.view
        val snackbarTextView: TextView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text)
        snackbarTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        snackbarTextView.setTextAppearance(R.style.SnackbarCustom)
        snackbarTextView.setTextColor(Color.BLACK)
        snackbarView.setBackgroundColor(Color.TRANSPARENT)
        customSnackbar.show()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (doubleBackToExitPressedOnce) {
                requireActivity().finishAffinity()
                return
            }

            doubleBackToExitPressedOnce = true
            showExitSnackbar()

            Handler(Looper.getMainLooper()).postDelayed({
                doubleBackToExitPressedOnce = false
            }, 2000)
        }
    }
}
