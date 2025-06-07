package com.komiker.events.ui.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komiker.events.R
import com.komiker.events.data.database.models.Event
import com.komiker.events.databinding.ItemEventBinding
import com.komiker.events.databinding.PopupMenuBinding
import com.komiker.events.glide.CircleCropTransformation
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

class EventsAdapter(
    private val currentUserId: String?,
    private val onDeleteClicked: (Event) -> Unit,
    private val onItemClicked: (Event) -> Unit,
    private val likeCache: MutableMap<String, Boolean>,
    private val likesCountCache: MutableMap<String, Int>,
    private val onLikeClicked: (String, Boolean, (Boolean, Int) -> Unit) -> Unit
) : ListAdapter<Event, EventsAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val payload = payloads[0] as? Int
            payload?.let {
                holder.updateLikesCount(it)
            }
        } else {
            holder.bind(getItem(position))
        }
    }

    inner class EventViewHolder(private val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root) {
        private var popupWindow: PopupWindow? = null
        private var overlayView: View? = null
        private var isProcessingLike = false

        fun bind(event: Event) {
            bindUserInfo(event)
            bindEventDetails(event)
            setupClickListeners(event)
            setupMoreButton(event)
            setupLikeButton(event)
        }

        fun updateLikesCount(likesCount: Int) {
            binding.textLikesCount.text = formatLikesCount(likesCount)
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                likesCountCache[getItem(position).id] = likesCount
            }
        }

        private fun bindUserInfo(event: Event) {
            binding.textUserName.text = event.username
            Glide.with(binding.imageProfile.context)
                .load(event.userAvatar)
                .placeholder(R.drawable.img_profile_placeholder)
                .transform(CircleCropTransformation())
                .into(binding.imageProfile)
        }

        private fun bindEventDetails(event: Event) {
            binding.textTitle.text = event.title
            binding.textContent.text = event.description
            binding.textLikesCount.text = formatLikesCount(likesCountCache[event.id] ?: event.likesCount)
            binding.textTime.text = formatTimeAgo(event.createdAt)
        }

        private fun setupClickListeners(event: Event) {
            binding.buttonShare.setOnClickListener {
                shareEvent(event)
            }
            binding.root.setOnClickListener {
                onItemClicked(event)
            }
        }

        private fun setupMoreButton(event: Event) {
            if (currentUserId != null && event.userId == currentUserId) {
                binding.textUserName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.blue_60))
                binding.buttonMore.setOnClickListener { showPopupMenu(event) }
            } else {
                binding.textUserName.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.black))
                binding.buttonMore.setOnClickListener(null)
            }
        }

        private fun setupLikeButton(event: Event) {
            binding.imageLike.setImageResource(
                if (likeCache[event.id] == true) R.drawable.ic_heart else R.drawable.ic_heart_outline
            )

            binding.imageLike.setOnClickListener {
                if (currentUserId != null && !isProcessingLike) {
                    isProcessingLike = true
                    binding.imageLike.isEnabled = false
                    binding.imageLike.alpha = 0.5f

                    val isCurrentlyLiked = likeCache[event.id] ?: false
                    val currentLikesCount = likesCountCache[event.id] ?: event.likesCount

                    if (isCurrentlyLiked) {
                        val newLikesCount = currentLikesCount - 1
                        binding.textLikesCount.text = formatLikesCount(newLikesCount.coerceAtLeast(0))
                        binding.imageLike.setImageResource(R.drawable.ic_heart_outline)
                        likeCache[event.id] = false
                        likesCountCache[event.id] = newLikesCount
                        onLikeClicked(event.id, false) { success, serverLikesCount ->
                            isProcessingLike = false
                            binding.imageLike.isEnabled = true
                            binding.imageLike.alpha = 1.0f
                            if (!success) {
                                binding.textLikesCount.text = formatLikesCount(currentLikesCount)
                                binding.imageLike.setImageResource(R.drawable.ic_heart)
                                likeCache[event.id] = true
                                likesCountCache[event.id] = currentLikesCount
                            } else {
                                likesCountCache[event.id] = serverLikesCount
                                binding.textLikesCount.text = formatLikesCount(serverLikesCount)
                            }
                        }
                    } else {
                        val newLikesCount = currentLikesCount + 1
                        binding.textLikesCount.text = formatLikesCount(newLikesCount)
                        binding.imageLike.setImageResource(R.drawable.ic_heart)
                        likeCache[event.id] = true
                        likesCountCache[event.id] = newLikesCount
                        onLikeClicked(event.id, true) { success, serverLikesCount ->
                            isProcessingLike = false
                            binding.imageLike.isEnabled = true
                            binding.imageLike.alpha = 1.0f
                            if (!success) {
                                binding.textLikesCount.text = formatLikesCount(currentLikesCount)
                                binding.imageLike.setImageResource(R.drawable.ic_heart_outline)
                                likeCache[event.id] = false
                                likesCountCache[event.id] = currentLikesCount
                            } else {
                                likesCountCache[event.id] = serverLikesCount
                                binding.textLikesCount.text = formatLikesCount(serverLikesCount)
                            }
                        }
                    }
                }
            }
        }

        private fun shareEvent(event: Event) {
            val username = event.username.replace(" ", "_")
            val deepLink = "https://excito.netlify.app/@$username/event/${event.id}"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, deepLink)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            binding.root.context.startActivity(shareIntent)
        }

        private fun showPopupMenu(event: Event) {
            val popupBinding = PopupMenuBinding.inflate(LayoutInflater.from(binding.root.context))
            val popupView = popupBinding.root
            val rootViewGroup = binding.root.rootView as? ViewGroup ?: return

            popupWindow = PopupWindow(popupView, (rootViewGroup.width * 0.278).toInt(), (rootViewGroup.height * 0.110).toInt(), true).apply {
                isOutsideTouchable = true
                isFocusable = true
                setBackgroundDrawable(ContextCompat.getDrawable(binding.root.context, R.drawable.bg_popup_menu))
                animationStyle = R.style.PopupAnimation
                setOnDismissListener { dismissPopupMenu() }
            }

            overlayView = View(binding.root.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.neutral_0_30_percent))
                alpha = 0f
                setOnClickListener { dismissPopupMenu() }
                rootViewGroup.addView(this)
            }

            overlayView?.animate()
                ?.alpha(0.7f)
                ?.setDuration(250)
                ?.start()

            val location = IntArray(2)
            binding.buttonMore.getLocationOnScreen(location)
            val buttonX = location[0] + binding.buttonMore.width - popupWindow!!.width
            val buttonY = location[1]

            popupWindow?.showAtLocation(binding.root, android.view.Gravity.NO_GRAVITY, buttonX, buttonY)

            popupBinding.menuEdit.setOnClickListener {
                dismissPopupMenu()
                // TODO: Add edit logic
            }
            popupBinding.menuDelete.setOnClickListener {
                onDeleteClicked(event)
                dismissPopupMenu()
            }
        }

        private fun dismissPopupMenu() {
            overlayView?.animate()
                ?.alpha(0f)
                ?.setDuration(250)
                ?.withEndAction {
                    (overlayView?.parent as? ViewGroup)?.removeView(overlayView)
                    overlayView = null
                }
                ?.start()

            popupWindow?.dismiss()
            popupWindow = null
        }

        private fun formatTimeAgo(createdAt: OffsetDateTime?): String = createdAt?.let {
            val now = OffsetDateTime.now()
            when {
                ChronoUnit.MINUTES.between(it, now) < 1 -> "now"
                ChronoUnit.MINUTES.between(it, now) < 60 -> "${ChronoUnit.MINUTES.between(it, now)}m"
                ChronoUnit.HOURS.between(it, now) < 24 -> "${ChronoUnit.HOURS.between(it, now)}h"
                ChronoUnit.DAYS.between(it, now) < 7 -> "${ChronoUnit.DAYS.between(it, now)}d"
                else -> it.format(DateTimeFormatter.ofPattern("MM/dd"))
            }
        } ?: "Unknown"

        private fun formatLikesCount(count: Int): String {
            return when {
                count >= 999_999_950 -> {
                    val billions = count / 1_000_000_000.0
                    String.format(Locale.US, "%.1fB", billions)
                }
                count >= 999_950 -> {
                    val millions = count / 1_000_000.0
                    String.format(Locale.US, "%.1fM", millions)
                }
                count >= 1_000 -> {
                    val thousands = count / 1_000.0
                    String.format(Locale.US, "%.1fK", thousands)
                }
                else -> count.toString()
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem
        override fun getChangePayload(oldItem: Event, newItem: Event): Any? {
            return if (oldItem.likesCount != newItem.likesCount) newItem.likesCount else null
        }
    }
}