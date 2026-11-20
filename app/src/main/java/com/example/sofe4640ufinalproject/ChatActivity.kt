package com.example.sofe4640ufinalproject

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class ChatActivity : AppCompatActivity() {

    private lateinit var otherUser: String
    private lateinit var itemTitle: String
    private lateinit var adapter: ChatAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var etInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        otherUser = intent.getStringExtra("otherUser") ?: "Seller"
        itemTitle = intent.getStringExtra("itemTitle") ?: ""

        findViewById<TextView>(R.id.tvChatPartner).text = otherUser
        val tvTag = findViewById<TextView>(R.id.tvItemTitleTag)
        if (itemTitle.isNotBlank()) {
            tvTag.visibility = View.VISIBLE
            tvTag.text = "Item: $itemTitle"
        } else {
            tvTag.visibility = View.GONE
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        rvMessages = findViewById(R.id.rvChatMessages)
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.layoutManager = layoutManager

        val currentUserId = MessageStore.currentUserId()
        adapter = ChatAdapter(emptyList(), currentUserId)
        rvMessages.adapter = adapter

        etInput = findViewById(R.id.etMessageInput)
        val btnSend = findViewById<MaterialButton>(R.id.btnSendMessage)

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotBlank()) {
                MessageStore.sendMessage(this, otherUser, itemTitle, text) { success ->
                    if (success) {
                        etInput.setText("")
                        loadMessages()
                    } else {
                        Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        loadMessages()
    }

    override fun onResume() {
        super.onResume()
        ColorBlindHelper.apply(this)
    }

    private fun loadMessages() {
        MessageStore.getMessagesBetween(this, otherUser) { list ->
            adapter.update(list)
            if (list.isNotEmpty()) {
                rvMessages.smoothScrollToPosition(list.size - 1)
            }
        }
    }
}