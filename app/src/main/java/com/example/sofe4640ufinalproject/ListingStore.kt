package com.example.sofe4640ufinalproject

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object ListingStore {
    private val db = FirebaseFirestore.getInstance()

    private fun uid(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: "guest"

    fun add(title: String, price: String, category: String, onDone: (Boolean) -> Unit) {
        val listing = Listing(
            title = title,
            price = "\$$price",
            category = category,
            distance = "Nearby",
            ownerId = uid(),
            createdAt = System.currentTimeMillis()
        )
        db.collection("listings").add(listing)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    fun loadAll(onResult: (List<Listing>) -> Unit) {
        db.collection("listings").get()
            .addOnSuccessListener { snap ->
                onResult(snap.toObjects(Listing::class.java).sortedByDescending { it.createdAt })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun loadMine(onResult: (List<Listing>) -> Unit) {
        db.collection("listings").whereEqualTo("ownerId", uid()).get()
            .addOnSuccessListener { snap ->
                onResult(snap.toObjects(Listing::class.java).sortedByDescending { it.createdAt })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
}