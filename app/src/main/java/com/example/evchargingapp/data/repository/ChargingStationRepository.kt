package com.example.evchargingapp.data.repository

import android.util.Log
import com.example.evchargingapp.data.api.ApiResponse
import com.example.evchargingapp.data.api.ChargingStationApiService
import com.example.evchargingapp.data.api.ChargingStationDto
import com.example.evchargingapp.data.api.DashboardStats
import com.example.evchargingapp.data.api.BookingDetails
import retrofit2.Response

class ChargingStationRepository(private val apiService: ChargingStationApiService) {
    
    suspend fun getNearbyStations(
        latitude: Double, 
        longitude: Double, 
        radius: Double = 10.0
    ): Result<List<ChargingStationDto>> {
        return try {
            val response = apiService.getNearbyStations(latitude, longitude, radius)
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("ChargingStationRepo", "Error fetching nearby stations", e)
            Result.failure(e)
        }
    }
    
    suspend fun getStationById(stationId: String): Result<ChargingStationDto> {
        return try {
            val response = apiService.getStationById(stationId)
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("ChargingStationRepo", "Error fetching station by id", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAllStations(): Result<List<ChargingStationDto>> {
        return try {
            val response = apiService.getAllStations()
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("ChargingStationRepo", "Error fetching all stations", e)
            Result.failure(e)
        }
    }
    
    suspend fun getPendingReservations(): Result<List<BookingDetails>> {
        return try {
            val response = apiService.getPendingReservations()
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("ChargingStationRepo", "Error fetching pending reservations", e)
            Result.failure(e)
        }
    }
    
    suspend fun getApprovedReservations(): Result<List<BookingDetails>> {
        return try {
            val response = apiService.getApprovedReservations()
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("ChargingStationRepo", "Error fetching approved reservations", e)
            Result.failure(e)
        }
    }
    
    suspend fun getDashboardStats(): Result<DashboardStats> {
        return try {
            val response = apiService.getDashboardStats()
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("ChargingStationRepo", "Error fetching dashboard stats", e)
            Result.failure(e)
        }
    }
    
    private fun <T> handleApiResponse(response: Response<ApiResponse<T>>): Result<T> {
        return if (response.isSuccessful) {
            val apiResponse = response.body()
            if (apiResponse?.success == true && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(Exception(apiResponse?.message ?: "Unknown error"))
            }
        } else {
            Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
        }
    }
}