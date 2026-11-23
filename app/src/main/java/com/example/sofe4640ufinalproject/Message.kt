package com.example.sofe4640ufinalproject

data class Message(
    val id: Long = 0L,
    val senderId: String,
    val receiverId: String,
    val itemTitle: String = "",
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val audioUri: String? = null,
    val isAudio: Boolean = false
)

data class Conversation(
    val otherUser: String,
    val lastMessage: String,
    val timestamp: Long,
    val itemTitle: String = ""
)