package com.example.evchargingapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_bookings")
data class ScannedBookingEntity(
    @PrimaryKey
    val bookingId: String,
    val evOwnerName: String,
    val stationName: String,
    val stationLocation: String,
    val chargingSlotId: String,
    val bookingDate: String,
    val startTime: String,
    val endTime: String,
    val status: String, // "InProgress", "Completed"
    val vehicleNumber: String?,
    val contactNumber: String?,
    val scannedAt: Long, // Timestamp when the QR was scanned
    val updatedAt: Long  // Timestamp when last updated
)