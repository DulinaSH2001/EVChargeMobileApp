package com.example.evchargingapp.data.repository

import android.content.Context
import com.example.evchargingapp.data.api.BookingDetails
import com.example.evchargingapp.data.local.OperatorDatabase
import com.example.evchargingapp.data.local.ScannedBookingDao
import com.example.evchargingapp.data.local.ScannedBookingEntity
import kotlinx.coroutines.flow.Flow

class ScannedBookingRepository(context: Context) {
    
    private val scannedBookingDao: ScannedBookingDao = OperatorDatabase.getDatabase(context).scannedBookingDao()
    
    fun getAllScannedBookings(): Flow<List<ScannedBookingEntity>> {
        return scannedBookingDao.getAllScannedBookings()
    }
    
    fun getBookingsByStatus(status: String): Flow<List<ScannedBookingEntity>> {
        return scannedBookingDao.getBookingsByStatus(status)
    }
    
    suspend fun getBookingById(bookingId: String): ScannedBookingEntity? {
        return scannedBookingDao.getBookingById(bookingId)
    }
    
    suspend fun saveScannedBooking(bookingDetails: BookingDetails) {
        val currentTime = System.currentTimeMillis()
        val scannedBooking = ScannedBookingEntity(
            bookingId = bookingDetails.id,
            evOwnerName = bookingDetails.evOwnerName,
            stationName = "Station ${bookingDetails.stationId}", // Use stationId as fallback
            stationLocation = "Location not available", // Default value since not available in BookingDetails
            chargingSlotId = bookingDetails.chargingSlotId,
            bookingDate = bookingDetails.bookingDate,
            startTime = bookingDetails.startTime,
            endTime = bookingDetails.endTime,
            status = "InProgress", // Initially set to InProgress when scanned
            vehicleNumber = bookingDetails.vehicleNumber,
            contactNumber = bookingDetails.contactNumber,
            scannedAt = currentTime,
            updatedAt = currentTime
        )
        scannedBookingDao.insertBooking(scannedBooking)
    }
    
    // Alternative method that accepts BookingDto directly for better station info
    suspend fun saveScannedBookingFromDto(bookingDto: com.example.evchargingapp.data.api.BookingDto) {
        val currentTime = System.currentTimeMillis()
        val scannedBooking = ScannedBookingEntity(
            bookingId = bookingDto.id,
            evOwnerName = bookingDto.userName ?: "Unknown Owner",
            stationName = bookingDto.chargingStationName ?: "Station ${bookingDto.chargingStationId}",
            stationLocation = bookingDto.chargingStationLocation ?: "Location not available",
            chargingSlotId = bookingDto.slotNumber.toString(),
            bookingDate = bookingDto.reservationDateTime.split("T")[0],
            startTime = bookingDto.reservationDateTime.split("T")[1].substring(0, 5),
            endTime = calculateEndTime(bookingDto.reservationDateTime, bookingDto.duration),
            status = bookingDto.status, // Use the actual status from the API response
            vehicleNumber = null, // Not available in BookingDto
            contactNumber = null, // Not available in BookingDto
            scannedAt = currentTime,
            updatedAt = currentTime
        )
        
        android.util.Log.d("ScannedBookingRepo", "Saving booking: ID=${scannedBooking.bookingId}, Status=${scannedBooking.status}")
        scannedBookingDao.insertBooking(scannedBooking)
        android.util.Log.d("ScannedBookingRepo", "Booking saved successfully to database")
    }
    
    private fun calculateEndTime(startDateTime: String, duration: Int): String {
        return try {
            val startTime = startDateTime.split("T")[1].substring(0, 5) // Extract HH:mm
            val (hours, minutes) = startTime.split(":").map { it.toInt() }
            val totalMinutes = hours * 60 + minutes + duration
            val endHours = (totalMinutes / 60) % 24
            val endMinutes = totalMinutes % 60
            String.format("%02d:%02d", endHours, endMinutes)
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    suspend fun updateBookingStatus(bookingId: String, status: String) {
        scannedBookingDao.updateBookingStatus(bookingId, status, System.currentTimeMillis())
    }
    
    suspend fun deleteBooking(bookingId: String) {
        scannedBookingDao.deleteBookingById(bookingId)
    }
    
    suspend fun deleteAllBookings() {
        scannedBookingDao.deleteAllBookings()
    }
    
    suspend fun getInProgressBookingsCount(): Int {
        return scannedBookingDao.getInProgressBookingsCount()
    }
    
    suspend fun getCompletedBookingsCount(): Int {
        return scannedBookingDao.getCompletedBookingsCount()
    }
}