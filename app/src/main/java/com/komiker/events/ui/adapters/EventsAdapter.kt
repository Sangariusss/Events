package com.komiker.events.ui.adapters

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
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

class EventsAdapter(
    private val currentUserId: String?,
    private val onDeleteClicked: (Event) -> Unit,
    private val navController: NavController
) : ListAdapter<Event, EventsAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(private val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root) {
        private var popupWindow: PopupWindow? = null
        private var overlayView: View? = null

        fun bind(event: Event) {
            binding.textUserName.text = event.username
            binding.textTitle.text = event.title
            binding.textContent.text = event.description
            binding.textLikesCount.text = event.likesCount.toString()
            binding.textTime.text = formatTimeAgo(event.createdAt!!)

            Glide.with(binding.imageProfile.context)
                .load(event.userAvatar)
                .placeholder(R.drawable.img_profile_placeholder)
                .transform(CircleCropTransformation())
                .into(binding.imageProfile)

            binding.buttonShare.setOnClickListener {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, event.title)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                binding.root.context.startActivity(shareIntent)
            }

            if (currentUserId != null && event.userId == currentUserId) {
                binding.textUserName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.blue_60))
                binding.buttonMore.setOnClickListener {
                    showPopupMenu(event)
                }
            } else {
                binding.textUserName.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.black))
                binding.buttonMore.setOnClickListener(null)
            }

            binding.root.setOnClickListener {
                val bundle = Bundle().apply {
                    putParcelable("event", event)
                }
                navController.navigate(R.id.action_MainMenuFragment_to_EventDetailFragment, bundle)
            }
        }

        private fun showPopupMenu(event: Event) {
            val popupBinding = PopupMenuBinding.inflate(LayoutInflater.from(binding.root.context))
            val popupView = popupBinding.root

            val rootView = binding.root.rootView
            val width = (rootView.width * 0.278).toInt()
            val height = (rootView.height * 0.110).toInt()

            popupWindow = PopupWindow(
                popupView,
                width,
                height,
                true
            ).apply {
                isOutsideTouchable = true
                isFocusable = true
                setBackgroundDrawable(ContextCompat.getDrawable(binding.root.context, R.drawable.bg_popup_menu))
                animationStyle = R.style.PopupAnimation
            }

            overlayView = View(binding.root.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.neutral_0_30_percent))
                alpha = 0f
                setOnClickListener { dismissPopupMenu() }
            }

            val rootViewGroup = binding.root.rootView as? ViewGroup
            rootViewGroup?.addView(overlayView)

            overlayView?.animate()
                ?.alpha(0.7f)
                ?.setDuration(250)
                ?.start()

            val location = IntArray(2)
            binding.buttonMore.getLocationOnScreen(location)
            val buttonX = location[0]
            val buttonY = location[1]
            val buttonWidth = binding.buttonMore.width

            val menuX = buttonX + buttonWidth - width

            popupWindow?.showAtLocation(
                binding.root,
                android.view.Gravity.NO_GRAVITY,
                menuX,
                buttonY
            )

            popupBinding.menuEdit.setOnClickListener {
                dismissPopupMenu()
                // TODO: Add edit logic
            }

            popupBinding.menuDelete.setOnClickListener {
                onDeleteClicked(event)
                dismissPopupMenu()
            }

            popupWindow?.setOnDismissListener {
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

        private fun formatTimeAgo(createdAt: OffsetDateTime): String {
            val now = OffsetDateTime.now()
            val minutes = ChronoUnit.MINUTES.between(createdAt, now)
            val hours = ChronoUnit.HOURS.between(createdAt, now)
            val days = ChronoUnit.DAYS.between(createdAt, now)

            return when {
                minutes < 1 -> "now"
                minutes < 60 -> "${minutes}m"
                hours < 24 -> "${hours}h"
                days < 7 -> "${days}d"
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("MM/dd")
                    createdAt.format(formatter)
                }
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}