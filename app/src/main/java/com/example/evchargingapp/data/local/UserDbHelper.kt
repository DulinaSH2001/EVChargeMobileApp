package com.example.evchargingapp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    
    companion object {
        private const val DB_NAME = "ev_users.db"
        private const val DB_VERSION = 1
        
        private const val TABLE_USERS = "users"
        private const val COL_NIC = "nic"
        private const val COL_NAME = "name"
        private const val COL_EMAIL = "email"
        private const val COL_PHONE = "phone"
        private const val COL_PASSWORD = "password"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        val sql = """
            CREATE TABLE $TABLE_USERS (
                $COL_NIC TEXT PRIMARY KEY,
                $COL_NAME TEXT,
                $COL_EMAIL TEXT,
                $COL_PHONE TEXT,
                $COL_PASSWORD TEXT
            )
        """.trimIndent()
        db.execSQL(sql)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }
    
    fun insertUser(user: User): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_NIC, user.nic)
            put(COL_NAME, user.name)
            put(COL_EMAIL, user.email)
            put(COL_PHONE, user.phone)
            put(COL_PASSWORD, user.password)
        }
        val id = db.insertWithOnConflict(TABLE_USERS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        return id != -1L
    }
    
    fun getUserByNic(nic: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS, 
            null, 
            "$COL_NIC=?", 
            arrayOf(nic), 
            null, 
            null, 
            null
        )
        
        return cursor?.use { c ->
            if (c.moveToFirst()) {
                User(
                    nic = c.getString(c.getColumnIndexOrThrow(COL_NIC)),
                    name = c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                    email = c.getString(c.getColumnIndexOrThrow(COL_EMAIL)),
                    phone = c.getString(c.getColumnIndexOrThrow(COL_PHONE)),
                    password = c.getString(c.getColumnIndexOrThrow(COL_PASSWORD))
                )
            } else {
                null
            }
        }
    }
}