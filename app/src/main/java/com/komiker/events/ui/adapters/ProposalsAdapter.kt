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
import com.komiker.events.data.database.models.Proposal
import com.komiker.events.databinding.ItemProposalBinding
import com.komiker.events.databinding.PopupMenuBinding
import com.komiker.events.glide.CircleCropTransformation
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

class ProposalsAdapter(
    private val currentUserId: String?,
    private val onDeleteClicked: (Proposal) -> Unit,
    private val navController: NavController
) : ListAdapter<Proposal, ProposalsAdapter.ProposalViewHolder>(ProposalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProposalViewHolder {
        val binding = ItemProposalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProposalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProposalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProposalViewHolder(private val binding: ItemProposalBinding) : RecyclerView.ViewHolder(binding.root) {
        private var popupWindow: PopupWindow? = null
        private var overlayView: View? = null

        fun bind(proposal: Proposal) {
            binding.textUserName.text = proposal.username
            binding.textContent.text = proposal.content
            binding.textLikesCount.text = proposal.likesCount.toString()

            binding.textTime.text = formatTimeAgo(proposal.createdAt)

            Glide.with(binding.imageProfile.context)
                .load(proposal.userAvatar)
                .placeholder(R.drawable.img_profile_placeholder)
                .transform(CircleCropTransformation())
                .into(binding.imageProfile)

            binding.buttonShare.setOnClickListener {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, proposal.content)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                binding.root.context.startActivity(shareIntent)
            }

            if (currentUserId != null && proposal.userId == currentUserId) {
                binding.textUserName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.blue_60))
                binding.buttonMore.setOnClickListener {
                    showPopupMenu(proposal)
                }
            } else {
                binding.textUserName.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.black))
                binding.buttonMore.setOnClickListener(null)
            }

            binding.root.setOnClickListener {
                val bundle = Bundle().apply {
                    putParcelable("proposal", proposal)
                }
                navController.navigate(
                    R.id.action_MainMenuFragment_to_ProposalDetailFragment,
                    bundle
                )
            }
        }

        private fun showPopupMenu(proposal: Proposal) {
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
                val bundle = Bundle().apply {
                    putParcelable("proposal", proposal)
                }
                navController.navigate(
                    R.id.action_MainMenuFragment_to_EditProposalFragment,
                    bundle
                )
            }

            popupBinding.menuDelete.setOnClickListener {
                onDeleteClicked(proposal)
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

    class ProposalDiffCallback : DiffUtil.ItemCallback<Proposal>() {
        override fun areItemsTheSame(oldItem: Proposal, newItem: Proposal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Proposal, newItem: Proposal): Boolean {
            return oldItem == newItem
        }
    }
}