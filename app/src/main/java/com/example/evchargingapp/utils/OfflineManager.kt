package com.example.evchargingapp.utils

import android.content.Context
import android.util.Log
import com.example.evchargingapp.data.local.UserDbHelper
import com.example.evchargingapp.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OfflineManager(private val context: Context) {
    
    private val dbHelper = UserDbHelper(context)
    private val networkUtils = NetworkUtils(context)
    private val authRepository = AuthRepository(context)
    
    fun checkAndSyncData() {
        if (networkUtils.isNetworkAvailable()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Sync pending users
                    authRepository.syncPendingUsers()
                    Log.d("OfflineManager", "Data sync completed")
                } catch (e: Exception) {
                    Log.e("OfflineManager", "Data sync failed", e)
                }
            }
        }
    }
    
    fun hasUnsyncedData(): Boolean {
        val unsyncedUsers = dbHelper.getUsersNeedingSync()
        return unsyncedUsers.isNotEmpty()
    }
    
    fun getUnsyncedDataCount(): Int {
        return dbHelper.getUsersNeedingSync().size
    }
    
    fun showOfflineNotification(callback: (message: String) -> Unit) {
        if (!networkUtils.isNetworkAvailable()) {
            callback("You're offline. Some features may be limited.")
        } else if (hasUnsyncedData()) {
            val count = getUnsyncedDataCount()
            callback("$count items need to be synced. Syncing in background...")
            checkAndSyncData()
        }
    }
}