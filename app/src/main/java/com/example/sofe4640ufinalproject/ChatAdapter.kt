package com.example.sofe4640ufinalproject

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private var messages: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SENT) {
            val v = inflater.inflate(R.layout.item_message_sent, parent, false)
            SentVH(v)
        } else {
            val v = inflater.inflate(R.layout.item_message_received, parent, false)
            ReceivedVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(msg.timestamp))

        val isCB = ColorBlindHelper.isColorBlindMode(holder.itemView.context)
        ColorBlindHelper.recolorViewTree(holder.itemView, isCB)

        val displayText = if (msg.isAudio) "🎤 Voice Message (Tap to Play)" else msg.messageText

        val clickListener = View.OnClickListener {
            if (msg.isAudio && !msg.audioUri.isNullOrBlank()) {
                val ctx = it.context
                try {
                    val mp = MediaPlayer().apply {
                        setDataSource(msg.audioUri)
                        prepare()
                        start()
                    }
                    mp.setOnCompletionListener { it.release() }
                    Toast.makeText(ctx, "Playing voice message...", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(ctx, "Audio file not found or cannot play", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (holder is SentVH) {
            holder.text.text = displayText
            holder.time.text = timeStr
            holder.itemView.setOnClickListener(clickListener)
        } else if (holder is ReceivedVH) {
            holder.sender.text = msg.senderId
            holder.text.text = displayText
            holder.time.text = timeStr
            holder.itemView.setOnClickListener(clickListener)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun update(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    class SentVH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.tvMessageText)
        val time: TextView = v.findViewById(R.id.tvTime)
    }

    class ReceivedVH(v: View) : RecyclerView.ViewHolder(v) {
        val sender: TextView = v.findViewById(R.id.tvSender)
        val text: TextView = v.findViewById(R.id.tvMessageText)
        val time: TextView = v.findViewById(R.id.tvTime)
    }
}
