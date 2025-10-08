package com.example.evchargingapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.evchargingapp.data.local.User
import com.example.evchargingapp.data.local.UserType
import com.example.evchargingapp.data.api.UserRole

class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()
    
    companion object {
        private const val PREF_NAME = "EVChargingAppPrefs"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_EMAIL = "email" // Changed from KEY_NIC to KEY_EMAIL
        private const val KEY_ID = "user_id" // Backend user ID
        private const val KEY_NAME = "name"
        private const val KEY_PHONE = "phone"
        private const val KEY_ROLE = "role" // Changed from KEY_USER_TYPE to KEY_ROLE
        private const val KEY_IS_ACTIVE = "is_active"
        private const val KEY_LOGIN_TIME = "login_time"
        private const val KEY_SYNCED_WITH_SERVER = "synced_with_server"
        private const val KEY_TOKEN = "auth_token" // Store JWT token
        // Deprecated - keeping for backward compatibility
        private const val KEY_NIC = "nic"
        private const val KEY_OPERATOR = "operator"
        private const val KEY_USER_TYPE = "user_type"
    }
    
    fun saveLogin(user: User, token: String? = null) {
        editor.apply {
            putBoolean(KEY_LOGGED_IN, true)
            putString(KEY_EMAIL, user.email)
            putString(KEY_ID, user.id)
            putString(KEY_NAME, user.name)
            putString(KEY_PHONE, user.phone)
            putString(KEY_ROLE, user.role.value)
            putBoolean(KEY_IS_ACTIVE, user.isActive)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            putBoolean(KEY_SYNCED_WITH_SERVER, user.syncedWithServer)
            token?.let { putString(KEY_TOKEN, it) }
            // Handle backward compatibility
            putBoolean(KEY_OPERATOR, user.isStationOperator())
            putString(KEY_NIC, user.email) // For backward compatibility
            apply()
        }
    }
    
    fun setOperatorLogin(operatorName: String?) {
        editor.apply {
            putBoolean(KEY_LOGGED_IN, true)
            putString(KEY_NIC, "OPERATOR")
            putString(KEY_NAME, operatorName ?: "Operator")
            putString(KEY_EMAIL, "operator@example.com")
            putString(KEY_PHONE, "")
            putString(KEY_USER_TYPE, UserType.STATION_OPERATOR.name)
            putBoolean(KEY_IS_ACTIVE, true)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            putBoolean(KEY_SYNCED_WITH_SERVER, true)
            // Handle backward compatibility
            putBoolean(KEY_OPERATOR, true)
            apply()
        }
    }
    
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)
    
    fun isOperator(): Boolean {
        // Check new field first, fall back to old field for compatibility
        val userType = prefs.getString(KEY_USER_TYPE, null)
        return if (userType != null) {
            userType == UserType.STATION_OPERATOR.name
        } else {
            prefs.getBoolean(KEY_OPERATOR, false)
        }
    }
    
    fun isEvOwner(): Boolean {
        val userType = prefs.getString(KEY_USER_TYPE, null)
        return if (userType != null) {
            userType == UserType.EV_OWNER.name
        } else {
            !prefs.getBoolean(KEY_OPERATOR, false) // Assume EV owner if not operator
        }
    }
    
    fun isAccountActive(): Boolean = prefs.getBoolean(KEY_IS_ACTIVE, true)
    
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    
    fun getUserId(): String? = prefs.getString(KEY_ID, null)
    
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    
    // Backward compatibility
    @Deprecated("Use getEmail instead")
    fun getNic(): String? = getEmail()
    
    fun getUserName(): String = prefs.getString(KEY_NAME, "User") ?: "User"
    
    fun getUserEmail(): String = prefs.getString(KEY_EMAIL, "user@example.com") ?: "user@example.com"
    
    fun getUserPhone(): String = prefs.getString(KEY_PHONE, "") ?: ""
    
    fun getUserType(): UserType {
        val userTypeString = prefs.getString(KEY_USER_TYPE, null)
        return if (userTypeString != null) {
            try {
                UserType.valueOf(userTypeString)
            } catch (e: Exception) {
                UserType.EV_OWNER
            }
        } else {
            // Backward compatibility
            if (prefs.getBoolean(KEY_OPERATOR, false)) {
                UserType.STATION_OPERATOR
            } else {
                UserType.EV_OWNER
            }
        }
    }
    
    fun getLoginTime(): Long = prefs.getLong(KEY_LOGIN_TIME, 0L)
    
    fun isSyncedWithServer(): Boolean = prefs.getBoolean(KEY_SYNCED_WITH_SERVER, false)
    
    fun getCurrentUser(): User? {
        if (!isLoggedIn()) return null
        
        val nic = getNic() ?: return null
        
        // Convert UserType to UserRole
        val userRole = when (getUserType()) {
            UserType.EV_OWNER -> UserRole.EV_OWNER
            UserType.STATION_OPERATOR -> UserRole.STATION_OPERATOR
            UserType.ADMIN -> UserRole.ADMIN
        }
        
        return User(
            email = nic, // Using email field to store identifier
            id = "", // No backend ID in session
            name = getUserName(),
            phone = getUserPhone(),
            password = "", // Don't store password in session
            role = userRole,
            isActive = isAccountActive(),
            lastSyncTime = getLoginTime(),
            syncedWithServer = isSyncedWithServer()
        )
    }
    
    fun canPerformAction(): Boolean {
        return isLoggedIn() && isAccountActive()
    }
    
    fun requiresReauth(): Boolean {
        if (!isLoggedIn()) return true
        if (!isAccountActive()) return true
        
        val loginTime = getLoginTime()
        val currentTime = System.currentTimeMillis()
        val sessionDuration = currentTime - loginTime
        val maxSessionDuration = 24 * 60 * 60 * 1000L // 24 hours
        
        return sessionDuration > maxSessionDuration
    }
    
    fun updateAccountStatus(isActive: Boolean) {
        editor.putBoolean(KEY_IS_ACTIVE, isActive)
        editor.apply()
    }
    
    fun updateSyncStatus(isSynced: Boolean) {
        editor.putBoolean(KEY_SYNCED_WITH_SERVER, isSynced)
        editor.apply()
    }
    
    fun logout() {
        editor.clear()
        editor.apply()
    }
}