package com.example.sofe4640ufinalproject

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

object ProfileStore {

    fun getPhoto(context: Context, userId: String): String? {
        val db = DatabaseHelper.getInstance(context).readableDatabase
        db.rawQuery(
            "SELECT photo_path FROM ${DatabaseHelper.TABLE_PROFILES} WHERE user_id = ?",
            arrayOf(userId)
        ).use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return null
    }

    fun setPhoto(context: Context, userId: String, path: String) {
        val values = ContentValues().apply {
            put("user_id", userId)
            put("photo_path", path)
        }
        DatabaseHelper.getInstance(context).writableDatabase.insertWithOnConflict(
            DatabaseHelper.TABLE_PROFILES, null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}