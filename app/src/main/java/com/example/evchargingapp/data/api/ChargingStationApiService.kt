package com.example.evchargingapp.data.api

import retrofit2.Response
import retrofit2.http.*

interface ChargingStationApiService {
    
    @GET("ChargingStation/nearby")
    suspend fun getNearbyStations(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Double = 10.0
    ): Response<ApiResponse<List<ChargingStationDto>>>
    
    @GET("ChargingStation/{id}")
    suspend fun getStationById(
        @Path("id") stationId: String
    ): Response<ApiResponse<ChargingStationDto>>
    
    @GET("ChargingStation")
    suspend fun getAllStations(): Response<ApiResponse<List<ChargingStationDto>>>
}