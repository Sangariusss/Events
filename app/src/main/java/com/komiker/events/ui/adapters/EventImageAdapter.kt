package com.komiker.events.ui.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.komiker.events.R

class EventImageAdapter(
    private val imageUrls: List<String>,
    private val onImageLoadedListener: (Int) -> Unit
) : RecyclerView.Adapter<EventImageAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_event_image, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val displayMetrics = holder.itemView.context.resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.911).toInt()
        val height = (displayMetrics.heightPixels * 0.327).toInt()
        val radius = (20 * displayMetrics.density).toInt()

        Glide.with(holder.itemView.context)
            .load(imageUrls[position])
            .apply(RequestOptions()
                .placeholder(R.color.neutral_100)
                .error(R.drawable.img_event_placeholder)
                .centerCrop()
                .override(width, height)
                .transform(RoundedCorners(radius)))
            .listener(object : com.bumptech.glide.request.RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    if (holder.absoluteAdapterPosition != RecyclerView.NO_POSITION) onImageLoadedListener(holder.absoluteAdapterPosition)
                    return false
                }
                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    if (holder.absoluteAdapterPosition != RecyclerView.NO_POSITION) onImageLoadedListener(holder.absoluteAdapterPosition)
                    return false
                }
            })
            .into(holder.imageView)
    }

    override fun getItemCount() = imageUrls.size
}