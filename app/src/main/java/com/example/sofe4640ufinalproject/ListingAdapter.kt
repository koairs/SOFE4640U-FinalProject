package com.example.sofe4640ufinalproject

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ListingAdapter(private var items: List<Listing>) :
    RecyclerView.Adapter<ListingAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val price: TextView = view.findViewById(R.id.tvPrice)
        val meta: TextView = view.findViewById(R.id.tvMeta)
        val thumb: ImageView = view.findViewById(R.id.ivThumb)
        val play: TextView = view.findViewById(R.id.tvPlay)
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
        holder.meta.text = "${item.category} • ${item.distance}"

        if (item.mediaUri != null) {
            Glide.with(holder.thumb).load(Uri.parse(item.mediaUri))
                .centerCrop()
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(holder.thumb)
            holder.play.visibility = if (item.mediaType == "video") View.VISIBLE else View.GONE
        } else {
            holder.thumb.setImageResource(R.drawable.profile)
            holder.play.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val ctx = it.context
            ctx.startActivity(
                Intent(ctx, ListingDetailActivity::class.java).apply {
                    putExtra("title", item.title)
                    putExtra("price", item.price)
                    putExtra("category", item.category)
                    putExtra("mediaUri", item.mediaUri)
                    putExtra("mediaType", item.mediaType)
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