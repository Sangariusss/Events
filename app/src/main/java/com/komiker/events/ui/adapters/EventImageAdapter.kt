package com.komiker.events.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class EventImageAdapter(
    private val imageNames: List<String>,
    private val supabaseClient: SupabaseClientProvider
) : RecyclerView.Adapter<EventImageAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageName = imageNames[position]
        val bucketName = "event-images"

        val signedUrl = runBlocking {
            supabaseClient.client.storage.from(bucketName).createSignedUrl(imageName, 60.seconds)
        }

        Glide.with(holder.itemView.context)
            .load(signedUrl)
            .apply(RequestOptions()
                .placeholder(R.drawable.img_event_placeholder)
                .fitCenter()
                .override(1000, 327)
                .optionalTransform(RoundedCorners(20)))
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = imageNames.size
}