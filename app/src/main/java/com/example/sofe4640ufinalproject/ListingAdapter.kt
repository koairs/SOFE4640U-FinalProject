package com.example.sofe4640ufinalproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ListingAdapter(private var items: List<Listing>) :
    RecyclerView.Adapter<ListingAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val price: TextView = view.findViewById(R.id.tvPrice)
        val meta: TextView = view.findViewById(R.id.tvMeta)
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
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<Listing>) {
        items = newItems
        notifyDataSetChanged()
    }
}