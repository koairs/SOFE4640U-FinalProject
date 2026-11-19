package com.example.sofe4640ufinalproject

data class Message(
    val id: Long = 0L,
    val senderId: String,
    val receiverId: String,
    val itemTitle: String = "",
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Conversation(
    val otherUser: String,
    val lastMessage: String,
    val timestamp: Long,
    val itemTitle: String = ""
)