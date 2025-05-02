package com.komiker.events.ui.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komiker.events.R
import com.komiker.events.data.database.entities.Proposal
import com.komiker.events.databinding.ItemProposalBinding
import com.komiker.events.glide.CircleCropTransformation
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

class ProposalsAdapter : ListAdapter<Proposal, ProposalsAdapter.ProposalViewHolder>(ProposalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProposalViewHolder {
        val binding = ItemProposalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProposalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProposalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProposalViewHolder(private val binding: ItemProposalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(proposal: Proposal) {
            binding.textUserName.text = proposal.username
            binding.textContent.text = proposal.content
            binding.textLikesCount.text = proposal.likesCount.toString()

            val now = OffsetDateTime.now()
            binding.textTime.text = formatTimeAgo(proposal.createdAt, now)

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
        }

        private fun formatTimeAgo(createdAt: OffsetDateTime, now: OffsetDateTime): String {
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