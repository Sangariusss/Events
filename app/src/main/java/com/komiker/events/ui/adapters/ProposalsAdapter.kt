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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.komiker.events.R
import com.komiker.events.data.database.models.Proposal
import com.komiker.events.databinding.ItemProposalBinding
import com.komiker.events.databinding.PopupMenuBinding
import com.komiker.events.glide.CircleCropTransformation
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProposalsAdapter(
    private val currentUserId: String?,
    private val onDeleteClicked: (Proposal) -> Unit,
    private val navController: NavController,
    private val likeCache: MutableMap<String, Boolean>,
    private val likesCountCache: MutableMap<String, Int>,
    private val onLikeClicked: (String, Boolean, (Boolean, Int) -> Unit) -> Unit
) : ListAdapter<Proposal, ProposalsAdapter.ProposalViewHolder>(ProposalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProposalViewHolder {
        val binding = ItemProposalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProposalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProposalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ProposalViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val payload = payloads[0] as? Int
            payload?.let {
                holder.updateLikesCount(it)
            }
        } else {
            holder.bind(getItem(position))
        }
    }

    inner class ProposalViewHolder(private val binding: ItemProposalBinding) : RecyclerView.ViewHolder(binding.root) {
        private var popupWindow: PopupWindow? = null
        private var overlayView: View? = null
        private var isProcessingLike = false

        fun bind(proposal: Proposal) {
            binding.textUserName.text = proposal.username
            binding.textContent.text = proposal.content
            binding.textLikesCount.text = formatLikesCount(likesCountCache[proposal.id] ?: proposal.likesCount)
            binding.textTime.text = formatTimeAgo(proposal.createdAt)

            Glide.with(binding.imageProfile.context)
                .load(proposal.userAvatar)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.img_profile_placeholder)
                .transform(CircleCropTransformation())
                .into(binding.imageProfile)

            binding.buttonShare.setOnClickListener {
                shareProposal(proposal)
            }

            if (currentUserId != null && proposal.userId == currentUserId) {
                binding.textUserName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.blue_60))
                binding.buttonMore.setOnClickListener { showPopupMenu(proposal) }
            } else {
                binding.textUserName.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.black))
                binding.buttonMore.setOnClickListener(null)
            }

            binding.root.setOnClickListener {
                val bundle = Bundle().apply {
                    putParcelable("proposal", proposal)
                    putBoolean("isLiked", likeCache[proposal.id] ?: false)
                    putInt("likesCount", likesCountCache[proposal.id] ?: proposal.likesCount)
                }
                navController.navigate(R.id.action_MainMenuFragment_to_ProposalDetailFragment, bundle)
            }

            setupLikeButton(proposal)
        }

        fun updateLikesCount(likesCount: Int) {
            binding.textLikesCount.text = formatLikesCount(likesCount)
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                likesCountCache[getItem(position).id] = likesCount
            }
        }

        private fun shareProposal(proposal: Proposal) {
            val username = proposal.username.replace(" ", "_")
            val deepLink = "https://excito.netlify.app/@$username/proposal/${proposal.id}"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, deepLink)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            binding.root.context.startActivity(shareIntent)
        }

        private fun setupLikeButton(proposal: Proposal) {
            binding.imageLike.setOnClickListener {
                if (currentUserId != null && !isProcessingLike) {
                    isProcessingLike = true
                    binding.imageLike.isEnabled = false
                    binding.imageLike.alpha = 0.5f

                    val isCurrentlyLiked = likeCache[proposal.id] ?: false
                    val currentLikesCount = likesCountCache[proposal.id] ?: proposal.likesCount

                    if (isCurrentlyLiked) {
                        val newLikesCount = currentLikesCount - 1
                        binding.textLikesCount.text = formatLikesCount(newLikesCount.coerceAtLeast(0))
                        binding.imageLike.setImageResource(R.drawable.ic_heart_outline)
                        likeCache[proposal.id] = false
                        likesCountCache[proposal.id] = newLikesCount
                        onLikeClicked(proposal.id, false) { success, serverLikesCount ->
                            isProcessingLike = false
                            binding.imageLike.isEnabled = true
                            binding.imageLike.alpha = 1.0f
                            if (!success) {
                                binding.textLikesCount.text = formatLikesCount(currentLikesCount)
                                binding.imageLike.setImageResource(R.drawable.ic_heart)
                                likeCache[proposal.id] = true
                                likesCountCache[proposal.id] = currentLikesCount
                            } else {
                                likesCountCache[proposal.id] = serverLikesCount
                                binding.textLikesCount.text = formatLikesCount(serverLikesCount)
                            }
                        }
                    } else {
                        val newLikesCount = currentLikesCount + 1
                        binding.textLikesCount.text = formatLikesCount(newLikesCount)
                        binding.imageLike.setImageResource(R.drawable.ic_heart)
                        likeCache[proposal.id] = true
                        likesCountCache[proposal.id] = newLikesCount
                        onLikeClicked(proposal.id, true) { success, serverLikesCount ->
                            isProcessingLike = false
                            binding.imageLike.isEnabled = true
                            binding.imageLike.alpha = 1.0f
                            if (!success) {
                                binding.textLikesCount.text = formatLikesCount(currentLikesCount)
                                binding.imageLike.setImageResource(R.drawable.ic_heart_outline)
                                likeCache[proposal.id] = false
                                likesCountCache[proposal.id] = currentLikesCount
                            } else {
                                likesCountCache[proposal.id] = serverLikesCount
                                binding.textLikesCount.text = formatLikesCount(serverLikesCount)
                            }
                        }
                    }
                }
            }
            binding.imageLike.setImageResource(
                if (likeCache[proposal.id] == true) R.drawable.ic_heart else R.drawable.ic_heart_outline
            )
        }

        private fun showPopupMenu(proposal: Proposal) {
            val popupBinding = PopupMenuBinding.inflate(LayoutInflater.from(binding.root.context))
            val popupView = popupBinding.root

            val rootView = binding.root.rootView
            val width = (rootView.width * 0.278).toInt()
            val height = (rootView.height * 0.110).toInt()

            popupWindow = PopupWindow(popupView, width, height, true).apply {
                isOutsideTouchable = true
                isFocusable = true
                setBackgroundDrawable(ContextCompat.getDrawable(binding.root.context, R.drawable.bg_popup_menu))
                animationStyle = R.style.PopupAnimation
            }

            overlayView = View(binding.root.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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

            popupWindow?.showAtLocation(binding.root, android.view.Gravity.NO_GRAVITY, menuX, buttonY)

            popupBinding.menuEdit.setOnClickListener {
                dismissPopupMenu()
                val bundle = Bundle().apply {
                    putParcelable("proposal", proposal)
                }
                navController.navigate(R.id.action_MainMenuFragment_to_EditProposalFragment, bundle)
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
                else -> DateTimeFormatter.ofPattern("MM/dd").format(createdAt)
            }
        }

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

    class ProposalDiffCallback : DiffUtil.ItemCallback<Proposal>() {
        override fun areItemsTheSame(oldItem: Proposal, newItem: Proposal) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Proposal, newItem: Proposal) = oldItem == newItem
        override fun getChangePayload(oldItem: Proposal, newItem: Proposal): Any? {
            return if (oldItem.likesCount != newItem.likesCount) newItem.likesCount else null
        }
    }
}