package com.example.sofe4640ufinalproject

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import java.io.File

object ListingStore {

    fun currentUserId(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.email?.ifBlank { null } ?: user?.uid ?: "guest"
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
        if (!mediaUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(mediaUri)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    mediaBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                }
            } catch (e: Exception) {
                Log.e("ListingStore", "Failed to encode media to Base64", e)
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
            "mediaUri" to (mediaUri ?: ""),
            "mediaBase64" to mediaBase64,
            "mediaType" to (mediaType ?: ""),
            "isSold" to false
        )
        Firebase.firestore.collection("listings")
            .add(data)
            .addOnSuccessListener { doc ->
                Log.d("ListingStore", "Saved to cloud: ${doc.id}")
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

    private fun parseListing(context: Context, doc: DocumentSnapshot): Listing {
        val base64Str = doc.getString("mediaBase64")
        val type = doc.getString("mediaType") ?: "photo"
        var resolvedMediaUri = doc.getString("mediaUri") ?: ""

        if (!base64Str.isNullOrBlank()) {
            try {
                val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
                val ext = if (type == "video") "mp4" else "jpg"
                val cacheFile = File(context.cacheDir, "listing_${doc.id}.$ext")
                cacheFile.writeBytes(decodedBytes)
                resolvedMediaUri = cacheFile.absolutePath
            } catch (e: Exception) {
                Log.e("ListingStore", "Failed to decode listing media Base64", e)
            }
        }

        return Listing(
            id = doc.id,
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
    }

    fun loadAll(context: Context, onResult: (List<Listing>) -> Unit) {
        Firebase.firestore.collection("listings")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.map { doc ->
                    parseListing(context, doc)
                }.sortedByDescending { it.createdAt }
                onResult(list)
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
                val list = result.documents.map { doc ->
                    parseListing(context, doc)
                }.sortedByDescending { it.createdAt }
                onResult(list)
            }
            .addOnFailureListener { e ->
                Log.e("ListingStore", "Load user listings failed", e)
                onResult(emptyList())
            }
    }
}
