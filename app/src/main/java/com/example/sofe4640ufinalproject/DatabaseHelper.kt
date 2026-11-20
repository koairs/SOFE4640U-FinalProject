package com.example.sofe4640ufinalproject

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
        CREATE TABLE $TABLE_LISTINGS (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            price TEXT NOT NULL,
            category TEXT NOT NULL,
            distance TEXT NOT NULL,
            owner_id TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            media_uri TEXT,
            media_type TEXT
        )
            """.trimIndent()
        )
        db.execSQL(
            """
        CREATE TABLE $TABLE_MESSAGES (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sender_id TEXT NOT NULL,
            receiver_id TEXT NOT NULL,
            item_title TEXT,
            message_text TEXT NOT NULL,
            timestamp INTEGER NOT NULL
        )
            """.trimIndent()
        )
        createExtraTables(db)
    }

    private fun createExtraTables(db: SQLiteDatabase) {
        db.execSQL(
            """
        CREATE TABLE IF NOT EXISTS $TABLE_RATINGS (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            rater_id TEXT NOT NULL,
            rated_id TEXT NOT NULL,
            stars INTEGER NOT NULL,
            comment TEXT,
            created_at INTEGER NOT NULL,
            UNIQUE(rater_id, rated_id)
        )
            """.trimIndent()
        )
        db.execSQL(
            """
        CREATE TABLE IF NOT EXISTS $TABLE_PROFILES (
            user_id TEXT PRIMARY KEY,
            photo_path TEXT
        )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_LISTINGS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
            onCreate(db)
        } else {
            createExtraTables(db)
        }
    }

    companion object {
        const val DB_NAME = "marketplace.db"
        const val DB_VERSION = 4
        const val TABLE_LISTINGS = "listings"
        const val TABLE_MESSAGES = "messages"
        const val TABLE_RATINGS = "ratings"
        const val TABLE_PROFILES = "profiles"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
    }
}