package com.komiker.events.ui.fragments

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.SupabaseUserDao
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class BottomSheetAvatarFragment : BottomSheetDialogFragment() {

    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openImagePicker()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val imagePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    uploadAvatarToServer(it)
                } ?: run {
                    Toast.makeText(requireContext(), "Failed to get image", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bottom_sheet_avatar, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_98)
        }

        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as ViewGroup
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isHideable = true
            behavior.skipCollapsed = true

            bottomSheet.post {
                val layoutParams = bottomSheet.layoutParams
                layoutParams.height = (resources.displayMetrics.heightPixels * 0.277).toInt()
                bottomSheet.layoutParams = layoutParams
            }
        }
    }

    override fun onStop() {
        super.onStop()
        dialog?.window?.let { window ->
            window.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.neutral_100)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val removeAvatarButton = view.findViewById<ConstraintLayout>(R.id.constraint_button_remove_avatar)
        val changeAvatarButton = view.findViewById<ConstraintLayout>(R.id.constraint_button_change_avatar)

        removeAvatarButton.setOnClickListener {
            dismiss()
        }

        changeAvatarButton.setOnClickListener {
            checkPermissionsAndOpenPicker()
        }
    }

    private fun checkPermissionsAndOpenPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED -> {
                    openImagePicker()
                }
                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                    openImagePicker()
                }
                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun uploadAvatarToServer(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val avatarPath = "avatars/${profileViewModel.getUserId()}.jpg"
                    val bucket = supabaseClient.storage.from("avatars")
                    val byteArray = inputStream.readBytes()
                    val result = bucket.upload(avatarPath, byteArray, upsert = true)

                    if (result.isNotEmpty()) {
                        val publicUrl = getPublicUrl(avatarPath)
                        withContext(Dispatchers.Main) {
                            profileViewModel.updateUserAvatar(publicUrl)

                            dismiss()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to open input stream", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getPublicUrl(path: String): String {
        val bucketUrl = "https://npbtrddkgejcsluqzguq.supabase.co/storage/v1/object/public/avatars/"
        return bucketUrl + path
    }
}