package com.example.evchargingapp.data.local

/**
 * UI-level user type selection for login/registration
 * This is different from UserRole which is the backend role system
 */
enum class UserType {
    EV_OWNER,
    STATION_OPERATOR,
    ADMIN
}