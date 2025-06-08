package com.komiker.events.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.databinding.FragmentRegistrationSuccessBinding
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.gotrue.auth

class RegistrationSuccessFragment : Fragment() {

    private var _binding: FragmentRegistrationSuccessBinding? = null
    private val binding get() = _binding!!

    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOnBackPressed()
        setupButtonGetStarted()

        val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
        userId?.let { profileViewModel.loadUser(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, true) {
            requireActivity().finish()
        }
    }

    private fun setupButtonGetStarted() {
        binding.buttonGetStarted.setOnClickListener {
            val navController = findNavController()
            val navOptions = navOptions {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
            }
            navController.navigate(R.id.MainMenuFragment, null, navOptions)
        }
    }
}
