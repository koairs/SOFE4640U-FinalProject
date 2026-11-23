package com.example.sofe4640ufinalproject

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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
        listingOwnerId: String = "",
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
            "isAudio" to isAudio,
            "isRead" to false,
            "listingOwnerId" to listingOwnerId
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

    fun markMessagesAsRead(
        context: Context,
        otherUser: String,
        onDone: ((Boolean) -> Unit)? = null
    ) {
        val current = currentUserId()
        Firebase.firestore.collection("messages")
            .whereEqualTo("senderId", otherUser)
            .whereEqualTo("receiverId", current)
            .get()
            .addOnSuccessListener { result ->
                val unreadDocs = result.documents.filter { doc ->
                    doc.getBoolean("isRead") != true
                }
                if (unreadDocs.isEmpty()) {
                    onDone?.invoke(true)
                    return@addOnSuccessListener
                }
                val batch = Firebase.firestore.batch()
                for (doc in unreadDocs) {
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit()
                    .addOnSuccessListener { onDone?.invoke(true) }
                    .addOnFailureListener { e ->
                        Log.e("MessageStore", "Batch mark as read failed", e)
                        onDone?.invoke(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MessageStore", "markMessagesAsRead failed", e)
                onDone?.invoke(false)
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
                            isAudio = doc.getBoolean("isAudio") ?: false,
                            isRead = doc.getBoolean("isRead") ?: false,
                            listingOwnerId = doc.getString("listingOwnerId") ?: ""
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
        Firebase.firestore.collection("listings")
            .whereEqualTo("ownerId", current)
            .get()
            .addOnSuccessListener { myListingDocs ->
                val myListingTitles = myListingDocs.documents.mapNotNull { it.getString("title") }.toSet()

                Firebase.firestore.collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { result ->
                        val partnerMessages = mutableMapOf<String, MutableList<DocumentSnapshot>>()
                        for (doc in result.documents) {
                            val sender = doc.getString("senderId") ?: ""
                            val receiver = doc.getString("receiverId") ?: ""
                            if (sender == current || receiver == current) {
                                val partner = if (sender == current) receiver else sender
                                partnerMessages.getOrPut(partner) { mutableListOf() }.add(doc)
                            }
                        }

                        val conversations = partnerMessages.map { (partner, docs) ->
                            val latestDoc = docs.first()
                            val lastMessageText = latestDoc.getString("messageText") ?: ""
                            val latestTimestamp = latestDoc.getLong("timestamp") ?: 0L
                            val itemTitle = latestDoc.getString("itemTitle") ?: ""

                            val unreadCount = docs.count { doc ->
                                val s = doc.getString("senderId") ?: ""
                                val r = doc.getString("receiverId") ?: ""
                                val read = doc.getBoolean("isRead") ?: false
                                s == partner && r == current && !read
                            }

                            val earliestDoc = docs.last()
                            val earliestSender = earliestDoc.getString("senderId") ?: ""
                            val listingOwnerId = latestDoc.getString("listingOwnerId")?.ifBlank { null }
                                ?: earliestDoc.getString("listingOwnerId")?.ifBlank { null }
                                ?: ""

                            val isSelling = when {
                                listingOwnerId == current -> true
                                listingOwnerId.isNotBlank() && listingOwnerId != current -> false
                                itemTitle.isNotBlank() && myListingTitles.contains(itemTitle) -> true
                                earliestSender == partner -> true
                                else -> false
                            }

                            Conversation(
                                otherUser = partner,
                                lastMessage = lastMessageText,
                                timestamp = latestTimestamp,
                                itemTitle = itemTitle,
                                isUnread = unreadCount > 0,
                                unreadCount = unreadCount,
                                isSelling = isSelling
                            )
                        }.sortedByDescending { it.timestamp }

                        onResult(conversations)
                    }
                    .addOnFailureListener { e ->
                        Log.e("MessageStore", "Get recent conversations failed", e)
                        onResult(emptyList())
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MessageStore", "Load my listings failed", e)
                onResult(emptyList())
            }
    }
}
