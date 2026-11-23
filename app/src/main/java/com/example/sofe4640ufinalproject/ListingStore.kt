package com.example.sofe4640ufinalproject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

object ListingStore {

    fun currentUserId(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.email?.ifBlank { null } ?: user?.uid ?: "guest"
    }

    private fun compressImageToBase64(context: Context, uri: Uri): String {
        try {
            val inputStream = when (uri.scheme) {
                "file" -> FileInputStream(uri.path ?: return "")
                else -> try {
                    context.contentResolver.openInputStream(uri)
                } catch (_: Exception) {
                    null
                } ?: try {
                    FileInputStream(uri.path ?: return "")
                } catch (_: Exception) {
                    return ""
                }
            }
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return ""

            val maxSize = 800
            val width = bitmap.width
            val height = bitmap.height
            val ratio = width.toFloat() / height.toFloat()
            val newWidth: Int
            val newHeight: Int
            if (width > height) {
                newWidth = maxSize
                newHeight = (maxSize / ratio).toInt()
            } else {
                newHeight = maxSize
                newWidth = (maxSize * ratio).toInt()
            }

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val bytes = outputStream.toByteArray()
            outputStream.close()
            if (bitmap != resizedBitmap) {
                bitmap.recycle()
            }
            resizedBitmap.recycle()
            return Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("ListingStore", "Image compression failed", e)
            return ""
        }
    }

    fun add(
        context: Context,
        title: String,
        description: String,
        price: String,
        category: String,
        location: String,
        mediaUri: String?,
        mediaType: String?,
        onDone: (Boolean) -> Unit
    ) {
        var mediaBase64 = ""
        val storedMediaUri = if (mediaType == "video") (mediaUri ?: "") else ""
        if (!mediaUri.isNullOrBlank() && mediaType != "video") {
            try {
                val uri = Uri.parse(mediaUri)
                mediaBase64 = compressImageToBase64(context, uri)
            } catch (e: Exception) {
                Log.e("ListingStore", "Failed to encode media", e)
            }
        }

        val data = mapOf(
            "title" to title,
            "description" to description,
            "price" to if (price.startsWith("$")) price else "\$$price",
            "category" to category,
            "location" to location,
            "distance" to "Nearby",
            "ownerId" to currentUserId(),
            "createdAt" to System.currentTimeMillis(),
            "mediaUri" to storedMediaUri,
            "mediaType" to (mediaType ?: ""),
            "isSold" to false,
            "mediaBase64" to mediaBase64
        )
        Firebase.firestore.collection("listings")
            .add(data)
            .addOnSuccessListener { doc ->
                Log.d("ListingStore", "Saved to cloud with media: ${doc.id}")
                onDone(true)
            }
            .addOnFailureListener { e ->
                Log.e("ListingStore", "Cloud save failed", e)
                onDone(false)
            }
    }

    fun update(
        context: Context,
        listingId: String,
        title: String,
        description: String,
        price: String,
        category: String,
        location: String,
        onDone: (Boolean) -> Unit
    ) {
        val updates = mapOf(
            "title" to title,
            "description" to description,
            "price" to if (price.startsWith("$")) price else "\$$price",
            "category" to category,
            "location" to location
        )
        Firebase.firestore.collection("listings").document(listingId)
            .update(updates)
            .addOnSuccessListener {
                onDone(true)
            }
            .addOnFailureListener { e ->
                Log.e("ListingStore", "Update failed", e)
                onDone(false)
            }
    }

    fun markAsSold(
        context: Context,
        listingId: String,
        isSold: Boolean,
        onDone: (Boolean) -> Unit
    ) {
        Firebase.firestore.collection("listings").document(listingId)
            .update("isSold", isSold)
            .addOnSuccessListener {
                onDone(true)
            }
            .addOnFailureListener { e ->
                Log.e("ListingStore", "markAsSold failed", e)
                onDone(false)
            }
    }

    fun delete(
        context: Context,
        listingId: String,
        onDone: (Boolean) -> Unit
    ) {
        Firebase.firestore.collection("listings").document(listingId)
            .delete()
            .addOnSuccessListener {
                onDone(true)
            }
            .addOnFailureListener { e ->
                Log.e("ListingStore", "Delete failed", e)
                onDone(false)
            }
    }

    private fun processListings(
        context: Context,
        documents: List<DocumentSnapshot>,
        onResult: (List<Listing>) -> Unit
    ) {
        val list = documents.map { doc ->
            val docId = doc.id
            val base64Str = doc.getString("mediaBase64")
            val storedUri = doc.getString("mediaUri") ?: ""
            val type = doc.getString("mediaType") ?: "photo"
            var resolvedMediaUri = ""

            if (!base64Str.isNullOrBlank()) {
                try {
                    val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
                    val ext = if (type == "video") "mp4" else "jpg"
                    val cacheFile = File(context.cacheDir, "listing_$docId.$ext")
                    if (!cacheFile.exists() || cacheFile.length() == 0L) {
                        cacheFile.writeBytes(decodedBytes)
                    }
                    resolvedMediaUri = cacheFile.absolutePath
                } catch (e: Exception) {
                    Log.e("ListingStore", "Failed to decode listing media Base64", e)
                }
            } else if (storedUri.isNotBlank()) {
                resolvedMediaUri = storedUri
            }

            Listing(
                id = docId,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                price = doc.getString("price") ?: "",
                category = doc.getString("category") ?: "",
                distance = doc.getString("distance") ?: "",
                ownerId = doc.getString("ownerId") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L,
                mediaUri = resolvedMediaUri.ifBlank { null },
                mediaType = type.ifBlank { null },
                isSold = doc.getBoolean("isSold") ?: false,
                location = doc.getString("location") ?: "",
                mediaBase64 = base64Str
            )
        }.sortedByDescending { it.createdAt }

        onResult(list)
    }

    fun loadAll(context: Context, onResult: (List<Listing>) -> Unit) {
        Firebase.firestore.collection("listings")
            .get()
            .addOnSuccessListener { result ->
                processListings(context, result.documents, onResult)
            }
            .addOnFailureListener { e ->
                Log.e("ListingStore", "Load all failed", e)
                onResult(emptyList())
            }
    }

    fun loadMine(context: Context, onResult: (List<Listing>) -> Unit) {
        loadUserListings(context, currentUserId(), onResult)
    }

    fun loadUserListings(context: Context, userId: String, onResult: (List<Listing>) -> Unit) {
        Firebase.firestore.collection("listings")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { result ->
                processListings(context, result.documents, onResult)
            }
            .addOnFailureListener { e ->
                Log.e("ListingStore", "Load user listings failed", e)
                onResult(emptyList())
            }
    }
}
