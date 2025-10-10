package com.example.evchargingapp.data.api

import com.google.gson.annotations.SerializedName

// Generic API Response wrapper
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("data")
    val data: T? = null
)

// Login requests
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class NicLoginRequest(
    @SerializedName("nic")
    val nic: String,
    @SerializedName("password")
    val password: String
)

// Login response data
data class LoginResponseData(
    @SerializedName("token")
    val token: String,
    @SerializedName("user")
    val user: ApiUser
)

// User data from backend
data class ApiUser(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("firstName")
    val firstName: String? = null,
    @SerializedName("lastName")
    val lastName: String? = null,
    @SerializedName("role")
    val role: String
)

// Registration requests
data class RegisterRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("phone")
    val phone: String? = null
)

data class EvOwnerRegisterRequest(
    @SerializedName("nic")
    val nic: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("address")
    val address: String,
    @SerializedName("dateOfBirth")
    val dateOfBirth: String
)

// EV Owner registration response
data class EvOwnerRegisterResponseData(
    @SerializedName("nic")
    val nic: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("address")
    val address: String,
    @SerializedName("dateOfBirth")
    val dateOfBirth: String,
    @SerializedName("isActive")
    val isActive: Boolean,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String
)

// NIC validation response for EV Owner login
data class NicValidationResponseData(
    @SerializedName("email")
    val email: String
)

// Token verification response
data class VerifyTokenResponseData(
    @SerializedName("user")
    val user: ApiUser
)

// User roles enum
enum class UserRole(val value: String) {
    EV_OWNER("EVOwner"),
    STATION_OPERATOR("StationOperator"),
    ADMIN("Backoffice");
    
    companion object {
        fun fromString(value: String): UserRole {
            return values().find { it.value == value } ?: EV_OWNER
        }
    }
}

// Operator specific models
data class OperatorLoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class OperatorUser(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("stationId")
    val stationId: String? = null,
    @SerializedName("role")
    val role: String
)

data class OperatorLoginResponseData(
    @SerializedName("token")
    val token: String,
    @SerializedName("user")
    val user: OperatorUser
)

// Booking related models for operator operations
data class BookingDetails(
    @SerializedName("id")
    val id: String,
    @SerializedName("evOwnerId")
    val evOwnerId: String,
    @SerializedName("evOwnerName")
    val evOwnerName: String,
    @SerializedName("stationId")
    val stationId: String,
    @SerializedName("chargingSlotId")
    val chargingSlotId: String,
    @SerializedName("bookingDate")
    val bookingDate: String,
    @SerializedName("startTime")
    val startTime: String,
    @SerializedName("endTime")
    val endTime: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("qrCode")
    val qrCode: String? = null,
    @SerializedName("vehicleNumber")
    val vehicleNumber: String? = null,
    @SerializedName("contactNumber")
    val contactNumber: String? = null
) : java.io.Serializable

data class OperatorConfirmRequest(
    @SerializedName("bookingId")
    val bookingId: String,
    @SerializedName("operatorId")
    val operatorId: String,
    @SerializedName("startTime")
    val startTime: String? = null
)

data class OperatorFinalizeRequest(
    @SerializedName("bookingId")
    val bookingId: String,
    @SerializedName("operatorId")
    val operatorId: String,
    @SerializedName("endTime")
    val endTime: String,
    @SerializedName("energyConsumed")
    val energyConsumed: Double? = null,
    @SerializedName("totalCost")
    val totalCost: Double? = null
)

data class OperatorSessionResponse(
    @SerializedName("sessionId")
    val sessionId: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String
)

// Operator profile update request
data class OperatorProfileUpdateRequest(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("stationId")
    val stationId: String? = null
)

// Result classes for operator operations
sealed class OperatorResult {
    data class BookingFound(val booking: BookingDetails) : OperatorResult()
    data class BookingFoundWithDto(val booking: BookingDetails, val originalDto: BookingDto) : OperatorResult()
    data class SessionResult(val response: OperatorSessionResponse) : OperatorResult()
    data class ProfileResult(val profile: OperatorUser) : OperatorResult()
    data class StatusUpdateSuccess(val booking: BookingDto) : OperatorResult()
    data class Success(val message: String) : OperatorResult()
    data class Failure(val error: String) : OperatorResult()
}

// Charging Station Models
data class ChargingStationDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String,
    @SerializedName("location")
    val location: String,
    @SerializedName("type")
    val type: String, // "AC" or "DC"
    @SerializedName("totalSlots")
    val totalSlots: Int,
    @SerializedName("availableSlots")
    val availableSlots: Int,
    @SerializedName("schedule")
    val schedule: List<SlotScheduleDto>? = null,
    @SerializedName("isActive")
    val isActive: Boolean,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null,
    @SerializedName("distance")
    val distance: Double? = null // calculated distance from user location
) {
    // Parse coordinates from location string if latitude/longitude are null
    fun getCoordinateLatitude(): Double? {
        return latitude ?: parseCoordinateFromLocation("latitude")
    }
    
    fun getCoordinateLongitude(): Double? {
        return longitude ?: parseCoordinateFromLocation("longitude")
    }
    
    fun getParsedAddress(): String {
        // Extract address from location string
        val addressMatch = Regex("address:([^,]+)").find(location)
        return addressMatch?.groupValues?.get(1)?.trim() ?: location
    }
    
    private fun parseCoordinateFromLocation(coordinate: String): Double? {
        return try {
            val pattern = "$coordinate:([0-9.-]+)"
            val match = Regex(pattern).find(location)
            match?.groupValues?.get(1)?.toDouble()
        } catch (e: Exception) {
            null
        }
    }
}

data class SlotScheduleDto(
    @SerializedName("slotNumber")
    val slotNumber: Int,
    @SerializedName("isAvailable")
    val isAvailable: Boolean,
    @SerializedName("startTime")
    val startTime: String,
    @SerializedName("endTime")
    val endTime: String
)

// Nearby stations request
data class NearbyStationsRequest(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("radius")
    val radius: Double = 10.0 // Default 10km radius
)

// Dashboard stats response
data class DashboardStats(
    @SerializedName("pendingReservations")
    val pendingReservations: Int,
    @SerializedName("approvedReservations")
    val approvedReservations: Int,
    @SerializedName("pastReservations")
    val pastReservations: Int
)

// Booking related models
data class CreateBookingRequest(
    @SerializedName("chargingStationId")
    val chargingStationId: String,
    @SerializedName("slotNumber")
    val slotNumber: Int,
    @SerializedName("reservationDateTime")
    val reservationDateTime: String, // ISO 8601 format: "2025-09-28T10:00:00Z"
    @SerializedName("duration")
    val duration: Int, // Duration in minutes
    @SerializedName("notes")
    val notes: String? = null
)

data class UpdateBookingRequest(
    @SerializedName("slotNumber")
    val slotNumber: Int? = null,
    @SerializedName("reservationDateTime")
    val reservationDateTime: String? = null,
    @SerializedName("duration")
    val duration: Int? = null,
    @SerializedName("notes")
    val notes: String? = null
)

data class BookingDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("chargingStationId")
    val chargingStationId: String,
    @SerializedName("chargingStationName")
    val chargingStationName: String? = null,
    @SerializedName("chargingStationLocation")
    val chargingStationLocation: String? = null,
    @SerializedName("slotNumber")
    val slotNumber: Int,
    @SerializedName("reservationDateTime")
    val reservationDateTime: String,
    @SerializedName("duration")
    val duration: Int,
    @SerializedName("status")
    val status: String, // PENDING, APPROVED, CANCELLED, COMPLETED
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("qrCode")
    val qrCode: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    @SerializedName("isActive")
    val isActive: Boolean? = null,
    @SerializedName("userName")
    val userName: String? = null
) : java.io.Serializable {
    
    fun getParsedAddress(): String {
        return chargingStationLocation?.let { location ->
            try {
                // Extract address from location string
                val addressMatch = Regex("address:([^,]+)").find(location)
                addressMatch?.groupValues?.get(1)?.trim() ?: location
            } catch (e: Exception) {
                location
            }
        } ?: "Location not available"
    }
}

data class BookingSearchRequest(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("fromDate")
    val fromDate: String? = null,
    @SerializedName("toDate")
    val toDate: String? = null,
    @SerializedName("chargingStationId")
    val chargingStationId: String? = null
)

// EV Owner Profile Models
data class EVOwnerDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("nic")
    val nic: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("dateOfBirth")
    val dateOfBirth: String? = null,
    @SerializedName("isActive")
    val isActive: Boolean = true,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class UpdateEVOwnerRequest(
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("dateOfBirth")
    val dateOfBirth: String? = null
)

// Operator booking status update models
data class BookingStatusUpdateRequest(
    @SerializedName("status")
    val status: String // "InProgress" or "Completed"
)