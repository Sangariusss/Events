package com.komiker.events.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.komiker.events.data.models.TagItem
import com.komiker.events.databinding.ItemSubTagBinding
import com.komiker.events.databinding.ItemTagHeaderBinding

class TagsAdapter(
    private val onSubTagSelectionChanged: (List<String>) -> Unit
) : ListAdapter<TagItem, RecyclerView.ViewHolder>(TagItemDiffCallback()) {

    private val selectedSubTags = mutableSetOf<String>()
    private var headerHeight: Int = 0
    private var subTagHeight: Int = 0

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_SUB_TAG = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TagItem.Header -> VIEW_TYPE_HEADER
            is TagItem.SubTag -> VIEW_TYPE_SUB_TAG
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemTagHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_SUB_TAG -> {
                val binding = ItemSubTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SubTagViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(getItem(position) as TagItem.Header)
            is SubTagViewHolder -> holder.bind(getItem(position) as TagItem.SubTag)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemTagHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: TagItem.Header) {
            if (headerHeight > 0) {
                val layoutParams = binding.root.layoutParams
                layoutParams.height = headerHeight
                binding.root.layoutParams = layoutParams
            }
            binding.textCategoryName.text = header.name
        }
    }

    inner class SubTagViewHolder(private val binding: ItemSubTagBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(subTag: TagItem.SubTag) {
            if (subTagHeight > 0) {
                val layoutParams = binding.root.layoutParams
                layoutParams.height = subTagHeight
                binding.root.layoutParams = layoutParams
            }
            binding.textSubTag.text = subTag.name
            binding.checkboxSubTag.setOnCheckedChangeListener(null)
            binding.checkboxSubTag.isChecked = selectedSubTags.contains(subTag.name)
            binding.checkboxSubTag.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedSubTags.add(subTag.name)
                } else {
                    selectedSubTags.remove(subTag.name)
                }
                onSubTagSelectionChanged(selectedSubTags.toList())
            }
        }
    }

    class TagItemDiffCallback : DiffUtil.ItemCallback<TagItem>() {
        override fun areItemsTheSame(oldItem: TagItem, newItem: TagItem): Boolean {
            return when {
                oldItem is TagItem.Header && newItem is TagItem.Header -> oldItem.name == newItem.name
                oldItem is TagItem.SubTag && newItem is TagItem.SubTag -> oldItem.name == newItem.name
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: TagItem, newItem: TagItem): Boolean {
            return oldItem == newItem
        }
    }

    fun getSelectedTags(): List<String> {
        return selectedSubTags.toList()
    }

    fun setHeaderHeight(height: Int) {
        this.headerHeight = height
        notifyItemRangeChanged(0, itemCount)
    }

    fun setSubTagHeight(height: Int) {
        this.subTagHeight = height
        notifyItemRangeChanged(0, itemCount)
    }
}