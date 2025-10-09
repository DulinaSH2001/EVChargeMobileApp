package com.example.evchargingapp.data.repository

import android.util.Log
import com.example.evchargingapp.data.api.EVOwnerApiService
import com.example.evchargingapp.data.api.EVOwnerDto
import com.example.evchargingapp.data.api.UpdateEVOwnerRequest

class EVOwnerRepository(private val apiService: EVOwnerApiService) {
    
    companion object {
        private const val TAG = "EVOwnerRepository"
    }
    
    suspend fun getEVOwnerByNIC(nic: String): Result<EVOwnerDto> {
        return try {
            Log.d(TAG, "Fetching EV Owner details for NIC: $nic")
            val response = apiService.getEVOwnerByNIC(nic)
            
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true && apiResponse.data != null) {
                    Log.d(TAG, "Successfully fetched EV Owner: ${apiResponse.data.firstName} ${apiResponse.data.lastName}")
                    Result.success(apiResponse.data)
                } else {
                    val errorMessage = apiResponse?.message ?: "Failed to get EV Owner details"
                    Log.e(TAG, "API error: $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            } else {
                val errorMessage = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "HTTP error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching EV Owner details", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateEVOwner(nic: String, request: UpdateEVOwnerRequest): Result<EVOwnerDto> {
        return try {
            Log.d(TAG, "Updating EV Owner details for NIC: $nic")
            val response = apiService.updateEVOwner(nic, request)
            
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true && apiResponse.data != null) {
                    Log.d(TAG, "Successfully updated EV Owner: ${apiResponse.data.firstName} ${apiResponse.data.lastName}")
                    Result.success(apiResponse.data)
                } else {
                    val errorMessage = apiResponse?.message ?: "Failed to update EV Owner details"
                    Log.e(TAG, "API error: $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            } else {
                val errorMessage = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, "HTTP error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while updating EV Owner details", e)
            Result.failure(e)
        }
    }
}