package com.example.evchargingapp.data.repository

import android.content.Context
import android.util.Log
import com.example.evchargingapp.data.api.*
import com.example.evchargingapp.utils.ApiConfig
import com.example.evchargingapp.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OperatorRepository(private val context: Context) {
    
    private val apiService = ApiConfig.getAuthenticatedRetrofit(context).create(AuthApiService::class.java)
    private val networkUtils = NetworkUtils(context)
    
    suspend fun getBookingDetails(qrCode: String): OperatorResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext OperatorResult.Failure("No internet connection")
            }
            
            val response = apiService.getBookingByQr(qrCode)
            if (response.isSuccessful && response.body()?.success == true) {
                val bookingData = response.body()!!.data 
                    ?: return@withContext OperatorResult.Failure("No booking found")
                OperatorResult.BookingFound(bookingData)
            } else {
                OperatorResult.Failure("Booking not found or invalid QR code")
            }
        } catch (e: Exception) {
            Log.e("OperatorRepository", "Booking lookup error", e)
            OperatorResult.Failure("Failed to lookup booking: ${e.message}")
        }
    }
    
    suspend fun confirmBooking(bookingId: String, operatorId: String): OperatorResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext OperatorResult.Failure("No internet connection")
            }
            
            val request = OperatorConfirmRequest(
                bookingId = bookingId,
                operatorId = operatorId,
                startTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            )
            
            val response = apiService.confirmBooking(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val sessionData = response.body()!!.data 
                    ?: return@withContext OperatorResult.Failure("No session data received")
                OperatorResult.SessionResult(sessionData)
            } else {
                OperatorResult.Failure("Failed to confirm booking: ${response.body()?.message ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("OperatorRepository", "Booking confirmation error", e)
            OperatorResult.Failure("Failed to confirm booking: ${e.message}")
        }
    }
    
    suspend fun finalizeBooking(
        bookingId: String, 
        operatorId: String, 
        energyConsumed: Double?, 
        totalCost: Double?
    ): OperatorResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext OperatorResult.Failure("No internet connection")
            }
            
            val request = OperatorFinalizeRequest(
                bookingId = bookingId,
                operatorId = operatorId,
                endTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                energyConsumed = energyConsumed,
                totalCost = totalCost
            )
            
            val response = apiService.finalizeBooking(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val sessionData = response.body()!!.data 
                    ?: return@withContext OperatorResult.Failure("No session data received")
                OperatorResult.SessionResult(sessionData)
            } else {
                OperatorResult.Failure("Failed to finalize booking: ${response.body()?.message ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("OperatorRepository", "Booking finalization error", e)
            OperatorResult.Failure("Failed to finalize booking: ${e.message}")
        }
    }
    
    suspend fun getOperatorProfile(): OperatorResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext OperatorResult.Failure("No internet connection")
            }
            
            val response = apiService.getOperatorProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                val operatorData = response.body()!!.data 
                    ?: return@withContext OperatorResult.Failure("No profile data")
                OperatorResult.ProfileResult(operatorData)
            } else {
                OperatorResult.Failure("Failed to load profile: ${response.body()?.message ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("OperatorRepository", "Profile load error", e)
            OperatorResult.Failure("Failed to load profile: ${e.message}")
        }
    }
    
    suspend fun updateOperatorProfile(
        name: String?, 
        phone: String?, 
        stationId: String?
    ): OperatorResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext OperatorResult.Failure("No internet connection")
            }
            
            val request = OperatorProfileUpdateRequest(
                name = name,
                phone = phone,
                stationId = stationId
            )
            
            val response = apiService.updateOperatorProfile(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val operatorData = response.body()!!.data 
                    ?: return@withContext OperatorResult.Failure("No updated profile data")
                OperatorResult.ProfileResult(operatorData)
            } else {
                OperatorResult.Failure("Failed to update profile: ${response.body()?.message ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("OperatorRepository", "Profile update error", e)
            OperatorResult.Failure("Failed to update profile: ${e.message}")
        }
    }
}