package com.example.evchargingapp.data.local

data class Booking(
    val id: String,
    val stationName: String,
    val location: String,
    val date: String,
    val time: String,
    val duration: String,
    val status: BookingStatus,
    val userId: String
)

enum class BookingStatus {
    PENDING,        // 0
    CONFIRMED,      // 1
    INPROGRESS,     // 2
    COMPLETED,      // 3
    CANCELLED,      // 4
    NOSHOW          // 5
}