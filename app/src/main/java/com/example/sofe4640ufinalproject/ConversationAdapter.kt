package com.example.sofe4640ufinalproject

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private var conversations: List<Conversation>,
    private val onClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: ImageView = v.findViewById(R.id.ivAvatar)
        val tvOtherUser: TextView = v.findViewById(R.id.tvOtherUser)
        val tvLastMessage: TextView = v.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
        val tvItemTag: TextView = v.findViewById(R.id.tvItemTag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = conversations[position]
        holder.tvOtherUser.text = item.otherUser
        holder.tvLastMessage.text = item.lastMessage
        val timeStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.timestamp))
        holder.tvTime.text = timeStr

        if (item.itemTitle.isNotBlank()) {
            holder.tvItemTag.visibility = View.VISIBLE
            holder.tvItemTag.text = "Re: ${item.itemTitle}"
        } else {
            holder.tvItemTag.visibility = View.GONE
        }

        ProfileStore.getPhoto(holder.itemView.context, item.otherUser) { path ->
            if (path != null && File(path).exists()) {
                Glide.with(holder.itemView.context).load(File(path)).centerCrop().into(holder.ivAvatar)
            } else {
                holder.ivAvatar.setImageResource(R.drawable.profile)
            }
        }

        holder.ivAvatar.setOnClickListener {
            val ctx = it.context
            val intent = Intent(ctx, Profile::class.java).apply {
                putExtra("userId", item.otherUser)
            }
            ctx.startActivity(intent)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = conversations.size

    fun update(newConversations: List<Conversation>) {
        conversations = newConversations
        notifyDataSetChanged()
    }
}