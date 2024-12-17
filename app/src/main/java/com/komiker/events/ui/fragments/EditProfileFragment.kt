package com.komiker.events.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.SupabaseUserDao
import com.komiker.events.data.database.entities.User
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
    private lateinit var profileImage: ImageView
    private lateinit var nicknameTitle: TextView
    private lateinit var usernameEditText: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeUIComponents(view)

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                updateUIWithUserData(user)
            } else {
                handleEmptyUserData()
            }
        }

        setupSystemBars()
        initButtonBack()
        initButtonCheckmark()
        initButtonImage()
        setupOnBackPressedCallback()
        setupClearButtonWithAction(R.id.edittext_username)
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

    private fun initButtonCheckmark() {
        binding.buttonCheckmark.setOnClickListener {
            val username = usernameEditText.text.toString()

            if (username.isNotEmpty()) {
                profileViewModel.updateUsername(username)

                navigateToMainMenuWithProfile()
            } else {
                Toast.makeText(context, "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initButtonImage() {
        binding.imageProfile.setOnClickListener {
            val bottomSheet = BottomSheetAvatarFragment()
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
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
            },
            NavOptions.Builder()
                .setEnterAnim(R.anim.fade_in)
                .setExitAnim(R.anim.fade_out)
                .build()
        )
    }

    private fun initializeUIComponents(view: View) {
        profileImage = view.findViewById(R.id.image_profile)
        nicknameTitle = view.findViewById(R.id.text_nickname_title)
        usernameEditText = view.findViewById(R.id.edittext_username)
    }

    private fun updateUIWithUserData(user: User) {
        nicknameTitle.text = user.username
        usernameEditText.setText(user.username)

        Glide.with(this)
            .load(user.avatar)
            .override(400, 400)
            .signature(ObjectKey(System.currentTimeMillis().toString()))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .placeholder(R.drawable.img_profile_placeholder)
            .transform(CircleCropTransformation())
            .into(profileImage)
    }

    private fun handleEmptyUserData() {
        nicknameTitle.text = getString(R.string.user_not_found)
        usernameEditText.setText("")
        profileImage.setImageResource(R.drawable.img_profile_placeholder)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupClearButtonWithAction(editTextId: Int) {
        val editText = view?.findViewById<EditText>(editTextId)
        val clearButton: Drawable? = editText?.compoundDrawablesRelative?.get(2)

        editText?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (editText.right - clearButton!!.bounds.width())) {
                    editText.setText("")

                    editText.requestFocus()

                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)

                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}