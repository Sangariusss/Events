package com.komiker.events.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.komiker.events.R
import com.komiker.events.databinding.ItemImageBinding
import java.io.File

class ImageAdapter(
    private val images: MutableList<ImageItem>,
    private val onDeleteClick: (Int) -> Unit,
    private val recyclerViewHeight: Int,
    private val heightPercent: Float = 0.181f,
    private val spacePercent: Float = 0.024f
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    data class ImageItem(val file: File, val name: String, val size: String)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position], position)
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            val layoutParams = binding.root.layoutParams as RecyclerView.LayoutParams

            layoutParams.height = (recyclerViewHeight * heightPercent).toInt()

            if (bindingAdapterPosition < itemCount - 1) {
                layoutParams.bottomMargin = (recyclerViewHeight * spacePercent).toInt()
            } else {
                layoutParams.bottomMargin = 0
            }

            binding.root.layoutParams = layoutParams
        }

        fun bind(imageItem: ImageItem, position: Int) {
            binding.textImageName.text = imageItem.name
            binding.textImageInfo.text = binding.root.context.getString(R.string.image_info_format, imageItem.size)
            binding.buttonDelete.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }
}