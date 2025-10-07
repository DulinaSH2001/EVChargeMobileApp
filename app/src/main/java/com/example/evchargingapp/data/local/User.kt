package com.example.evchargingapp.data.local

import com.example.evchargingapp.data.api.UserRole

data class User(
    var email: String = "", // Primary identifier changed from NIC to email
    var id: String = "", // Backend user ID
    var name: String = "",
    var phone: String = "",
    var password: String = "", // For local storage only
    var role: UserRole = UserRole.EV_OWNER, // Changed from userType to role
    var isActive: Boolean = true,
    var lastSyncTime: Long = 0L, // Timestamp for last server sync
    var syncedWithServer: Boolean = false,
    // Additional fields for EV Owner registration
    var address: String = "",
    var dateOfBirth: String = ""
) {
    constructor() : this("", "", "", "", "", UserRole.EV_OWNER, true, 0L, false, "", "")
    
    // Helper methods
    fun isEvOwner(): Boolean = role == UserRole.EV_OWNER
    fun isStationOperator(): Boolean = role == UserRole.STATION_OPERATOR
    fun isAdmin(): Boolean = role == UserRole.ADMIN
    fun canLogin(): Boolean = isActive && email.isNotEmpty()
    
    // For backward compatibility with NIC-based system
    @Deprecated("Use email instead", ReplaceWith("email"))
    var nic: String
        get() = email
        set(value) { email = value }
}