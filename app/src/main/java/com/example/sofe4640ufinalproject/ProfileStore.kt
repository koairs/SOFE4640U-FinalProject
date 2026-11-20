package com.example.sofe4640ufinalproject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.io.ByteArrayOutputStream
import java.io.File

object ProfileStore {

    fun getPhoto(context: Context, userId: String, onResult: (String?) -> Unit) {
        Firebase.firestore.collection("profiles").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val base64Str = doc.getString("photoBase64")
                if (!base64Str.isNullOrBlank()) {
                    try {
                        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
                        val cacheFile = File(context.cacheDir, "avatar_${userId.hashCode()}.jpg")
                        cacheFile.writeBytes(decodedBytes)
                        onResult(cacheFile.absolutePath)
                    } catch (e: Exception) {
                        Log.e("ProfileStore", "Failed to decode avatar Base64", e)
                        onResult(doc.getString("photoPath"))
                    }
                } else {
                    onResult(doc.getString("photoPath"))
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun setPhoto(context: Context, userId: String, imageUri: Uri, onDone: (Boolean) -> Unit) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                onDone(false)
                return
            }

            val scaledBitmap = scaleBitmap(bitmap, 300)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            val base64Str = Base64.encodeToString(bytes, Base64.DEFAULT)

            val cacheFile = File(context.cacheDir, "avatar_${userId.hashCode()}.jpg")
            cacheFile.writeBytes(bytes)

            val data = mapOf(
                "userId" to userId,
                "photoBase64" to base64Str,
                "photoPath" to cacheFile.absolutePath
            )

            Firebase.firestore.collection("profiles").document(userId)
                .set(data)
                .addOnSuccessListener {
                    Log.d("ProfileStore", "Photo saved to cloud")
                    onDone(true)
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileStore", "Photo save failed", e)
                    onDone(false)
                }
        } catch (e: Exception) {
            Log.e("ProfileStore", "Error processing avatar", e)
            onDone(false)
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
