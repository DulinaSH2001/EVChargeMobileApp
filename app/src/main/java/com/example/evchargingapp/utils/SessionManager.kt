package com.example.evchargingapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.evchargingapp.data.local.User

class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()
    
    companion object {
        private const val PREF_NAME = "EVChargingAppPrefs"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_NIC = "nic"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_OPERATOR = "operator"
    }
    
    fun saveLogin(user: User) {
        editor.apply {
            putBoolean(KEY_LOGGED_IN, true)
            putString(KEY_NIC, user.nic)
            putString(KEY_NAME, user.name)
            putString(KEY_EMAIL, user.email)
            putBoolean(KEY_OPERATOR, false)
            apply()
        }
    }
    
    fun setOperatorLogin(operatorName: String?) {
        editor.apply {
            putBoolean(KEY_LOGGED_IN, true)
            putString(KEY_NIC, "OPERATOR")
            putString(KEY_NAME, operatorName ?: "Operator")
            putString(KEY_EMAIL, "operator@example.com")
            putBoolean(KEY_OPERATOR, true)
            apply()
        }
    }
    
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)
    
    fun isOperator(): Boolean = prefs.getBoolean(KEY_OPERATOR, false)
    
    fun getNic(): String? = prefs.getString(KEY_NIC, null)
    
    fun getUserName(): String = prefs.getString(KEY_NAME, "User") ?: "User"
    
    fun getUserEmail(): String = prefs.getString(KEY_EMAIL, "user@example.com") ?: "user@example.com"
    
    fun logout() {
        editor.clear()
        editor.apply()
    }
}