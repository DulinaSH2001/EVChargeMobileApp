package com.example.evchargingapp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.evchargingapp.data.api.UserRole

class UserDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    
    companion object {
        private const val DB_NAME = "ev_users.db"
        private const val DB_VERSION = 4 // Updated version for additional fields
        
        private const val TABLE_USERS = "users"
        private const val COL_EMAIL = "email" // Primary key changed from NIC to email
        private const val COL_ID = "id" // Backend user ID
        private const val COL_NAME = "name"
        private const val COL_PHONE = "phone"
        private const val COL_PASSWORD = "password"
        private const val COL_ROLE = "role" // Changed from user_type to role
        private const val COL_IS_ACTIVE = "is_active"
        private const val COL_LAST_SYNC_TIME = "last_sync_time"
        private const val COL_SYNCED_WITH_SERVER = "synced_with_server"
        private const val COL_ADDRESS = "address"
        private const val COL_DATE_OF_BIRTH = "date_of_birth"
        
        // Legacy columns for migration
        private const val COL_NIC = "nic"
        private const val COL_USER_TYPE = "user_type"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        val sql = """
            CREATE TABLE $TABLE_USERS (
                $COL_EMAIL TEXT PRIMARY KEY,
                $COL_ID TEXT,
                $COL_NAME TEXT,
                $COL_PHONE TEXT,
                $COL_PASSWORD TEXT,
                $COL_ROLE TEXT DEFAULT 'EV_OWNER',
                $COL_IS_ACTIVE INTEGER DEFAULT 1,
                $COL_LAST_SYNC_TIME INTEGER DEFAULT 0,
                $COL_SYNCED_WITH_SERVER INTEGER DEFAULT 0,
                $COL_ADDRESS TEXT DEFAULT '',
                $COL_DATE_OF_BIRTH TEXT DEFAULT ''
            )
        """.trimIndent()
        db.execSQL(sql)
        
        // Create index for better lookup performance on role-based queries
        db.execSQL("CREATE INDEX idx_users_role ON $TABLE_USERS($COL_ROLE)")
        db.execSQL("CREATE INDEX idx_users_active ON $TABLE_USERS($COL_IS_ACTIVE)")
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when {
            oldVersion < 4 -> {
                // Migration to version 4 - add address and date_of_birth columns
                try {
                    // Add new columns if they don't exist
                    db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_ADDRESS TEXT DEFAULT ''")
                    db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_DATE_OF_BIRTH TEXT DEFAULT ''")
                } catch (e: Exception) {
                    // If adding columns fails, recreate table
                    db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
                    onCreate(db)
                }
                
                // If we came from version < 3, we also need to do that migration
                if (oldVersion < 3) {
                    // Migration to version 3 - email-based system already done above with new table
                }
            }
            oldVersion < 3 -> {
                // Migration to version 3 - email-based system
                try {
                    // Create new table with email as primary key
                    val newTableSql = """
                        CREATE TABLE ${TABLE_USERS}_new (
                            $COL_EMAIL TEXT PRIMARY KEY,
                            $COL_ID TEXT,
                            $COL_NAME TEXT,
                            $COL_PHONE TEXT,
                            $COL_PASSWORD TEXT,
                            $COL_ROLE TEXT DEFAULT 'EV_OWNER',
                            $COL_IS_ACTIVE INTEGER DEFAULT 1,
                            $COL_LAST_SYNC_TIME INTEGER DEFAULT 0,
                            $COL_SYNCED_WITH_SERVER INTEGER DEFAULT 0,
                            $COL_ADDRESS TEXT DEFAULT '',
                            $COL_DATE_OF_BIRTH TEXT DEFAULT ''
                        )
                    """.trimIndent()
                    db.execSQL(newTableSql)
                    
                    // Migrate data if old table exists
                    val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$TABLE_USERS'", null)
                    if (cursor.moveToFirst()) {
                        // Old table exists, try to migrate data
                        val migrateSql = """
                            INSERT INTO ${TABLE_USERS}_new 
                            SELECT 
                                COALESCE($COL_EMAIL, $COL_NIC) as $COL_EMAIL,
                                '' as $COL_ID,
                                $COL_NAME,
                                COALESCE($COL_PHONE, '') as $COL_PHONE,
                                $COL_PASSWORD,
                                COALESCE($COL_ROLE, $COL_USER_TYPE, 'EV_OWNER') as $COL_ROLE,
                                COALESCE($COL_IS_ACTIVE, 1) as $COL_IS_ACTIVE,
                                COALESCE($COL_LAST_SYNC_TIME, 0) as $COL_LAST_SYNC_TIME,
                                COALESCE($COL_SYNCED_WITH_SERVER, 0) as $COL_SYNCED_WITH_SERVER,
                                '' as $COL_ADDRESS,
                                '' as $COL_DATE_OF_BIRTH
                            FROM $TABLE_USERS
                            WHERE COALESCE($COL_EMAIL, $COL_NIC) IS NOT NULL
                        """.trimIndent()
                        
                        try {
                            db.execSQL(migrateSql)
                        } catch (e: Exception) {
                            // Migration failed, continue with empty table
                        }
                    }
                    cursor.close()
                    
                    // Drop old table and rename new one
                    db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
                    db.execSQL("ALTER TABLE ${TABLE_USERS}_new RENAME TO $TABLE_USERS")
                    
                } catch (e: Exception) {
                    // If migration fails, recreate table
                    db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
                    db.execSQL("DROP TABLE IF EXISTS ${TABLE_USERS}_new")
                    onCreate(db)
                }
            }
        }
    }
    
    fun insertUser(user: User): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_EMAIL, user.email)
            put(COL_ID, user.id)
            put(COL_NAME, user.name)
            put(COL_PHONE, user.phone)
            put(COL_PASSWORD, user.password)
            put(COL_ROLE, user.role.value)
            put(COL_IS_ACTIVE, if (user.isActive) 1 else 0)
            put(COL_LAST_SYNC_TIME, user.lastSyncTime)
            put(COL_SYNCED_WITH_SERVER, if (user.syncedWithServer) 1 else 0)
            put(COL_ADDRESS, user.address)
            put(COL_DATE_OF_BIRTH, user.dateOfBirth)
        }
        val id = db.insertWithOnConflict(TABLE_USERS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        return id != -1L
    }
    
    fun getUserByEmail(email: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS, 
            null, 
            "$COL_EMAIL=?", 
            arrayOf(email), 
            null, 
            null, 
            null
        )
        
        return cursor?.use { c ->
            if (c.moveToFirst()) {
                User(
                    email = c.getString(c.getColumnIndexOrThrow(COL_EMAIL)),
                    id = c.getString(c.getColumnIndexOrThrow(COL_ID)) ?: "",
                    name = c.getString(c.getColumnIndexOrThrow(COL_NAME)) ?: "",
                    phone = c.getString(c.getColumnIndexOrThrow(COL_PHONE)) ?: "",
                    password = c.getString(c.getColumnIndexOrThrow(COL_PASSWORD)) ?: "",
                    role = UserRole.fromString(c.getString(c.getColumnIndexOrThrow(COL_ROLE)) ?: "EV_OWNER"),
                    isActive = c.getInt(c.getColumnIndexOrThrow(COL_IS_ACTIVE)) == 1,
                    lastSyncTime = c.getLong(c.getColumnIndexOrThrow(COL_LAST_SYNC_TIME)),
                    syncedWithServer = c.getInt(c.getColumnIndexOrThrow(COL_SYNCED_WITH_SERVER)) == 1,
                    address = c.getString(c.getColumnIndexOrThrow(COL_ADDRESS)) ?: "",
                    dateOfBirth = c.getString(c.getColumnIndexOrThrow(COL_DATE_OF_BIRTH)) ?: ""
                )
            } else {
                null
            }
        }
    }
    
    // Backward compatibility method
    @Deprecated("Use getUserByEmail instead")
    fun getUserByNic(nic: String): User? {
        return getUserByEmail(nic)
    }
    
    // Hybrid lookup method - can find user by email or NIC
    fun getUserByIdentifier(identifier: String): User? {
        // This is the same as getUserByEmail since we store both emails and NICs in the email field
        return getUserByEmail(identifier)
    }
    
    // Get users by role - useful for filtering
    fun getUsersByRole(role: UserRole): List<User> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COL_ROLE=?",
            arrayOf(role.value),
            null,
            null,
            COL_NAME
        )
        
        val users = mutableListOf<User>()
        cursor?.use { c ->
            while (c.moveToNext()) {
                users.add(User(
                    email = c.getString(c.getColumnIndexOrThrow(COL_EMAIL)),
                    id = c.getString(c.getColumnIndexOrThrow(COL_ID)) ?: "",
                    name = c.getString(c.getColumnIndexOrThrow(COL_NAME)) ?: "",
                    phone = c.getString(c.getColumnIndexOrThrow(COL_PHONE)) ?: "",
                    password = c.getString(c.getColumnIndexOrThrow(COL_PASSWORD)) ?: "",
                    role = UserRole.fromString(c.getString(c.getColumnIndexOrThrow(COL_ROLE)) ?: "EV_OWNER"),
                    isActive = c.getInt(c.getColumnIndexOrThrow(COL_IS_ACTIVE)) == 1,
                    lastSyncTime = c.getLong(c.getColumnIndexOrThrow(COL_LAST_SYNC_TIME)),
                    syncedWithServer = c.getInt(c.getColumnIndexOrThrow(COL_SYNCED_WITH_SERVER)) == 1
                ))
            }
        }
        return users
    }
    
    fun updateUser(user: User): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_ID, user.id)
            put(COL_NAME, user.name)
            put(COL_PHONE, user.phone)
            put(COL_PASSWORD, user.password)
            put(COL_ROLE, user.role.value)
            put(COL_IS_ACTIVE, if (user.isActive) 1 else 0)
            put(COL_LAST_SYNC_TIME, user.lastSyncTime)
            put(COL_SYNCED_WITH_SERVER, if (user.syncedWithServer) 1 else 0)
        }
        val rowsAffected = db.update(TABLE_USERS, cv, "$COL_EMAIL=?", arrayOf(user.email))
        return rowsAffected > 0
    }
    
    fun deactivateUser(email: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_IS_ACTIVE, 0)
            put(COL_LAST_SYNC_TIME, System.currentTimeMillis())
        }
        val rowsAffected = db.update(TABLE_USERS, cv, "$COL_EMAIL=?", arrayOf(email))
        return rowsAffected > 0
    }
    
    fun getUsersNeedingSync(): List<User> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COL_SYNCED_WITH_SERVER=?",
            arrayOf("0"),
            null,
            null,
            null
        )
        
        val users = mutableListOf<User>()
        cursor?.use { c ->
            while (c.moveToNext()) {
                users.add(User(
                    email = c.getString(c.getColumnIndexOrThrow(COL_EMAIL)),
                    id = c.getString(c.getColumnIndexOrThrow(COL_ID)) ?: "",
                    name = c.getString(c.getColumnIndexOrThrow(COL_NAME)) ?: "",
                    phone = c.getString(c.getColumnIndexOrThrow(COL_PHONE)) ?: "",
                    password = c.getString(c.getColumnIndexOrThrow(COL_PASSWORD)) ?: "",
                    role = UserRole.fromString(c.getString(c.getColumnIndexOrThrow(COL_ROLE)) ?: "EV_OWNER"),
                    isActive = c.getInt(c.getColumnIndexOrThrow(COL_IS_ACTIVE)) == 1,
                    lastSyncTime = c.getLong(c.getColumnIndexOrThrow(COL_LAST_SYNC_TIME)),
                    syncedWithServer = c.getInt(c.getColumnIndexOrThrow(COL_SYNCED_WITH_SERVER)) == 1
                ))
            }
        }
        return users
    }
    
    fun markAsSynced(email: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_SYNCED_WITH_SERVER, 1)
            put(COL_LAST_SYNC_TIME, System.currentTimeMillis())
        }
        val rowsAffected = db.update(TABLE_USERS, cv, "$COL_EMAIL=?", arrayOf(email))
        return rowsAffected > 0
    }
}