package com.example.sofe4640ufinalproject

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object RatingStore {

    data class Summary(val average: Double, val count: Int)

    fun summary(context: Context, userId: String, onResult: (Summary) -> Unit) {
        Firebase.firestore.collection("ratings")
            .whereEqualTo("ratedId", userId)
            .get()
            .addOnSuccessListener { result ->
                var total = 0.0
                val count = result.size()
                for (doc in result.documents) {
                    total += doc.getLong("stars") ?: 0L
                }
                val avg = if (count > 0) total / count else 0.0
                onResult(Summary(avg, count))
            }
            .addOnFailureListener {
                onResult(Summary(0.0, 0))
            }
    }

    fun label(s: Summary): String =
        if (s.count == 0) "No ratings yet" else String.format("%.1f ★ (%d)", s.average, s.count)

    fun hasInteracted(context: Context, a: String, b: String, onResult: (Boolean) -> Unit) {
        Firebase.firestore.collection("messages")
            .get()
            .addOnSuccessListener { result ->
                val interacted = result.documents.any { doc ->
                    val sender = doc.getString("senderId") ?: ""
                    val receiver = doc.getString("receiverId") ?: ""
                    (sender == a && receiver == b) || (sender == b && receiver == a)
                }
                onResult(interacted)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun myRating(context: Context, rater: String, rated: String, onResult: (Int?) -> Unit) {
        Firebase.firestore.collection("ratings")
            .whereEqualTo("raterId", rater)
            .whereEqualTo("ratedId", rated)
            .get()
            .addOnSuccessListener { result ->
                val stars = result.documents.firstOrNull()?.getLong("stars")?.toInt()
                onResult(stars)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun submit(context: Context, rater: String, rated: String, stars: Int, comment: String, onDone: (Boolean) -> Unit) {
        val docId = "${rater}_${rated}"
        val data = mapOf(
            "raterId" to rater,
            "ratedId" to rated,
            "stars" to stars,
            "comment" to comment,
            "createdAt" to System.currentTimeMillis()
        )
        Firebase.firestore.collection("ratings").document(docId)
            .set(data)
            .addOnSuccessListener {
                Log.d("RatingStore", "Rating submitted")
                onDone(true)
            }
            .addOnFailureListener { e ->
                Log.e("RatingStore", "Rating submit failed", e)
                onDone(false)
            }
    }
}
