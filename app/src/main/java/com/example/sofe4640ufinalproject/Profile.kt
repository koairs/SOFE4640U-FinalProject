package com.example.sofe4640ufinalproject

import android.content.Intent
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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class Profile : AppCompatActivity() {

    private lateinit var targetUserId: String
    private lateinit var me: String
    private lateinit var ivAvatar: ImageView
    private lateinit var adapter: ListingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        me = ListingStore.currentUserId()
        targetUserId = intent.getStringExtra("userId") ?: me

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        ivAvatar = findViewById(R.id.ivProfileAvatar)

        val displayName = if (targetUserId.contains("@")) targetUserId.substringBefore("@") else targetUserId
        tvName.text = displayName
        tvEmail.text = targetUserId

        val actionsLayout = findViewById<View>(R.id.layoutProfileActions)
        val btnMessage = findViewById<Button>(R.id.btnMessageUser)
        val btnRate = findViewById<Button>(R.id.btnRateUser)

        if (targetUserId == me) {
            actionsLayout.visibility = View.GONE
        } else {
            actionsLayout.visibility = View.VISIBLE
            btnMessage.setOnClickListener {
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("otherUser", targetUserId)
                }
                startActivity(intent)
            }
            btnRate.setOnClickListener {
                RatingStore.hasInteracted(this, me, targetUserId) { interacted ->
                    if (!interacted) {
                        Toast.makeText(this, "Message the seller first, then you can rate them", Toast.LENGTH_LONG).show()
                    } else {
                        showRateDialog()
                    }
                }
            }
        }

        val rv = findViewById<RecyclerView>(R.id.rvUserListings)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ListingAdapter(emptyList())
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        ColorBlindHelper.apply(this)
        loadProfileData()
    }

    private fun loadProfileData() {
        // Load Avatar
        ProfileStore.getPhoto(this, targetUserId) { path ->
            if (path != null && File(path).exists()) {
                Glide.with(this).load(File(path)).centerCrop().into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.profile)
            }
        }

        // Load Rating
        RatingStore.summary(this, targetUserId) { s ->
            findViewById<TextView>(R.id.tvProfileRating)?.text =
                if (s.count == 0) "New" else String.format("%.1f ★", s.average)
        }

        // Load User Listings
        ListingStore.loadUserListings(this, targetUserId) { list ->
            adapter.update(list)
            findViewById<TextView>(R.id.tvProfileListingsCount)?.text = list.size.toString()
        }
    }

    private fun showRateDialog() {
        RatingStore.myRating(this, me, targetUserId) { existingStars ->
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
                .setTitle("Rate ${targetUserId.substringBefore("@")}")
                .setView(box)
                .setPositiveButton("Submit") { _, _ ->
                    val stars = bar.rating.toInt().coerceIn(1, 5)
                    RatingStore.submit(
                        this, me, targetUserId, stars, comment.text.toString().trim()
                    ) { ok ->
                        Toast.makeText(
                            this,
                            if (ok) "Thanks for rating!" else "Couldn't save rating",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadProfileData()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}