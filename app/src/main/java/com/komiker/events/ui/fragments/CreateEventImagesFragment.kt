package com.komiker.events.ui.fragments

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.komiker.events.databinding.FragmentCreateEventImagesBinding
import com.komiker.events.ui.adapters.ImageAdapter
import com.komiker.events.viewmodels.CreateEventViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

class CreateEventImagesFragment : Fragment() {

    private var _binding: FragmentCreateEventImagesBinding? = null
    private val binding get() = _binding!!

    private lateinit var imageAdapter: ImageAdapter
    private val viewModel: CreateEventViewModel by viewModels({ requireParentFragment() })

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val startPosition = viewModel.images.size
        uris.forEach { uri ->
            val contentResolver: ContentResolver = requireContext().contentResolver
            val fileName = getFileName(uri, contentResolver) ?: "image_${System.currentTimeMillis()}"
            val fileSizeBytes = getFileSize(uri, contentResolver)
            val fileSizeFormatted = formatFileSize(fileSizeBytes)
            val fileType = getFileType(uri, contentResolver, fileName).uppercase()
            val fileInfo = "$fileType | $fileSizeFormatted"

            val tempFile = uriToTempFile(uri, fileName)
            if (tempFile != null) {
                val imageItem = ImageAdapter.ImageItem(tempFile, fileName, fileInfo)
                viewModel.images.add(imageItem)
            } else {
                Log.e("CreateEventImages", "Failed to create temp file for $fileName")
            }
        }
        val itemCount = uris.size
        if (itemCount > 0) {
            imageAdapter.notifyItemRangeInserted(startPosition, itemCount)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEventImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupUploadButton()
        setupRemoveAllButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.recyclerViewImages.post {
            val recyclerViewHeight = binding.recyclerViewImages.height
            imageAdapter = ImageAdapter(
                viewModel.images,
                { position ->
                    if (position in 0 until viewModel.images.size) {
                        viewModel.images.removeAt(position)
                        imageAdapter.notifyItemRemoved(position)
                        imageAdapter.notifyItemRangeChanged(position, viewModel.images.size)
                    } else {
                        Log.w("CreateEventImages", "Invalid position for removal: $position, size: ${viewModel.images.size}")
                    }
                },
                recyclerViewHeight,
                heightPercent = 0.181f,
                spacePercent = 0.024f
            )
            binding.recyclerViewImages.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewImages.adapter = imageAdapter
        }
    }

    private fun setupUploadButton() {
        binding.buttonUploadContainer.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }
    }

    private fun setupRemoveAllButton() {
        binding.buttonRemoveAll.setOnClickListener {
            val itemCount = viewModel.images.size
            viewModel.images.clear()
            if (itemCount > 0) {
                imageAdapter.notifyItemRangeRemoved(0, itemCount)
            }
        }
    }

    private fun formatFileSize(sizeBytes: Long): String {
        val kb = sizeBytes / 1000.0
        val df = DecimalFormat("#0.00")
        return if (kb < 1000) {
            "${df.format(kb)} kB"
        } else {
            val mb = kb / 1000.0
            "${df.format(mb)} MB"
        }
    }

    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val fileNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (fileNameIndex != -1) {
                    return cursor.getString(fileNameIndex)
                }
            }
        }
        val lastPathSegment = uri.lastPathSegment
        return lastPathSegment?.substringAfterLast("/") ?: "image_${System.currentTimeMillis()}"
    }

    private fun getFileSize(uri: Uri, contentResolver: ContentResolver): Long {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val fileSizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (fileSizeIndex != -1) {
                    return cursor.getLong(fileSizeIndex)
                }
            }
        }
        return 0L
    }

    private fun getFileType(uri: Uri, contentResolver: ContentResolver, fileName: String): String {
        val mimeType = contentResolver.getType(uri)
        if (mimeType != null) {
            when {
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> return "JPG"
                mimeType.contains("png") -> return "PNG"
                mimeType.contains("webp") -> return "WEBP"
            }
        }
        return fileName.substringAfterLast(".", "UNKNOWN").uppercase()
    }

    private fun uriToTempFile(uri: Uri, fileName: String): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val tempFile = File.createTempFile(
                    "temp_image_${System.currentTimeMillis()}",
                    ".${fileName.substringAfterLast(".")}",
                    requireContext().cacheDir
                )
                inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CreateEventImages", "Error converting URI to temp file: ${e.message}")
            null
        }
    }
}