package com.example.sofe4640ufinalproject

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class ListingDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listing_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val title = intent.getStringExtra("title") ?: "Listing Detail"
        val price = intent.getStringExtra("price") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val mediaUriStr = intent.getStringExtra("mediaUri")
        val mediaType = intent.getStringExtra("mediaType")

        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvPrice = findViewById<TextView>(R.id.tvDetailPrice)
        val tvCategory = findViewById<TextView>(R.id.tvDetailCategory)
        val ivDetail = findViewById<ImageView>(R.id.ivDetail)
        val vvDetail = findViewById<VideoView>(R.id.vvDetail)

        tvTitle.text = title
        tvPrice.text = price
        tvCategory.text = category

        if (!mediaUriStr.isNullOrBlank()) {
            val uri = Uri.parse(mediaUriStr)
            if (mediaType == "video") {
                ivDetail.visibility = View.GONE
                vvDetail.visibility = View.VISIBLE
                vvDetail.setVideoURI(uri)
                vvDetail.setOnPreparedListener { mp -> mp.isLooping = true }
                vvDetail.start()
            } else {
                vvDetail.visibility = View.GONE
                ivDetail.visibility = View.VISIBLE
                Glide.with(this).load(uri).centerCrop().into(ivDetail)
            }
        } else {
            vvDetail.visibility = View.GONE
            ivDetail.visibility = View.VISIBLE
            ivDetail.setImageResource(R.drawable.profile)
        }

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnContactSeller)?.setOnClickListener {
            startActivity(Intent(this, Messages::class.java))
        }
    }
}