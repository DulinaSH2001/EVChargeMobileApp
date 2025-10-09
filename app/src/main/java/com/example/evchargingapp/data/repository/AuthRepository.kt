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
    private val authenticatedApiService by lazy { ApiConfig.getAuthenticatedRetrofit(context).create(AuthApiService::class.java) }
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
        Log.d("AuthRepository", "Attempting remote login for userType: $userType")
        
        val response = when (userType) {
            UserType.STATION_OPERATOR -> apiService.operatorLogin(OperatorLoginRequest(identifier, password))
            UserType.EV_OWNER -> apiService.loginWithNic(NicLoginRequest(identifier, password))
            else -> return AuthResult.Failure("Invalid user type")
        }
        
        Log.d("AuthRepository", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
        Log.d("AuthRepository", "Response body success: ${response.body()?.success}")
        
        if (response.isSuccessful && response.body()?.success == true) {
            val responseBody = response.body()!!
            Log.d("AuthRepository", "Processing successful response for $userType")
            
            val user = when (userType) {
                UserType.STATION_OPERATOR -> {
                    // Cast to operator login response
                    val operatorData = responseBody.data as? OperatorLoginResponseData
                    
                    Log.d("AuthRepository", "operatorData: $operatorData")
                    Log.d("AuthRepository", "user: ${operatorData?.user}")
                    Log.d("AuthRepository", "token: ${operatorData?.token}")
                    
                    if (operatorData?.user != null) {
                        val roleString = operatorData.user.role
                        Log.d("AuthRepository", "Role from backend: $roleString")
                        val mappedRole = UserRole.fromString(roleString)
                        Log.d("AuthRepository", "Mapped role: $mappedRole")
                        
                        User(
                            email = operatorData.user.email,
                            id = operatorData.user.id,
                            name = operatorData.user.email, // Using email as name since name isn't provided
                            phone = "",
                            password = password,
                            role = mappedRole,
                            isActive = true,
                            lastSyncTime = System.currentTimeMillis(),
                            syncedWithServer = true
                        )
                    } else {
                        Log.d("AuthRepository", "operatorData or user is null, falling back to local login")
                        return handleLocalHybridLogin(identifier, password)
                    }
                }
                UserType.EV_OWNER -> {
                    val loginData = responseBody.data as? LoginResponseData
                    
                    if (loginData != null) {
                        val firstName = loginData.user.firstName ?: ""
                        val lastName = loginData.user.lastName ?: ""
                        val fullName = "$firstName $lastName".trim()
                        
                        Log.d("AuthRepository", "Login API response - firstName: '$firstName', lastName: '$lastName', fullName: '$fullName'")
                        
                        // For EV Owners, the complete profile will be fetched immediately after login
                        // So we just provide a basic user object here
                        val displayName = if (fullName.isNotEmpty()) fullName else ""
                        
                        User(
                            email = identifier,
                            id = loginData.user.id,
                            name = displayName,
                            phone = "",
                            password = password,
                            role = UserRole.fromString(loginData.user.role),
                            isActive = true,
                            lastSyncTime = System.currentTimeMillis(),
                            syncedWithServer = true
                        )
                    } else {
                        return handleLocalHybridLogin(identifier, password)
                    }
                }
                else -> return AuthResult.Failure("Invalid user type")
            }
            
            val token = when (userType) {
                UserType.STATION_OPERATOR -> {
                    val operatorData = responseBody.data as? OperatorLoginResponseData
                    operatorData?.token
                }
                UserType.EV_OWNER -> {
                    val loginData = responseBody.data as? LoginResponseData
                    loginData?.token
                }
                else -> null
            }
            
            // Merge with local data
            val existingUser = dbHelper.getUserByEmail(user.email)
            val finalUser = existingUser?.let {
                user.copy(name = if (it.name.isNotEmpty()) it.name else user.name, phone = it.phone)
            } ?: user
            
            Log.d("AuthRepository", "Final user created: ${finalUser.name}, email: ${finalUser.email}, role: ${finalUser.role}")
            
            saveOrUpdateUser(finalUser)
            return AuthResult.Success(finalUser, token)
        }
        
        Log.d("AuthRepository", "Login failed - Response code: ${response.code()}")
        return if (response.code() == 401) {
            AuthResult.Failure("Invalid credentials")
        } else {
            Log.d("AuthRepository", "Falling back to local login due to non-401 error")
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
            // Check network availability first - registration requires internet connection
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext AuthResult.Failure("Internet connection required for registration")
            }
            
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
            
            // Attempt API registration - no local fallback for registration
            val response = apiService.registerEvOwner(registerRequest)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val responseBody = response.body()!!
                val registeredUser = responseBody.data
                
                // Create user object from server response
                val user = User(
                    email = email,
                    id = generateUserId(), // Generate local ID since server doesn't return one for EVOwner registration
                    name = "$firstName $lastName".trim(),
                    phone = registeredUser?.phone ?: phone,
                    password = password, // Keep password for potential future login
                    role = UserRole.EV_OWNER,
                    isActive = registeredUser?.isActive ?: true,
                    lastSyncTime = System.currentTimeMillis(),
                    syncedWithServer = true, // Always true since we're only using API
                    address = registeredUser?.address ?: address,
                    dateOfBirth = registeredUser?.dateOfBirth ?: dateOfBirth
                )
                
                Log.d("AuthRepository", "Registration successful via API for user: ${user.email}")
                AuthResult.Success(user, null, "Registration successful")
            } else {
                val errorMessage = response.body()?.message ?: "Registration failed"
                Log.e("AuthRepository", "API registration failed: $errorMessage")
                AuthResult.Failure("Registration failed: $errorMessage")
            }
            
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration error", e)
            AuthResult.Failure("Registration failed: ${e.message}")
        }
    }
    
    suspend fun verifyToken(token: String): AuthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext AuthResult.Failure("No internet connection")
            }
            
            val response = authenticatedApiService.verifyToken()
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
                    val response = authenticatedApiService.logout()
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
    
    suspend fun validateNic(nic: String): NicValidationResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext NicValidationResult.Failure("No internet connection")
            }
            
            val response = apiService.validateNic(nic)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data
                if (data != null) {
                    NicValidationResult.Success(data.email)
                } else {
                    NicValidationResult.Failure("No data received")
                }
            } else {
                val errorMessage = response.body()?.message ?: "User not found"
                NicValidationResult.Failure(errorMessage)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "NIC validation error", e)
            NicValidationResult.Failure("Validation failed: ${e.message}")
        }
    }
    
    suspend fun loginWithEmail(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext AuthResult.Failure("No internet connection")
            }
            
            val response = apiService.login(LoginRequest(email, password))
            
            if (response.isSuccessful && response.body()?.success == true) {
                val responseBody = response.body()!!
                val loginData = responseBody.data
                
                if (loginData != null) {
                    val firstName = loginData.user.firstName ?: ""
                    val lastName = loginData.user.lastName ?: ""
                    val fullName = "$firstName $lastName".trim()
                    
                    val user = User(
                        email = email,
                        id = loginData.user.id,
                        name = fullName,
                        phone = "",
                        password = password,
                        role = UserRole.fromString(loginData.user.role),
                        isActive = true,
                        lastSyncTime = System.currentTimeMillis(),
                        syncedWithServer = true
                    )
                    
                    AuthResult.Success(user, loginData.token, "Login successful")
                } else {
                    AuthResult.Failure("Login failed: No user data received")
                }
            } else {
                val errorMessage = response.body()?.message ?: "Invalid credentials"
                AuthResult.Failure(errorMessage)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Email login error", e)
            AuthResult.Failure("Login failed: ${e.message}")
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
    
    // Operator specific methods
    suspend fun getBookingDetails(qrCode: String): AuthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext AuthResult.Failure("No internet connection")
            }
            
            val response = authenticatedApiService.getBookingByQr(qrCode)
            if (response.isSuccessful && response.body()?.success == true) {
                val bookingData = response.body()!!.data ?: return@withContext AuthResult.Failure("No booking found")
                AuthResult.Success(User(), null, "Booking found") // Using User for generic response, data would be in custom result
            } else {
                AuthResult.Failure("Booking not found or invalid QR code")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Booking lookup error", e)
            AuthResult.Failure("Failed to lookup booking: ${e.message}")
        }
    }
    
    suspend fun confirmBooking(bookingId: String, operatorId: String): AuthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext AuthResult.Failure("No internet connection")
            }
            
            val request = OperatorConfirmRequest(
                bookingId = bookingId,
                operatorId = operatorId,
                startTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            )
            
            val response = authenticatedApiService.confirmBooking(request)
            if (response.isSuccessful && response.body()?.success == true) {
                AuthResult.Success(User(), null, "Booking confirmed successfully")
            } else {
                AuthResult.Failure("Failed to confirm booking")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Booking confirmation error", e)
            AuthResult.Failure("Failed to confirm booking: ${e.message}")
        }
    }
    
    suspend fun finalizeBooking(bookingId: String, operatorId: String, energyConsumed: Double?, totalCost: Double?): AuthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext AuthResult.Failure("No internet connection")
            }
            
            val request = OperatorFinalizeRequest(
                bookingId = bookingId,
                operatorId = operatorId,
                endTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                energyConsumed = energyConsumed,
                totalCost = totalCost
            )
            
            val response = authenticatedApiService.finalizeBooking(request)
            if (response.isSuccessful && response.body()?.success == true) {
                AuthResult.Success(User(), null, "Booking finalized successfully")
            } else {
                AuthResult.Failure("Failed to finalize booking")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Booking finalization error", e)
            AuthResult.Failure("Failed to finalize booking: ${e.message}")
        }
    }
    
    suspend fun getOperatorProfile(): AuthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext AuthResult.Failure("No internet connection")
            }
            
            val response = authenticatedApiService.getOperatorProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                val operatorData = response.body()!!.data ?: return@withContext AuthResult.Failure("No profile data")
                AuthResult.Success(User(), null, "Profile loaded")
            } else {
                AuthResult.Failure("Failed to load profile")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Profile load error", e)
            AuthResult.Failure("Failed to load profile: ${e.message}")
        }
    }
    
    suspend fun updateOperatorProfile(name: String?, phone: String?, stationId: String?): AuthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext AuthResult.Failure("No internet connection")
            }
            
            val request = OperatorProfileUpdateRequest(
                name = name,
                phone = phone,
                stationId = stationId
            )
            
            val response = authenticatedApiService.updateOperatorProfile(request)
            if (response.isSuccessful && response.body()?.success == true) {
                AuthResult.Success(User(), null, "Profile updated successfully")
            } else {
                AuthResult.Failure("Failed to update profile")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Profile update error", e)
            AuthResult.Failure("Failed to update profile: ${e.message}")
        }
    }
}

sealed class NicValidationResult {
    data class Success(val email: String) : NicValidationResult()
    data class Failure(val error: String) : NicValidationResult()
}

sealed class AuthResult {
    data class Success(val user: User, val token: String? = null, val message: String? = null) : AuthResult()
    data class Failure(val error: String) : AuthResult()
}