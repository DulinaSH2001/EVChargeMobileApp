package com.example.evchargingapp.data.repository

import android.content.Context
import android.util.Log
import com.example.evchargingapp.data.api.*
import com.example.evchargingapp.data.local.User
import com.example.evchargingapp.data.local.UserDbHelper
import com.example.evchargingapp.data.local.UserType
import com.example.evchargingapp.utils.ApiConfig
import com.example.evchargingapp.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    
    private val apiService = ApiConfig.retrofit.create(AuthApiService::class.java)
    private val networkUtils = NetworkUtils(context)
    private val dbHelper = UserDbHelper(context)
    
    suspend fun loginHybrid(identifier: String, password: String, userType: UserType): AuthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (networkUtils.isNetworkAvailable()) {
                handleRemoteHybridLogin(identifier, password, userType)
            } else {
                handleLocalHybridLogin(identifier, password)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Hybrid login error", e)
            handleLocalHybridLogin(identifier, password, "Login failed: ${e.message}")
        }
    }
    
    private suspend fun handleRemoteHybridLogin(identifier: String, password: String, userType: UserType): AuthResult {
        val response = when (userType) {
            UserType.STATION_OPERATOR -> apiService.login(LoginRequest(identifier, password))
            UserType.EV_OWNER -> apiService.loginWithNic(NicLoginRequest(identifier, password))
            else -> return AuthResult.Failure("Invalid user type")
        }
        
        if (response.isSuccessful && response.body()?.success == true) {
            val loginData = response.body()!!.data ?: return handleLocalHybridLogin(identifier, password)
            
            val user = User(
                email = if (userType == UserType.EV_OWNER) identifier else loginData.user.email,
                id = loginData.user.id,
                name = "",
                phone = "",
                password = password,
                role = UserRole.fromString(loginData.user.role),
                isActive = true,
                lastSyncTime = System.currentTimeMillis(),
                syncedWithServer = true
            )
            
            saveOrUpdateUser(user)
            return AuthResult.Success(user, loginData.token)
        }
        
        return if (response.code() == 401) {
            AuthResult.Failure("Invalid credentials")
        } else {
            handleLocalHybridLogin(identifier, password)
        }
    }
    
    private suspend fun handleLocalHybridLogin(identifier: String, password: String, errorMessage: String? = null): AuthResult {
        val localUser = dbHelper.getUserByEmail(identifier)
        return if (localUser != null && localUser.password == password) {
            if (!localUser.canLogin()) {
                AuthResult.Failure("Account may be deactivated. Please connect to internet to verify.")
            } else {
                val message = if (errorMessage != null) "Logged in offline (Fallback)" else "Logged in offline"
                AuthResult.Success(localUser, null, message)
            }
        } else {
            val error = if (errorMessage != null) {
                "Login failed. Check your connection and try again. Error: $errorMessage"
            } else {
                "Invalid credentials"
            }
            AuthResult.Failure(error)
        }
    }
    
    suspend fun registerEvOwnerWithDetails(
        nic: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phone: String,
        address: String,
        dateOfBirth: String
    ): AuthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val registerRequest = EvOwnerRegisterRequest(
                nic = nic,
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                phone = phone,
                address = address,
                dateOfBirth = dateOfBirth
            )
            
            val user = User(
                email = nic, // Store NIC in email field for compatibility
                id = generateUserId(),
                name = "$firstName $lastName",
                phone = phone,
                password = password,
                role = UserRole.EV_OWNER,
                isActive = true,
                lastSyncTime = System.currentTimeMillis(),
                syncedWithServer = false,
                address = address,
                dateOfBirth = dateOfBirth
            )
            
            // Try remote registration if network available
            if (networkUtils.isNetworkAvailable()) {
                try {
                    val response = apiService.registerEvOwner(registerRequest)
                    if (response.isSuccessful && response.body()?.success == true) {
                        user.syncedWithServer = true
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Remote registration failed, proceeding with local", e)
                }
            }
            
            val inserted = dbHelper.insertUser(user)
            if (inserted) {
                val message = if (user.syncedWithServer) "Registration successful" else "Registered locally. Will sync when connected."
                AuthResult.Success(user, null, message)
            } else {
                AuthResult.Failure("Registration failed. User may already exist.")
            }
            
        } catch (e: Exception) {
            Log.e("AuthRepository", "EV Owner registration error", e)
            AuthResult.Failure("Registration failed: ${e.message}")
        }
    }
    
    suspend fun verifyToken(token: String): AuthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext AuthResult.Failure("No internet connection")
            }
            
            val response = apiService.verifyToken()
            if (response.isSuccessful && response.body()?.success == true) {
                val verifyData = response.body()!!.data ?: return@withContext AuthResult.Failure("Token verification failed: No data received")
                
                val user = User(
                    email = verifyData.user.email,
                    id = verifyData.user.id,
                    name = "",
                    phone = "",
                    password = "",
                    role = UserRole.fromString(verifyData.user.role),
                    isActive = true,
                    lastSyncTime = System.currentTimeMillis(),
                    syncedWithServer = true
                )
                
                // Merge with local data if available
                val localUser = dbHelper.getUserByEmail(user.email)
                val finalUser = localUser?.let {
                    user.copy(name = it.name, phone = it.phone, password = it.password)
                } ?: user
                
                AuthResult.Success(finalUser, token)
            } else {
                AuthResult.Failure("Token verification failed")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Token verification error", e)
            AuthResult.Failure("Token verification failed: ${e.message}")
        }
    }
    
    suspend fun logout(): AuthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (networkUtils.isNetworkAvailable()) {
                try {
                    val response = apiService.logout()
                    if (response.isSuccessful) {
                        Log.d("AuthRepository", "Logged out from server")
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Server logout failed", e)
                }
            }
            AuthResult.Success(User(), null, "Logged out successfully")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Logout error", e)
            AuthResult.Success(User(), null, "Logged out locally")
        }
    }
    
    suspend fun syncPendingUsers() = withContext(Dispatchers.IO) {
        if (!networkUtils.isNetworkAvailable()) return@withContext
        
        val unsyncedUsers = dbHelper.getUsersNeedingSync()
        unsyncedUsers.forEach { user ->
            try {
                dbHelper.markAsSynced(user.email)
                Log.d("AuthRepository", "Marked user as synced: ${user.email}")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to sync user ${user.email}", e)
            }
        }
    }
    
    private suspend fun saveOrUpdateUser(user: User) {
        val existingUser = dbHelper.getUserByEmail(user.email)
        if (existingUser != null) {
            // Keep existing name and phone if available
            val updatedUser = user.copy(
                name = if (existingUser.name.isNotEmpty()) existingUser.name else user.name,
                phone = if (existingUser.phone.isNotEmpty()) existingUser.phone else user.phone
            )
            dbHelper.updateUser(updatedUser)
        } else {
            dbHelper.insertUser(user)
        }
    }
    
    private fun generateUserId(): String {
        return "user_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}

sealed class AuthResult {
    data class Success(val user: User, val token: String? = null, val message: String? = null) : AuthResult()
    data class Failure(val error: String) : AuthResult()
}