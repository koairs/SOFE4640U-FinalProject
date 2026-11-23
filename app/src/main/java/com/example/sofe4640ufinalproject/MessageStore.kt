package com.example.sofe4640ufinalproject

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

object MessageStore {

    fun currentUserId(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.email?.ifBlank { null } ?: user?.uid ?: "guest"
    }

    fun sendMessage(
        context: Context,
        receiverId: String,
        itemTitle: String,
        messageText: String,
        audioUri: String? = null,
        isAudio: Boolean = false,
        onDone: (Boolean) -> Unit
    ) {
        val senderId = currentUserId()
        val data = mapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "itemTitle" to itemTitle,
            "messageText" to messageText,
            "timestamp" to System.currentTimeMillis(),
            "audioUri" to (audioUri ?: ""),
            "isAudio" to isAudio
        )
        Firebase.firestore.collection("messages")
            .add(data)
            .addOnSuccessListener { doc ->
                Log.d("MessageStore", "Saved to cloud: ${doc.id}")
                onDone(true)
            }
            .addOnFailureListener { e ->
                Log.e("MessageStore", "Cloud save failed", e)
                onDone(false)
            }
    }

    fun getMessagesBetween(
        context: Context,
        otherUser: String,
        onResult: (List<Message>) -> Unit
    ) {
        val current = currentUserId()
        Firebase.firestore.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    val sender = doc.getString("senderId") ?: ""
                    val receiver = doc.getString("receiverId") ?: ""
                    if ((sender == current && receiver == otherUser) || (sender == otherUser && receiver == current)) {
                        Message(
                            id = doc.id.hashCode().toLong(),
                            senderId = sender,
                            receiverId = receiver,
                            itemTitle = doc.getString("itemTitle") ?: "",
                            messageText = doc.getString("messageText") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            audioUri = doc.getString("audioUri")?.ifBlank { null },
                            isAudio = doc.getBoolean("isAudio") ?: false
                        )
                    } else {
                        null
                    }
                }
                onResult(list)
            }
            .addOnFailureListener { e ->
                Log.e("MessageStore", "Get messages failed", e)
                onResult(emptyList())
            }
    }

    fun getRecentConversations(
        context: Context,
        onResult: (List<Conversation>) -> Unit
    ) {
        val current = currentUserId()
        Firebase.firestore.collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val map = mutableMapOf<String, Triple<String, Long, String>>()
                for (doc in result.documents) {
                    val sender = doc.getString("senderId") ?: ""
                    val receiver = doc.getString("receiverId") ?: ""
                    if (sender == current || receiver == current) {
                        val partner = if (sender == current) receiver else sender
                        if (!map.containsKey(partner)) {
                            val text = doc.getString("messageText") ?: ""
                            val time = doc.getLong("timestamp") ?: 0L
                            val itemTitle = doc.getString("itemTitle") ?: ""
                            map[partner] = Triple(text, time, itemTitle)
                        }
                    }
                }
                val conversations = map.map { (partner, triple) ->
                    Conversation(
                        otherUser = partner,
                        lastMessage = triple.first,
                        timestamp = triple.second,
                        itemTitle = triple.third
                    )
                }.sortedByDescending { it.timestamp }
                onResult(conversations)
            }
            .addOnFailureListener { e ->
                Log.e("MessageStore", "Get recent conversations failed", e)
                onResult(emptyList())
            }
    }
}
