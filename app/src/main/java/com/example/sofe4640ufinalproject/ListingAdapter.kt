package com.example.sofe4640ufinalproject

import android.content.Intent
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class ListingAdapter(private var items: List<Listing>) :
    RecyclerView.Adapter<ListingAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val price: TextView = view.findViewById(R.id.tvPrice)
        val meta: TextView = view.findViewById(R.id.tvMeta)
        val thumb: ImageView = view.findViewById(R.id.ivThumb)
        val play: TextView = view.findViewById(R.id.tvPlay)
        val ownerAvatar: ImageView = view.findViewById(R.id.ivOwnerAvatar)
        val soldBadge: TextView = view.findViewById(R.id.soldBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_listing, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.price.text = item.price

        val isCB = ColorBlindHelper.isColorBlindMode(holder.itemView.context)
        ColorBlindHelper.recolorViewTree(holder.itemView, isCB)

        RatingStore.summary(holder.itemView.context, item.ownerId) { summary ->
            val rating = RatingStore.label(summary)
            holder.meta.text = "${item.category} • ${item.distance} • $rating"
        }

        ProfileStore.getPhoto(holder.itemView.context, item.ownerId) { path ->
            if (path != null && File(path).exists()) {
                Glide.with(holder.itemView.context).load(File(path)).centerCrop().into(holder.ownerAvatar)
            } else {
                holder.ownerAvatar.setImageResource(R.drawable.profile)
            }
        }

        holder.ownerAvatar.setOnClickListener {
            val ctx = it.context
            ctx.startActivity(
                Intent(ctx, Profile::class.java).apply {
                    putExtra("userId", item.ownerId)
                }
            )
        }

        if (!item.mediaUri.isNullOrBlank()) {
            val file = File(item.mediaUri)
            val loadTarget = if (file.exists()) file else item.mediaUri
            Glide.with(holder.thumb).load(loadTarget)
                .centerCrop()
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(holder.thumb)
            holder.play.visibility = if (item.mediaType == "video") View.VISIBLE else View.GONE
        } else if (!item.mediaBase64.isNullOrBlank()) {
            try {
                val decoded = Base64.decode(item.mediaBase64, Base64.DEFAULT)
                val cacheFile = File(holder.itemView.context.cacheDir, "listing_thumb_${item.id}.jpg")
                if (!cacheFile.exists() || cacheFile.length() == 0L) {
                    cacheFile.writeBytes(decoded)
                }
                Glide.with(holder.thumb).load(cacheFile)
                    .centerCrop()
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(holder.thumb)
            } catch (_: Exception) {
                holder.thumb.setImageResource(R.drawable.profile)
            }
            holder.play.visibility = if (item.mediaType == "video") View.VISIBLE else View.GONE
        } else {
            holder.thumb.setImageResource(R.drawable.profile)
            holder.play.visibility = View.GONE
        }

        if (item.isSold) {
            holder.soldBadge.visibility = View.VISIBLE
            holder.itemView.alpha = 0.75f
        } else {
            holder.soldBadge.visibility = View.GONE
            holder.itemView.alpha = 1.0f
        }

        holder.itemView.setOnClickListener {
            val ctx = it.context
            ctx.startActivity(
                Intent(ctx, ListingDetailActivity::class.java).apply {
                    putExtra("listingId", item.id)
                    putExtra("title", item.title)
                    putExtra("description", item.description)
                    putExtra("price", item.price)
                    putExtra("category", item.category)
                    putExtra("isSold", item.isSold)
                    putExtra("mediaUri", item.mediaUri)
                    putExtra("mediaType", item.mediaType)
                    putExtra("mediaBase64", item.mediaBase64)
                    putExtra("ownerId", item.ownerId)
                    putExtra("location", item.location)
                }
            )
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<Listing>) {
        items = newItems
        notifyDataSetChanged()
    }
}