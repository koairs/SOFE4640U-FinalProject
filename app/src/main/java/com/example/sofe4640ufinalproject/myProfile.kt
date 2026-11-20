package com.example.sofe4640ufinalproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.io.File

class myProfile : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView

    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        ProfileStore.setPhoto(this, ListingStore.currentUserId(), uri) { success ->
            if (success) {
                loadAvatar()
                Toast.makeText(this, "Profile photo updated & saved to cloud", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Couldn't save photo to cloud", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_profile)

        val user = FirebaseAuth.getInstance().currentUser
        val tvName = findViewById<TextView>(R.id.textView)
        val tvEmail = findViewById<TextView>(R.id.tvUserEmail)
        ivAvatar = findViewById(R.id.imageView)

        if (user != null) {
            val displayName = user.displayName?.ifBlank { null } ?: user.email?.substringBefore("@") ?: "User"
            tvName?.text = displayName
            tvEmail?.text = user.email ?: "No email available"
        } else {
            tvName?.text = "Guest User"
            tvEmail?.text = "Not logged in"
        }

        ivAvatar.setOnClickListener {
            pickPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
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
        loadAvatar()
        ListingStore.loadMine(this) { list ->
            findViewById<TextView>(R.id.tvListingsCount)?.text = list.size.toString()
        }
        RatingStore.summary(this, ListingStore.currentUserId()) { s ->
            findViewById<TextView>(R.id.tvMyRating)?.text =
                if (s.count == 0) "New" else String.format("%.1f ★", s.average)
        }
    }

    private fun loadAvatar() {
        ProfileStore.getPhoto(this, ListingStore.currentUserId()) { path ->
            if (path != null && File(path).exists()) {
                Glide.with(this).load(File(path)).centerCrop().into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.profile)
            }
        }
    }
}
