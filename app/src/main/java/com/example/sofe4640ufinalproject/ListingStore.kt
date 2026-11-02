package com.example.sofe4640ufinalproject

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

object ListingStore {

    // No login system yet, so every listing belongs to "guest"
    private const val CURRENT_USER = "guest"

    fun add(
        context: Context,
        title: String,
        price: String,
        category: String,
        onDone: (Boolean) -> Unit
    ) {
        try {
            val values = ContentValues().apply {
                put("title", title)
                put("price", "\$$price")
                put("category", category)
                put("distance", "Nearby")
                put("owner_id", CURRENT_USER)
                put("created_at", System.currentTimeMillis())
            }
            val db = DatabaseHelper.getInstance(context).writableDatabase
            val rowId = db.insert(DatabaseHelper.TABLE_LISTINGS, null, values)
            onDone(rowId != -1L)
        } catch (e: Exception) {
            onDone(false)
        }
    }

    fun loadAll(context: Context, onResult: (List<Listing>) -> Unit) {
        try {
            val db = DatabaseHelper.getInstance(context).readableDatabase
            val cursor = db.query(
                DatabaseHelper.TABLE_LISTINGS,
                null, null, null, null, null,
                "created_at DESC"
            )
            onResult(readListings(cursor))
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }

    fun loadMine(context: Context, onResult: (List<Listing>) -> Unit) {
        try {
            val db = DatabaseHelper.getInstance(context).readableDatabase
            val cursor = db.query(
                DatabaseHelper.TABLE_LISTINGS,
                null,
                "owner_id = ?",
                arrayOf(CURRENT_USER),
                null, null,
                "created_at DESC"
            )
            onResult(readListings(cursor))
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }

    private fun readListings(cursor: Cursor): List<Listing> {
        val list = mutableListOf<Listing>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Listing(
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        price = it.getString(it.getColumnIndexOrThrow("price")),
                        category = it.getString(it.getColumnIndexOrThrow("category")),
                        distance = it.getString(it.getColumnIndexOrThrow("distance")),
                        ownerId = it.getString(it.getColumnIndexOrThrow("owner_id")),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }
        return list
    }
}