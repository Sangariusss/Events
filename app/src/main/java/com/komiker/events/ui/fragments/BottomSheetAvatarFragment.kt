package com.komiker.events.ui.fragments

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class BottomSheetAvatarFragment : BottomSheetDialogFragment() {

    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val profileViewModel: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }

    private val requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String> =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) openImagePicker()
        }

    private val imagePickerLauncher: androidx.activity.result.ActivityResultLauncher<Intent> =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let { uploadAvatarToServer(it) }
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
            window.setDimAmount(0.2f)
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

        removeAvatarButton.setOnClickListener { dismiss() }
        changeAvatarButton.setOnClickListener { checkPermissionsAndOpenPicker() }
    }

    private fun checkPermissionsAndOpenPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun uploadAvatarToServer(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val compressedByteArray = compressImage(inputStream, uri)

                val avatarPath = "avatars/${profileViewModel.getUserId()}.jpg"
                val bucket = supabaseClient.storage.from("avatars")
                val result = bucket.upload(avatarPath, compressedByteArray, upsert = true)

                if (result.isNotEmpty()) {
                    val publicUrl = getPublicUrl(avatarPath)
                    withContext(Dispatchers.Main) {
                        profileViewModel.updateUserAvatar(publicUrl)
                        dismiss()
                    }
                }
            }
        }
    }

    private fun compressImage(inputStream: java.io.InputStream, uri: Uri): ByteArray {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val newInputStream = requireContext().contentResolver.openInputStream(uri)!!

        options.inJustDecodeBounds = false
        options.inSampleSize = calculateInSampleSize(options, 1080, 1080)
        val bitmap = BitmapFactory.decodeStream(newInputStream, null, options)!!

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

        bitmap.recycle()
        newInputStream.close()

        return outputStream.toByteArray()
    }

    @Suppress("SameParameterValue")
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getPublicUrl(path: String): String {
        val bucketUrl = "https://npbtrddkgejcsluqzguq.supabase.co/storage/v1/object/public/avatars/"
        return bucketUrl + path
    }
}