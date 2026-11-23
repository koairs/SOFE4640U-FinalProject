package com.example.sofe4640ufinalproject

import android.content.Intent
import android.graphics.Color
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
import com.google.android.material.button.MaterialButton
import java.io.File

class ListingDetailActivity : AppCompatActivity() {

    private lateinit var listingId: String
    private lateinit var ownerId: String
    private lateinit var me: String
    private lateinit var currentTitle: String
    private lateinit var currentDesc: String
    private lateinit var currentPrice: String
    private lateinit var currentCategory: String
    private var currentLocation: String = ""
    private var isSold: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listing_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        listingId = intent.getStringExtra("listingId") ?: ""
        currentTitle = intent.getStringExtra("title") ?: "Listing Detail"
        currentDesc = intent.getStringExtra("description") ?: ""
        currentPrice = intent.getStringExtra("price") ?: ""
        currentCategory = intent.getStringExtra("category") ?: ""
        currentLocation = intent.getStringExtra("location") ?: ""
        isSold = intent.getBooleanExtra("isSold", false)
        val mediaUriStr = intent.getStringExtra("mediaUri")
        val mediaType = intent.getStringExtra("mediaType")
        ownerId = intent.getStringExtra("ownerId") ?: "Seller"
        me = ListingStore.currentUserId()

        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvDescription = findViewById<TextView>(R.id.tvDetailDescription)
        val tvPrice = findViewById<TextView>(R.id.tvDetailPrice)
        val tvCategory = findViewById<TextView>(R.id.tvDetailCategory)
        val tvLocation = findViewById<TextView>(R.id.tvDetailLocation)
        val ivDetail = findViewById<ImageView>(R.id.ivDetail)
        val vvDetail = findViewById<VideoView>(R.id.vvDetail)

        tvTitle.text = currentTitle
        tvDescription.text = currentDesc.ifBlank { "No description provided." }
        tvPrice.text = currentPrice
        tvCategory.text = currentCategory
        val distanceText = LocationHelper.calculateDistance(this, currentLocation)
        tvLocation.text = if (currentLocation.isNotBlank()) "$currentLocation • $distanceText" else distanceText

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

        val layoutOwner = findViewById<View>(R.id.layoutOwnerActions)
        val layoutBuyer = findViewById<View>(R.id.layoutBuyerActions)
        val btnRate = findViewById<Button>(R.id.btnRateSeller)

        if (ownerId == me) {
            layoutOwner.visibility = View.VISIBLE
            layoutBuyer.visibility = View.GONE
            btnRate.visibility = View.GONE
        } else {
            layoutOwner.visibility = View.GONE
            layoutBuyer.visibility = View.VISIBLE
            btnRate.visibility = View.VISIBLE
        }

        updateSoldUI()

        findViewById<Button>(R.id.btnContactSeller)?.setOnClickListener {
            val chat = Intent(this, ChatActivity::class.java).apply {
                putExtra("otherUser", ownerId)
                putExtra("itemTitle", currentTitle)
                putExtra("listingOwnerId", ownerId)
            }
            startActivity(chat)
        }

        findViewById<Button>(R.id.btnEditListing)?.setOnClickListener {
            showEditDialog()
        }

        findViewById<Button>(R.id.btnToggleSold)?.setOnClickListener {
            val nextSoldState = !isSold
            ListingStore.markAsSold(this, listingId, nextSoldState) { success ->
                if (success) {
                    isSold = nextSoldState
                    updateSoldUI()
                    Toast.makeText(
                        this,
                        if (isSold) "Listing marked as Sold" else "Listing marked as Available",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this, "Failed to update item status", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.btnDeleteListing)?.setOnClickListener {
            showDeleteDialog()
        }

        val tvSellerName = findViewById<TextView>(R.id.tvSellerName)
        tvSellerName.text = ownerId.substringBefore("@")

        val openSellerProfile = View.OnClickListener {
            val intent = Intent(this, Profile::class.java).apply {
                putExtra("userId", ownerId)
            }
            startActivity(intent)
        }
        findViewById<View>(R.id.ivSellerAvatar)?.setOnClickListener(openSellerProfile)
        tvSellerName.setOnClickListener(openSellerProfile)

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

    private fun updateSoldUI() {
        val cardSold = findViewById<View>(R.id.cardSoldStatus)
        val btnToggleSold = findViewById<MaterialButton>(R.id.btnToggleSold)
        val btnContactSeller = findViewById<MaterialButton>(R.id.btnContactSeller)

        if (isSold) {
            cardSold?.visibility = View.VISIBLE
            btnToggleSold?.text = "Mark Available"
            btnToggleSold?.setBackgroundColor(Color.parseColor("#475569"))
            btnContactSeller?.isEnabled = false
            btnContactSeller?.text = "Item Sold"
        } else {
            cardSold?.visibility = View.GONE
            btnToggleSold?.text = "Mark Sold"
            btnToggleSold?.setBackgroundColor(Color.parseColor("#059669"))
            btnContactSeller?.isEnabled = true
            btnContactSeller?.text = "Message Seller"
        }
    }

    override fun onResume() {
        super.onResume()
        ColorBlindHelper.apply(this)
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

    private fun showEditDialog() {
        val etTitle = EditText(this).apply { setText(currentTitle); hint = "Title" }
        val etDesc = EditText(this).apply { setText(currentDesc); hint = "Description" }
        val etPrice = EditText(this).apply { setText(currentPrice); hint = "Price" }
        val etCat = EditText(this).apply { setText(currentCategory); hint = "Category" }
        val etLoc = EditText(this).apply { setText(currentLocation); hint = "Location" }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(etTitle)
            addView(etDesc)
            addView(etPrice)
            addView(etCat)
            addView(etLoc)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Listing")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = etTitle.text.toString().trim()
                val newDesc = etDesc.text.toString().trim()
                val newPrice = etPrice.text.toString().trim()
                val newCat = etCat.text.toString().trim()
                val newLoc = etLoc.text.toString().trim()
                if (newTitle.isNotEmpty() && newPrice.isNotEmpty() && newCat.isNotEmpty()) {
                    ListingStore.update(this, listingId, newTitle, newDesc, newPrice, newCat, newLoc) { success ->
                        Toast.makeText(this, if (success) "Listing updated" else "Update failed", Toast.LENGTH_SHORT).show()
                        if (success) {
                            currentTitle = newTitle
                            currentDesc = newDesc
                            currentPrice = newPrice
                            currentCategory = newCat
                            currentLocation = newLoc
                            findViewById<TextView>(R.id.tvDetailTitle).text = newTitle
                            findViewById<TextView>(R.id.tvDetailDescription).text = newDesc.ifBlank { "No description provided." }
                            findViewById<TextView>(R.id.tvDetailPrice).text = newPrice
                            findViewById<TextView>(R.id.tvDetailCategory).text = newCat
                        }
                    }
                } else {
                    Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Listing")
            .setMessage("Are you sure you want to delete this listing?")
            .setPositiveButton("Delete") { _, _ ->
                ListingStore.delete(this, listingId) { success ->
                    Toast.makeText(this, if (success) "Listing deleted" else "Delete failed", Toast.LENGTH_SHORT).show()
                    if (success) finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
