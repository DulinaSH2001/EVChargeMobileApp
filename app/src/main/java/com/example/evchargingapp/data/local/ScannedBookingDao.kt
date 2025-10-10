package com.example.evchargingapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedBookingDao {
    
    @Query("SELECT * FROM scanned_bookings ORDER BY scannedAt DESC")
    fun getAllScannedBookings(): Flow<List<ScannedBookingEntity>>
    
    @Query("SELECT * FROM scanned_bookings WHERE status = :status ORDER BY scannedAt DESC")
    fun getBookingsByStatus(status: String): Flow<List<ScannedBookingEntity>>
    
    @Query("SELECT * FROM scanned_bookings WHERE bookingId = :bookingId")
    suspend fun getBookingById(bookingId: String): ScannedBookingEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: ScannedBookingEntity)
    
    @Update
    suspend fun updateBooking(booking: ScannedBookingEntity)
    
    @Query("UPDATE scanned_bookings SET status = :status, updatedAt = :updatedAt WHERE bookingId = :bookingId")
    suspend fun updateBookingStatus(bookingId: String, status: String, updatedAt: Long)
    
    @Delete
    suspend fun deleteBooking(booking: ScannedBookingEntity)
    
    @Query("DELETE FROM scanned_bookings WHERE bookingId = :bookingId")
    suspend fun deleteBookingById(bookingId: String)
    
    @Query("DELETE FROM scanned_bookings")
    suspend fun deleteAllBookings()
    
    @Query("SELECT COUNT(*) FROM scanned_bookings WHERE status = 'InProgress'")
    suspend fun getInProgressBookingsCount(): Int
    
    @Query("SELECT COUNT(*) FROM scanned_bookings WHERE status = 'Completed'")
    suspend fun getCompletedBookingsCount(): Int
}