package com.example.sofe4640ufinalproject

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

object RatingStore {

    data class Summary(val average: Double, val count: Int)

    fun summary(context: Context, userId: String): Summary {
        val db = DatabaseHelper.getInstance(context).readableDatabase
        db.rawQuery(
            "SELECT AVG(stars), COUNT(*) FROM ${DatabaseHelper.TABLE_RATINGS} WHERE rated_id = ?",
            arrayOf(userId)
        ).use { c ->
            if (c.moveToFirst()) return Summary(c.getDouble(0), c.getInt(1))
        }
        return Summary(0.0, 0)
    }

    fun label(s: Summary): String =
        if (s.count == 0) "No ratings yet" else String.format("%.1f ★ (%d)", s.average, s.count)

    fun hasInteracted(context: Context, a: String, b: String): Boolean {
        val db = DatabaseHelper.getInstance(context).readableDatabase
        db.rawQuery(
            "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_MESSAGES} " +
                    "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)",
            arrayOf(a, b, b, a)
        ).use { c ->
            if (c.moveToFirst()) return c.getInt(0) > 0
        }
        return false
    }

    fun myRating(context: Context, rater: String, rated: String): Int? {
        val db = DatabaseHelper.getInstance(context).readableDatabase
        db.rawQuery(
            "SELECT stars FROM ${DatabaseHelper.TABLE_RATINGS} WHERE rater_id = ? AND rated_id = ?",
            arrayOf(rater, rated)
        ).use { c ->
            if (c.moveToFirst()) return c.getInt(0)
        }
        return null
    }

    fun submit(context: Context, rater: String, rated: String, stars: Int, comment: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put("rater_id", rater)
                put("rated_id", rated)
                put("stars", stars)
                put("comment", comment)
                put("created_at", System.currentTimeMillis())
            }
            val db = DatabaseHelper.getInstance(context).writableDatabase
            db.insertWithOnConflict(
                DatabaseHelper.TABLE_RATINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE
            ) != -1L
        } catch (e: Exception) {
            false
        }
    }
}