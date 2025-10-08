package com.example.evchargingapp.data.local

/**
 * UI-level user type selection for login/registration
 * This maps to the backend role system
 */
enum class UserType(val displayName: String, val backendRole: String) {
    EV_OWNER("EV Owner", "EVOwner"),
    STATION_OPERATOR("Station Operator", "StationOperator"),
    ADMIN("Admin", "Backoffice");
    
    companion object {
        fun fromBackendRole(role: String): UserType? {
            return values().find { it.backendRole == role }
        }
    }
}