package com.example.evchargingapp.data.api

import retrofit2.Response
import retrofit2.http.*

interface EVOwnerApiService {
    
    @GET("EVOwner/{nic}")
    suspend fun getEVOwnerByNIC(@Path("nic") nic: String): Response<ApiResponse<EVOwnerDto>>
    
    @PUT("EVOwner/{nic}")
    suspend fun updateEVOwner(
        @Path("nic") nic: String,
        @Body request: UpdateEVOwnerRequest
    ): Response<ApiResponse<EVOwnerDto>>
}