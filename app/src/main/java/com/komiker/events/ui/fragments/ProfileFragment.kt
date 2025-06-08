package com.komiker.events.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.User
import com.komiker.events.databinding.FragmentProfileBinding
import com.komiker.events.glide.CircleCropTransformation
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
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
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                updateUIWithUserData(user)
            } else {
                handleEmptyUserData()
            }
        }

        initButtonAddSocialMedia()
        initButtonEditProfile()
        initButtonChangeEmail()
        initButtonMyEvents()
        initButtonLogOut()
        initButtonDelete()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUIWithUserData(user: User) {
        binding.textProfileName.text = user.name
        binding.textProfileEmail.text = user.email

        Glide.with(this)
            .load(user.avatar)
            .override(400, 400)
            .signature(ObjectKey(System.currentTimeMillis().toString()))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .placeholder(R.drawable.img_profile_placeholder)
            .transform(CircleCropTransformation())
            .into(binding.imageProfile)
    }

    private fun handleEmptyUserData() {
        binding.textProfileName.text = getString(R.string.user_not_found)
        binding.textProfileEmail.text = ""
        binding.imageProfile.setImageResource(R.drawable.img_profile_placeholder)
    }

    private fun initButtonAddSocialMedia() {
        binding.constraintAddSocialMediaButtonLayout.setOnClickListener {
            findNavController().navigate(R.id.action_ProfileFragment_to_AddSocialMediaFragment)
        }
    }

    private fun initButtonEditProfile() {
        binding.constraintEditProfileButtonLayout.setOnClickListener {
            findNavController().navigate(R.id.action_ProfileFragment_to_EditProfileFragment)
        }
    }

    private fun initButtonChangeEmail() {
        binding.constraintChangeEmailButtonLayout.setOnClickListener {
            findNavController().navigate(R.id.action_ProfileFragment_to_ChangeEmailFragment)
        }
    }

    private fun initButtonMyEvents() {
        binding.constraintMyEventsButtonLayout.setOnClickListener {
            findNavController().navigate(R.id.action_ProfileFragment_to_MyEventsFragment)
        }
    }

    private fun initButtonLogOut() {
        binding.constraintLogOutButtonLayout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                supabaseClient.auth.signOut()
                navigateToWelcomeAndClearStack()
            }
        }
    }

    private fun initButtonDelete() {
        binding.constraintDeleteAccountButtonLayout.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showDeleteAccountDialog() {
        val tempRoot = FrameLayout(requireContext())

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_account, tempRoot, false)

        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        dialogView.findViewById<Button>(R.id.button_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.button_submit).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
                userId?.let {
                    supabaseClient.from("users").delete {
                        filter { eq("user_id", userId) }
                    }
                } ?: run {
                    Log.e("UserId", "UserId is null")
                }
                supabaseClient.auth.signOut()
                navigateToWelcomeAndClearStack()
            }
            dialog.dismiss()
        }

        dialog.show()

        val window = dialog.window
        window?.let {
            val displayMetrics = resources.displayMetrics
            val dialogWidth = (displayMetrics.widthPixels * 0.867).toInt()
            val dialogHeight = (displayMetrics.heightPixels * 0.359).toInt()

            it.setLayout(dialogWidth, dialogHeight)

            it.setBackgroundDrawableResource(android.R.color.transparent)

            it.setDimAmount(0.2f)
        }
    }

    private fun navigateToWelcomeAndClearStack() {
        val mainNavController = requireActivity().findNavController(R.id.fragment_nav_host_content_main)

        val navOptions = navOptions {
            anim {
                enter = R.anim.fade_in
                exit = R.anim.fade_out
            }
            popUpTo(R.id.nav_graph) {
                inclusive = true
            }
        }
        mainNavController.navigate(R.id.WelcomeFragment, null, navOptions)
    }
}