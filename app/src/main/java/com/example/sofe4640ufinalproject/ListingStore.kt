package com.example.sofe4640ufinalproject

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

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
        mediaUri: String?,
        mediaType: String?,
        onDone: (Boolean) -> Unit
    ) {
        val data = mapOf(
            "title" to title,
            "description" to description,
            "price" to if (price.startsWith("$")) price else "\$$price",
            "category" to category,
            "distance" to "Nearby",
            "ownerId" to currentUserId(),
            "createdAt" to System.currentTimeMillis(),
            "mediaUri" to (mediaUri ?: ""),
            "mediaType" to (mediaType ?: "")
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
        onDone: (Boolean) -> Unit
    ) {
        val updates = mapOf(
            "title" to title,
            "description" to description,
            "price" to if (price.startsWith("$")) price else "\$$price",
            "category" to category
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

    fun loadAll(context: Context, onResult: (List<Listing>) -> Unit) {
        Firebase.firestore.collection("listings")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    Listing(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getString("price") ?: "",
                        category = doc.getString("category") ?: "",
                        distance = doc.getString("distance") ?: "",
                        ownerId = doc.getString("ownerId") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        mediaUri = doc.getString("mediaUri")?.ifBlank { null },
                        mediaType = doc.getString("mediaType")?.ifBlank { null }
                    )
                }.sortedByDescending { it.createdAt }
                onResult(list)
            }
            .addOnFailureListener { e ->
                Log.e("ListingStore", "Load all failed", e)
                onResult(emptyList())
            }
    }

    fun loadMine(context: Context, onResult: (List<Listing>) -> Unit) {
        val current = currentUserId()
        Firebase.firestore.collection("listings")
            .whereEqualTo("ownerId", current)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    Listing(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getString("price") ?: "",
                        category = doc.getString("category") ?: "",
                        distance = doc.getString("distance") ?: "",
                        ownerId = doc.getString("ownerId") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        mediaUri = doc.getString("mediaUri")?.ifBlank { null },
                        mediaType = doc.getString("mediaType")?.ifBlank { null }
                    )
                }.sortedByDescending { it.createdAt }
                onResult(list)
            }
            .addOnFailureListener { e ->
                Log.e("ListingStore", "Load mine failed", e)
                onResult(emptyList())
            }
    }
}
