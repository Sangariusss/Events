package com.komiker.events.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.User
import com.komiker.events.databinding.FragmentEditProfileBinding
import com.komiker.events.glide.CircleCropTransformation
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
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
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSystemBars()
        initButtonBack()
        initButtonOverflowMenu()
        initButtonUploadMedia()
        initButtonName()
        initButtonUsername()
        setupOnBackPressedCallback()
        observeUserData()

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSystemBars() {
        requireActivity().window.apply {
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        }
    }

    private fun initButtonBack() {
        binding.buttonBack.setOnClickListener {
            navigateToMainMenuWithProfile()
        }
    }

    private fun initButtonOverflowMenu() {
        binding.buttonOverflowMenu.setOnClickListener {
            //
        }
    }

    private fun initButtonUploadMedia() {
        binding.buttonUploadMedia.setOnClickListener {
            val bottomSheet = BottomSheetAvatarFragment()
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
        }
    }

    private fun initButtonName() {
        binding.buttonName.setOnClickListener {
            findNavController().navigate(R.id.action_EditProfileFragment_to_EditNameFragment)
        }
    }

    private fun initButtonUsername() {
        binding.buttonUsername.setOnClickListener {
            findNavController().navigate(R.id.action_EditProfileFragment_to_EditUsernameFragment)
        }
    }

    private fun setupOnBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToMainMenuWithProfile()
            }
        })
    }

    private fun navigateToMainMenuWithProfile() {
        findNavController().navigate(
            R.id.action_EditProfileFragment_to_MainMenuFragment,
            Bundle().apply {
                putString("navigateTo", "profile")
            }
        )
    }

    private fun observeUserData() {
        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                updateUIWithUserData(user)
            } else {
                handleEmptyUserData()
            }
        }
    }

    private fun updateUIWithUserData(user: User) {
        binding.textProfileName.text = user.name
        binding.textProfileUsername.text = getString(R.string.formatted_username, user.username.lowercase())
        binding.textNameValue.text = user.name
        binding.textUsernameValue.text = getString(R.string.formatted_username, user.username.lowercase())

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
        binding.textProfileUsername.text = ""
        binding.textNameValue.text = ""
        binding.textUsernameValue.text = ""
        binding.imageProfile.setImageResource(R.drawable.img_profile_placeholder)
    }
}