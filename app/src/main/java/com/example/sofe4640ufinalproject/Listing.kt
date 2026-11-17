package com.example.sofe4640ufinalproject

data class Listing(
    val id: Long = 0L,
    val title: String = "",
    val price: String = "",
    val category: String = "",
    val distance: String = "",
    val ownerId: String = "",
    val createdAt: Long = 0L,
    val mediaUri: String? = null,
    val mediaType: String? = null
)