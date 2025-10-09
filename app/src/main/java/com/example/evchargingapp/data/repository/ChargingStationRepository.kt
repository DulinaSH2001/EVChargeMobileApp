package com.example.evchargingapp.data.repository

import android.util.Log
import com.example.evchargingapp.data.api.ApiResponse
import com.example.evchargingapp.data.api.ChargingStationApiService
import com.example.evchargingapp.data.api.ChargingStationDto
import com.example.evchargingapp.data.api.DashboardStats
import com.example.evchargingapp.data.api.BookingDetails
import com.example.evchargingapp.utils.LocationUtils
import retrofit2.Response

class ChargingStationRepository(private val apiService: ChargingStationApiService) {
    
    /**
     * Get nearby stations by first fetching all stations, then filtering by location
     */
    suspend fun getNearbyStations(
        latitude: Double, 
        longitude: Double, 
        radius: Double = 10.0
    ): Result<List<ChargingStationDto>> {
        return try {
            Log.d("ChargingStationRepo", "Fetching nearby stations for location: $latitude, $longitude within ${radius}km")
            
            // First get all stations from the API
            val allStationsResult = getAllStations()
            
            if (allStationsResult.isSuccess) {
                val allStations = allStationsResult.getOrNull() ?: emptyList()
                Log.d("ChargingStationRepo", "Retrieved ${allStations.size} total stations")
                
                // Filter stations within radius and add distance information
                val nearbyStations = LocationUtils.filterStationsWithinRadius(
                    stations = allStations,
                    userLatitude = latitude,
                    userLongitude = longitude,
                    radiusKm = radius
                )
                
                Log.d("ChargingStationRepo", "Found ${nearbyStations.size} stations within ${radius}km")
                Result.success(nearbyStations)
            } else {
                Log.e("ChargingStationRepo", "Failed to get all stations")
                allStationsResult
            }
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
    
    /**
     * Search stations by name or location
     */
    suspend fun searchStations(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<List<ChargingStationDto>> {
        return try {
            Log.d("ChargingStationRepo", "Searching stations with query: $query")
            
            // Get all stations first
            val allStationsResult = getAllStations()
            
            if (allStationsResult.isSuccess) {
                val allStations = allStationsResult.getOrNull() ?: emptyList()
                
                // Filter stations by search query
                val filteredStations = allStations.filter { station ->
                    station.name.contains(query, ignoreCase = true) ||
                    station.location.contains(query, ignoreCase = true) ||
                    station.getParsedAddress().contains(query, ignoreCase = true)
                }
                
                Log.d("ChargingStationRepo", "Found ${filteredStations.size} stations matching query")
                Result.success(filteredStations)
            } else {
                Log.e("ChargingStationRepo", "Failed to get all stations for search")
                allStationsResult
            }
        } catch (e: Exception) {
            Log.e("ChargingStationRepo", "Error searching stations", e)
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