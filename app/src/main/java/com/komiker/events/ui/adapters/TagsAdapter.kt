package com.komiker.events.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.komiker.events.R
import com.komiker.events.data.models.TagItem
import com.komiker.events.databinding.ItemSubTagBinding
import com.komiker.events.databinding.ItemTagHeaderBinding

class TagsAdapter(
    private val onSubTagSelectionChanged: (List<String>) -> Unit
) : ListAdapter<TagItem, RecyclerView.ViewHolder>(TagItemDiffCallback()) {

    private val selectedSubTags = mutableSetOf<String>()
    private var headerHeight: Int = 0
    private var subTagHeight: Int = 0
    private lateinit var transparentDrawable: android.graphics.drawable.Drawable
    private lateinit var borderBottomDrawable: android.graphics.drawable.Drawable

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_SUB_TAG = 1
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is TagItem.Header -> VIEW_TYPE_HEADER
        is TagItem.SubTag -> VIEW_TYPE_SUB_TAG
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (!::transparentDrawable.isInitialized) {
            transparentDrawable = ContextCompat.getDrawable(parent.context, android.R.color.transparent)
                ?: throw IllegalStateException("Failed to load transparent drawable")
        }
        if (!::borderBottomDrawable.isInitialized) {
            borderBottomDrawable = ContextCompat.getDrawable(parent.context, R.drawable.border_bottom)
                ?: throw IllegalStateException("Failed to load border_bottom drawable")
        }

        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(ItemTagHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            VIEW_TYPE_SUB_TAG -> SubTagViewHolder(ItemSubTagBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderViewHolder -> if (item is TagItem.Header) holder.bind(item)
            is SubTagViewHolder -> if (item is TagItem.SubTag) holder.bind(item, position)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemTagHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: TagItem.Header) {
            if (headerHeight > 0 && binding.root.layoutParams.height != headerHeight) {
                binding.root.layoutParams = binding.root.layoutParams.apply { height = headerHeight }
            }
            binding.textCategoryName.text = header.name
        }
    }

    inner class SubTagViewHolder(private val binding: ItemSubTagBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(subTag: TagItem.SubTag, position: Int) {
            if (subTagHeight > 0 && binding.root.layoutParams.height != subTagHeight) {
                binding.root.layoutParams = binding.root.layoutParams.apply { height = subTagHeight }
            }
            binding.textSubTag.text = subTag.name

            val isChecked = selectedSubTags.contains(subTag.name)
            binding.buttonSubTag.isSelected = isChecked

            binding.root.background = if (isLastSubTagInCategory(position)) transparentDrawable else borderBottomDrawable

            binding.buttonSubTag.setOnClickListener {
                val newIsChecked = !binding.buttonSubTag.isSelected
                binding.buttonSubTag.isSelected = newIsChecked
                if (newIsChecked) selectedSubTags.add(subTag.name) else selectedSubTags.remove(subTag.name)
                onSubTagSelectionChanged(selectedSubTags.toList())
            }
        }

        private fun isLastSubTagInCategory(position: Int): Boolean {
            val itemCount = itemCount
            return position >= itemCount - 1 || (getItem(position) is TagItem.SubTag && getItem(position + 1) is TagItem.Header)
        }
    }

    class TagItemDiffCallback : DiffUtil.ItemCallback<TagItem>() {
        override fun areItemsTheSame(oldItem: TagItem, newItem: TagItem): Boolean = when {
            oldItem is TagItem.Header && newItem is TagItem.Header -> oldItem.name == newItem.name
            oldItem is TagItem.SubTag && newItem is TagItem.SubTag -> oldItem.name == newItem.name
            else -> false
        }

        override fun areContentsTheSame(oldItem: TagItem, newItem: TagItem): Boolean = oldItem == newItem
    }

    fun getSelectedTags(): List<String> = selectedSubTags.toList()

    fun setHeaderHeight(height: Int) {
        if (headerHeight != height) {
            headerHeight = height
            notifyItemRangeChanged(0, itemCount, "height")
        }
    }

    fun setSubTagHeight(height: Int) {
        if (subTagHeight != height) {
            subTagHeight = height
            notifyItemRangeChanged(0, itemCount, "height")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("height")) {
            when (holder) {
                is HeaderViewHolder -> holder.bind(getItem(position) as TagItem.Header)
                is SubTagViewHolder -> holder.bind(getItem(position) as TagItem.SubTag, position)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }
}