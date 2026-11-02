package com.example.sofe4640ufinalproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class postCreation : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_post_creation)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val etCategory = findViewById<EditText>(R.id.etCategory)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitListing)

        findViewById<Button>(R.id.btnAddMedia).setOnClickListener {
            Toast.makeText(this, "Camera/video capture coming next milestone", Toast.LENGTH_SHORT).show()
        }

        btnSubmit.setOnClickListener {
            var valid = true
            if (etTitle.text.isNullOrBlank()) { etTitle.error = "Required"; valid = false }
            if (etPrice.text.isNullOrBlank()) { etPrice.error = "Required"; valid = false }
            if (etCategory.text.isNullOrBlank()) { etCategory.error = "Required"; valid = false }
            if (valid) {
                btnSubmit.isEnabled = false
                ListingStore.add(
                    this,
                    etTitle.text.toString().trim(),
                    etPrice.text.toString().trim(),
                    etCategory.text.toString().trim()
                ) { success ->
                    if (success) {
                        Toast.makeText(this, "Listing saved", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        btnSubmit.isEnabled = true
                        Toast.makeText(this, "Save failed. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }
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
}