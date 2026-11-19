package com.example.sofe4640ufinalproject

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.google.firebase.auth.FirebaseAuth

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
        onDone: (Boolean) -> Unit
    ) {
        val senderId = currentUserId()
        try {
            val values = ContentValues().apply {
                put("sender_id", senderId)
                put("receiver_id", receiverId)
                put("item_title", itemTitle)
                put("message_text", messageText)
                put("timestamp", System.currentTimeMillis())
            }
            val db = DatabaseHelper.getInstance(context).writableDatabase
            val rowId = db.insert(DatabaseHelper.TABLE_MESSAGES, null, values)
            onDone(rowId != -1L)
        } catch (e: Exception) {
            onDone(false)
        }
    }

    fun getMessagesBetween(
        context: Context,
        otherUser: String,
        onResult: (List<Message>) -> Unit
    ) {
        val current = currentUserId()
        try {
            val db = DatabaseHelper.getInstance(context).readableDatabase
            val cursor = db.query(
                DatabaseHelper.TABLE_MESSAGES,
                null,
                "(sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)",
                arrayOf(current, otherUser, otherUser, current),
                null, null,
                "timestamp ASC"
            )
            val list = mutableListOf<Message>()
            cursor.use {
                while (it.moveToNext()) {
                    list.add(
                        Message(
                            id = it.getLong(it.getColumnIndexOrThrow("id")),
                            senderId = it.getString(it.getColumnIndexOrThrow("sender_id")),
                            receiverId = it.getString(it.getColumnIndexOrThrow("receiver_id")),
                            itemTitle = it.getString(it.getColumnIndexOrThrow("item_title")) ?: "",
                            messageText = it.getString(it.getColumnIndexOrThrow("message_text")),
                            timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp"))
                        )
                    )
                }
            }
            onResult(list)
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }

    fun getRecentConversations(
        context: Context,
        onResult: (List<Conversation>) -> Unit
    ) {
        val current = currentUserId()
        try {
            val db = DatabaseHelper.getInstance(context).readableDatabase
            val cursor = db.rawQuery(
                """
                SELECT m1.* FROM ${DatabaseHelper.TABLE_MESSAGES} m1
                INNER JOIN (
                    SELECT 
                        CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS chat_partner,
                        MAX(timestamp) AS max_time
                    FROM ${DatabaseHelper.TABLE_MESSAGES}
                    WHERE sender_id = ? OR receiver_id = ?
                    GROUP BY chat_partner
                ) m2 ON (
                    (m1.sender_id = ? AND m1.receiver_id = m2.chat_partner) OR
                    (m1.receiver_id = ? AND m1.sender_id = m2.chat_partner)
                ) AND m1.timestamp = m2.max_time
                ORDER BY m1.timestamp DESC
                """.trimIndent(),
                arrayOf(current, current, current, current, current)
            )
            val conversations = mutableListOf<Conversation>()
            cursor.use {
                while (it.moveToNext()) {
                    val sender = it.getString(it.getColumnIndexOrThrow("sender_id"))
                    val receiver = it.getString(it.getColumnIndexOrThrow("receiver_id"))
                    val partner = if (sender == current) receiver else sender
                    val text = it.getString(it.getColumnIndexOrThrow("message_text"))
                    val time = it.getLong(it.getColumnIndexOrThrow("timestamp"))
                    val itemTitle = it.getString(it.getColumnIndexOrThrow("item_title")) ?: ""
                    conversations.add(
                        Conversation(
                            otherUser = partner,
                            lastMessage = text,
                            timestamp = time,
                            itemTitle = itemTitle
                        )
                    )
                }
            }
            onResult(conversations)
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }
}