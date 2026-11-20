package com.example.sofe4640ufinalproject

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import java.io.File

class ListingDetailActivity : AppCompatActivity() {

    private lateinit var ownerId: String
    private lateinit var me: String

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
        ownerId = intent.getStringExtra("ownerId") ?: "Seller"
        me = ListingStore.currentUserId()

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

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        findViewById<Button>(R.id.btnContactSeller)?.setOnClickListener {
            val chat = Intent(this, ChatActivity::class.java).apply {
                putExtra("otherUser", ownerId)
                putExtra("itemTitle", title)
            }
            startActivity(chat)
        }

        findViewById<TextView>(R.id.tvSellerName).text = ownerId.substringBefore("@")

        val btnRate = findViewById<Button>(R.id.btnRateSeller)
        if (ownerId == me) btnRate.visibility = View.GONE
        btnRate.setOnClickListener {
            RatingStore.hasInteracted(this, me, ownerId) { interacted ->
                if (!interacted) {
                    Toast.makeText(this, "Message the seller first, then you can rate them", Toast.LENGTH_LONG).show()
                } else {
                    showRateDialog()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSeller()
    }

    private fun refreshSeller() {
        RatingStore.summary(this, ownerId) { summary ->
            findViewById<TextView>(R.id.tvSellerRating).text = RatingStore.label(summary)
        }

        val iv = findViewById<ImageView>(R.id.ivSellerAvatar)
        ProfileStore.getPhoto(this, ownerId) { path ->
            if (path != null && File(path).exists()) {
                Glide.with(this).load(File(path)).centerCrop().into(iv)
            } else {
                iv.setImageResource(R.drawable.profile)
            }
        }
    }

    private fun showRateDialog() {
        RatingStore.myRating(this, me, ownerId) { existingStars ->
            val bar = RatingBar(this).apply {
                numStars = 5
                stepSize = 1f
                rating = (existingStars ?: 5).toFloat()
            }
            val comment = EditText(this).apply { hint = "Comment (optional)" }
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 24, 48, 0)
                addView(
                    bar,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { gravity = Gravity.CENTER_HORIZONTAL }
                )
                addView(comment)
            }

            AlertDialog.Builder(this)
                .setTitle("Rate ${ownerId.substringBefore("@")}")
                .setView(box)
                .setPositiveButton("Submit") { _, _ ->
                    val stars = bar.rating.toInt().coerceIn(1, 5)
                    RatingStore.submit(
                        this, me, ownerId, stars, comment.text.toString().trim()
                    ) { ok ->
                        Toast.makeText(
                            this,
                            if (ok) "Thanks for rating!" else "Couldn't save rating",
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshSeller()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
