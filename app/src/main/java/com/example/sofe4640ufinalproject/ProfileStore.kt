package com.example.sofe4640ufinalproject

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object ProfileStore {

    fun getPhoto(context: Context, userId: String, onResult: (String?) -> Unit) {
        Firebase.firestore.collection("profiles").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.getString("photoPath"))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun setPhoto(context: Context, userId: String, path: String, onDone: ((Boolean) -> Unit)? = null) {
        val data = mapOf(
            "userId" to userId,
            "photoPath" to path
        )
        Firebase.firestore.collection("profiles").document(userId)
            .set(data)
            .addOnSuccessListener {
                Log.d("ProfileStore", "Photo saved to cloud")
                onDone?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileStore", "Photo save failed", e)
                onDone?.invoke(false)
            }
    }
}
