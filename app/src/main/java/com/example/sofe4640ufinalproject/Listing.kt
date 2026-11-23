package com.example.sofe4640ufinalproject

data class Listing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val category: String = "",
    val distance: String = "",
    val ownerId: String = "",
    val createdAt: Long = 0L,
    val mediaUri: String? = null,
    val mediaType: String? = null,
    val isSold: Boolean = false,
    val location: String = "",
    val mediaBase64: String? = null
)
