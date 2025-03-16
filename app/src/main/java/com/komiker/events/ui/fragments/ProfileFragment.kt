package com.komiker.events.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
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
import com.komiker.events.databinding.FragmentProfileBinding
import com.komiker.events.glide.CircleCropTransformation
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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
    private lateinit var buttonFavorite: ImageButton
    private lateinit var buttonNotification: ImageButton
    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeUIComponents(view)

        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                updateUIWithUserData(user)
            } else {
                handleEmptyUserData()
            }
        }

        setupButtonFavorite()
        setupButtonNotification()
        initButtonEditProfile()
        initButtonLogOut()
        initButtonDelete()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeUIComponents(view: View) {
        buttonFavorite = view.findViewById(R.id.button_favorite)
        buttonNotification = view.findViewById(R.id.button_notification)
        profileImage = view.findViewById(R.id.image_profile)
        profileName = view.findViewById(R.id.text_profile_name)
        profileEmail = view.findViewById(R.id.text_profile_username)
    }

    private fun updateUIWithUserData(user: User) {
        profileName.text = user.username
        profileEmail.text = user.email

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
        profileName.text = getString(R.string.user_not_found)
        profileEmail.text = ""
        profileImage.setImageResource(R.drawable.img_profile_placeholder)
    }

    private fun setupButtonFavorite() {
        buttonFavorite.setOnClickListener {
            //
        }
    }

    private fun setupButtonNotification() {
        buttonNotification.setOnClickListener {
            //
        }
    }

    private fun initButtonEditProfile() {
        binding.constraintEditProfileButtonLayout.setOnClickListener {
            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            findNavController().navigate(
                R.id.action_ProfileFragment_to_EditProfileFragment,
                null,
                NavOptions.Builder()
                    .setEnterAnim(fadeInAnimation)
                    .setExitAnim(fadeOutAnimation)
                    .build()
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun initButtonLogOut() {
        binding.constraintLogOutButtonLayout.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                supabaseClient.auth.signOut()

                val fadeOutAnimation = R.anim.fade_out
                val fadeInAnimation = R.anim.fade_in

                findNavController().navigate(
                    R.id.WelcomeFragment,
                    null,
                    NavOptions.Builder()
                        .setEnterAnim(fadeInAnimation)
                        .setExitAnim(fadeOutAnimation)
                        .build()
                )
            }
        }
    }

    private fun initButtonDelete() {
        binding.constraintDeleteAccountButtonLayout.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
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
            GlobalScope.launch(Dispatchers.Main) {
                val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
                userId?.let {
                    supabaseClient.from("users").delete {
                        filter {
                            eq("id_user", userId)
                        }
                    }
                } ?: run {
                    Log.e("UserId", "UserId is null")
                }

                supabaseClient.auth.signOut()

                val fadeOutAnimation = R.anim.fade_out
                val fadeInAnimation = R.anim.fade_in

                findNavController().navigate(
                    R.id.WelcomeFragment,
                    null,
                    NavOptions.Builder()
                        .setEnterAnim(fadeInAnimation)
                        .setExitAnim(fadeOutAnimation)
                        .build()
                )
            }
            dialog.dismiss()
        }

        dialog.show()

        val window = dialog.window
        window?.let {
            val displayMetrics = resources.displayMetrics
            val dialogWidth = (displayMetrics.widthPixels * 0.867).toInt()
            val dialogHeight = (displayMetrics.heightPixels * 0.331).toInt()

            it.setLayout(dialogWidth, dialogHeight)

            it.setBackgroundDrawableResource(android.R.color.transparent)

            it.setDimAmount(0.2f)
        }
    }
}