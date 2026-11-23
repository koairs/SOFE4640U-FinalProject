package com.example.sofe4640ufinalproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup

class Messages : AppCompatActivity() {

    private lateinit var adapter: ConversationAdapter
    private lateinit var etSearch: EditText
    private lateinit var cgMessageFilters: ChipGroup
    private var allConversations: List<Conversation> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_messages)

        val rv = findViewById<RecyclerView>(R.id.rvMessages)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ConversationAdapter(emptyList()) { conv ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("otherUser", conv.otherUser)
                putExtra("itemTitle", conv.itemTitle)
            }
            startActivity(intent)
        }
        rv.adapter = adapter

        etSearch = findViewById(R.id.etSearchMessages)
        etSearch.doOnTextChanged { _, _, _, _ -> applyFilter() }

        cgMessageFilters = findViewById(R.id.cgMessageFilters)
        cgMessageFilters.setOnCheckedStateChangeListener { _, _ -> applyFilter() }

        findViewById<ImageButton>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, myProfile::class.java))
        }
        findViewById<Button>(R.id.btnMessages).setOnClickListener {
            startActivity(Intent(this, Messages::class.java))
        }
        findViewById<Button>(R.id.btnMyListings).setOnClickListener {
            startActivity(Intent(this, myListings::class.java))
        }
        findViewById<Button>(R.id.btnHome).setOnClickListener {
            startActivity(Intent(this, Homepage::class.java))
        }
        findViewById<Button>(R.id.btnPost).setOnClickListener {
            startActivity(Intent(this, postCreation::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        ColorBlindHelper.apply(this)
        MessageStore.getRecentConversations(this) { list ->
            allConversations = list
            applyFilter()
        }
    }

    private fun applyFilter() {
        val q = etSearch.text.toString().trim()
        val checkedChipId = cgMessageFilters.checkedChipId

        adapter.update(
            allConversations.filter { conv ->
                val matchesSearch = conv.otherUser.contains(q, ignoreCase = true) ||
                        conv.lastMessage.contains(q, ignoreCase = true) ||
                        conv.itemTitle.contains(q, ignoreCase = true)

                val matchesChip = when (checkedChipId) {
                    R.id.chipBuying -> !conv.isSelling
                    R.id.chipSelling -> conv.isSelling
                    R.id.chipUnread -> conv.isUnread
                    else -> true
                }

                matchesSearch && matchesChip
            }
        )
    }
}
