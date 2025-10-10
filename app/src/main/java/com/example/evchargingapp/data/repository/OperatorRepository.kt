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
    private val bookingApiService = ApiConfig.getAuthenticatedRetrofit(context).create(BookingApiService::class.java)
    private val networkUtils = NetworkUtils(context)
    private val scannedBookingRepository = ScannedBookingRepository(context)
    
    // Updated method that directly updates status to InProgress and returns booking data
    suspend fun scanQRAndStartBooking(qrCode: String): OperatorResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext OperatorResult.Failure("No internet connection")
            }
            
            // Directly update booking status to InProgress and get booking data from response
            val request = BookingStatusUpdateRequest(status = "InProgress")
            val response = bookingApiService.updateBookingStatus(qrCode, request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val bookingData = response.body()!!.data 
                    ?: return@withContext OperatorResult.Failure("No booking data in response")
                
                // Convert BookingDto to BookingDetails for compatibility
                val bookingDetails = BookingDetails(
                    id = bookingData.id,
                    evOwnerId = bookingData.userId,
                    evOwnerName = bookingData.userName ?: "Unknown Owner",
                    stationId = bookingData.chargingStationId,
                    chargingSlotId = bookingData.slotNumber.toString(),
                    bookingDate = bookingData.reservationDateTime.split("T")[0], // Extract date part
                    startTime = bookingData.reservationDateTime.split("T")[1].substring(0, 5), // Extract time part
                    endTime = calculateEndTime(bookingData.reservationDateTime, bookingData.duration),
                    status = bookingData.status, // This will be "InProgress" after the update
                    qrCode = qrCode, // Use the scanned QR code
                    vehicleNumber = null, // Not available in BookingDto
                    contactNumber = null // Not available in BookingDto
                )
                
                // Save to local database for offline access
                try {
                    scannedBookingRepository.saveScannedBookingFromDto(bookingData)
                    Log.d("OperatorRepository", "Booking saved to local database: ${bookingData.id}")
                } catch (dbError: Exception) {
                    Log.e("OperatorRepository", "Failed to save to local database", dbError)
                    // Continue anyway as the main operation (status update) was successful
                }
                
                OperatorResult.BookingFoundWithDto(bookingDetails, bookingData)
            } else {
                OperatorResult.Failure("Failed to update booking status or booking not found")
            }
        } catch (e: Exception) {
            Log.e("OperatorRepository", "Booking status update error", e)
            OperatorResult.Failure("Failed to update booking status: ${e.message}")
        }
    }
    
    // Legacy method name for backward compatibility
    suspend fun getBookingDetails(qrCode: String): OperatorResult {
        return scanQRAndStartBooking(qrCode)
    }
    
    private fun calculateEndTime(startDateTime: String, duration: Int): String {
        return try {
            val startTime = startDateTime.split("T")[1].substring(0, 5) // Extract HH:mm
            val (hours, minutes) = startTime.split(":").map { it.toInt() }
            val totalMinutes = hours * 60 + minutes + duration
            val endHours = (totalMinutes / 60) % 24
            val endMinutes = totalMinutes % 60
            String.format("%02d:%02d", endHours, endMinutes)
        } catch (e: Exception) {
            "Unknown"
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
    
    // New methods for booking status updates
    suspend fun updateBookingStatusToInProgress(bookingId: String): OperatorResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext OperatorResult.Failure("No internet connection")
            }
            
            val request = BookingStatusUpdateRequest(status = "InProgress")
            val response = bookingApiService.updateBookingStatus(bookingId, request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val bookingData = response.body()!!.data 
                    ?: return@withContext OperatorResult.Failure("No booking data returned")
                OperatorResult.StatusUpdateSuccess(bookingData)
            } else {
                OperatorResult.Failure("Failed to update booking status: ${response.body()?.message ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("OperatorRepository", "Status update error", e)
            OperatorResult.Failure("Failed to update booking status: ${e.message}")
        }
    }
    
    suspend fun updateBookingStatusToCompleted(bookingId: String): OperatorResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext OperatorResult.Failure("No internet connection")
            }
            
            val request = BookingStatusUpdateRequest(status = "Completed")
            val response = bookingApiService.updateBookingStatus(bookingId, request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val bookingData = response.body()!!.data 
                    ?: return@withContext OperatorResult.Failure("No booking data returned")
                OperatorResult.StatusUpdateSuccess(bookingData)
            } else {
                OperatorResult.Failure("Failed to update booking status: ${response.body()?.message ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("OperatorRepository", "Status update error", e)
            OperatorResult.Failure("Failed to update booking status: ${e.message}")
        }
    }
}