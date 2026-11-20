package com.example.sofe4640ufinalproject

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import java.io.File

class postCreation : AppCompatActivity() {

    private var mediaUri: Uri? = null
    private var mediaType: String? = null
    private var pendingUri: Uri? = null

    private lateinit var previewContainer: FrameLayout
    private lateinit var ivPreview: ImageView
    private lateinit var tvVideoBadge: TextView

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) setMedia(pendingUri, "photo") else toast("Photo cancelled")
    }

    private val recordVideo = registerForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { ok ->
        if (ok) setMedia(pendingUri, "video") else toast("Video cancelled")
    }

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val mime = contentResolver.getType(uri) ?: ""
        val isVideo = mime.startsWith("video")
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: if (isVideo) "mp4" else "jpg"
        val copied = copyToAppStorage(uri, ext)
        if (copied != null) setMedia(copied, if (isVideo) "video" else "photo")
        else toast("Couldn't import that file")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_post_creation)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val etCategory = findViewById<EditText>(R.id.etCategory)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitListing)
        previewContainer = findViewById(R.id.previewContainer)
        ivPreview = findViewById(R.id.ivPreview)
        tvVideoBadge = findViewById(R.id.tvVideoBadge)

        findViewById<Button>(R.id.btnTakePhoto).setOnClickListener {
            pendingUri = newMediaUri("jpg")
            takePhoto.launch(pendingUri!!)
        }
        findViewById<Button>(R.id.btnRecordVideo).setOnClickListener {
            pendingUri = newMediaUri("mp4")
            recordVideo.launch(pendingUri!!)
        }
        findViewById<Button>(R.id.btnGallery).setOnClickListener {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        }
        findViewById<Button>(R.id.btnRemoveMedia).setOnClickListener { clearMedia() }

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
                    etDescription.text.toString().trim(),
                    etPrice.text.toString().trim(),
                    etCategory.text.toString().trim(),
                    mediaUri?.toString(),
                    mediaType
                ) { success ->
                    if (success) {
                        toast("Listing published")
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

    private fun newMediaFile(ext: String): File {
        val dir = File(getExternalFilesDir(null), "media").apply { mkdirs() }
        return File(dir, "MEDIA_${System.currentTimeMillis()}.$ext")
    }

    private fun newMediaUri(ext: String): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", newMediaFile(ext))

    private fun copyToAppStorage(source: Uri, ext: String): Uri? = try {
        val file = newMediaFile(ext)
        contentResolver.openInputStream(source)?.use { input ->
            file.outputStream().use { out -> input.copyTo(out) }
        }
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    } catch (e: Exception) {
        null
    }

    private fun setMedia(uri: Uri?, type: String) {
        if (uri == null) return
        mediaUri = uri
        mediaType = type
        previewContainer.visibility = View.VISIBLE
        tvVideoBadge.visibility = if (type == "video") View.VISIBLE else View.GONE
        Glide.with(this).load(uri).centerCrop().into(ivPreview)
        findViewById<View>(R.id.svPostCreation)?.post {
            (findViewById<View>(R.id.svPostCreation) as? ScrollView)?.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun clearMedia() {
        mediaUri = null
        mediaType = null
        previewContainer.visibility = View.GONE
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
