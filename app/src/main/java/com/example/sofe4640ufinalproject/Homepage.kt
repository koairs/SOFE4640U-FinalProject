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

class Homepage : AppCompatActivity() {

    private lateinit var adapter: ListingAdapter
    private lateinit var etSearch: EditText
    private var allListings: List<Listing> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_homepage)

        adapter = ListingAdapter(emptyList())
        val rv = findViewById<RecyclerView>(R.id.rvListings)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        etSearch = findViewById(R.id.etSearch)
        etSearch.doOnTextChanged { _, _, _, _ -> applyFilter() }

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
        ListingStore.loadAll(this) {
            allListings = it
            applyFilter()
        }
    }

    private fun applyFilter() {
        val q = etSearch.text.toString().trim()
        adapter.update(
            allListings.filter {
                it.title.contains(q, ignoreCase = true) ||
                        it.category.contains(q, ignoreCase = true)
            }
        )
    }
}