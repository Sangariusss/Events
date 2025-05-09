package com.komiker.events.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.komiker.events.R
import com.komiker.events.data.models.LocationItem
import com.komiker.events.databinding.ItemLocationBinding

class LocationAdapter(
    private val onAddressClick: (LocationItem) -> Unit
) : ListAdapter<LocationItem, LocationAdapter.LocationViewHolder>(AddressDiffCallback()) {

    private var itemHeight: Int = 0

    fun setItemHeight(height: Int) {
        if (itemHeight != height) {
            itemHeight = height
            notifyItemRangeChanged(0, itemCount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class LocationViewHolder(private val binding: ItemLocationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(locationItem: LocationItem, position: Int) {
            binding.textLocation.text = locationItem.address
            binding.root.setOnClickListener { onAddressClick(locationItem) }

            if (itemHeight > 0 && binding.root.layoutParams.height != itemHeight) {
                binding.root.layoutParams = binding.root.layoutParams.apply { height = itemHeight }
            }

            val isLastItem = position == itemCount - 1
            binding.root.background = if (isLastItem) {
                ContextCompat.getDrawable(binding.root.context, android.R.color.transparent)
            } else {
                ContextCompat.getDrawable(binding.root.context, R.drawable.border_bottom)
            }
        }
    }

    class AddressDiffCallback : DiffUtil.ItemCallback<LocationItem>() {
        override fun areItemsTheSame(oldItem: LocationItem, newItem: LocationItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LocationItem, newItem: LocationItem): Boolean {
            return oldItem == newItem
        }
    }
}