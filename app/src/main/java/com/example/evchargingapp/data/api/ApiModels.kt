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
    data class SessionResult(val response: OperatorSessionResponse) : OperatorResult()
    data class ProfileResult(val profile: OperatorUser) : OperatorResult()
    data class Success(val message: String) : OperatorResult()
    data class Failure(val error: String) : OperatorResult()
}