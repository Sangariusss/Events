package com.komiker.events.ui.fragments

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
    private val viewModel: CreateEventViewModel by activityViewModels()
    private val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val contentResolver = requireContext().contentResolver
        val validImageItems = mutableListOf<ImageAdapter.ImageItem>()

        uris.forEach { uri ->
            val fileSize = getFileSize(uri, contentResolver)
            val fileName = getFileName(uri, contentResolver) ?: "image_${System.currentTimeMillis()}"

            if (fileSize > MAX_FILE_SIZE_BYTES) {
                val sizeInMB = String.format("%.2f", fileSize / (1024.0 * 1024.0))
                Toast.makeText(
                    requireContext(),
                    "Image is too large ($sizeInMB MB). Max size is 10 MB.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val tempFile = uriToTempFile(uri, fileName)
                if (tempFile != null) {
                    val imageItem = ImageAdapter.ImageItem(
                        tempFile,
                        fileName,
                        "${getFileType(uri, contentResolver, fileName).uppercase()} | ${formatFileSize(fileSize)}"
                    )
                    validImageItems.add(imageItem)
                }
            }
        }

        if (validImageItems.isNotEmpty()) {
            val startPosition = viewModel.images.size
            viewModel.images.addAll(validImageItems)
            imageAdapter.notifyItemRangeInserted(startPosition, validImageItems.size)
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
                    if (position in viewModel.images.indices) {
                        viewModel.images.removeAt(position)
                        imageAdapter.notifyItemRemoved(position)
                        imageAdapter.notifyItemRangeChanged(position, viewModel.images.size)
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
            if (itemCount > 0) {
                viewModel.images.clear()
                imageAdapter.notifyItemRangeRemoved(0, itemCount)
            }
        }
    }

    private fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 B"
        val kb = sizeBytes / 1024.0
        val df = DecimalFormat("#.##")
        return if (kb < 1024) "${df.format(kb)} KB" else "${df.format(kb / 1024.0)} MB"
    }

    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val fileNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (fileNameIndex != -1) return cursor.getString(fileNameIndex)
            }
        }
        return uri.lastPathSegment?.substringAfterLast("/") ?: "image_${System.currentTimeMillis()}"
    }

    private fun getFileSize(uri: Uri, contentResolver: ContentResolver): Long {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val fileSizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (fileSizeIndex != -1 && !cursor.isNull(fileSizeIndex)) {
                    return cursor.getLong(fileSizeIndex)
                }
            }
        }
        return 0L
    }

    private fun getFileType(uri: Uri, contentResolver: ContentResolver, fileName: String): String {
        val mimeType = contentResolver.getType(uri)
        return when {
            mimeType?.contains("jpeg") == true || mimeType?.contains("jpg") == true -> "JPG"
            mimeType?.contains("png") == true -> "PNG"
            mimeType?.contains("webp") == true -> "WEBP"
            else -> fileName.substringAfterLast(".", "UNKNOWN").uppercase()
        }
    }

    private fun uriToTempFile(uri: Uri, fileName: String): File? {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                File.createTempFile(
                    "temp_image_${System.currentTimeMillis()}",
                    ".${fileName.substringAfterLast(".")}",
                    requireContext().cacheDir
                ).apply {
                    FileOutputStream(this).use { output -> input.copyTo(output) }
                    deleteOnExit()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}