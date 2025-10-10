package com.example.evchargingapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScannedBookingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OperatorDatabase : RoomDatabase() {
    
    abstract fun scannedBookingDao(): ScannedBookingDao
    
    companion object {
        @Volatile
        private var INSTANCE: OperatorDatabase? = null
        
        fun getDatabase(context: Context): OperatorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OperatorDatabase::class.java,
                    "operator_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}