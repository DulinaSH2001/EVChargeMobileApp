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
    EV_OWNER("EV_OWNER"),
    STATION_OPERATOR("STATION_OPERATOR"),
    ADMIN("ADMIN");
    
    companion object {
        fun fromString(value: String): UserRole {
            return values().find { it.value == value } ?: EV_OWNER
        }
    }
}