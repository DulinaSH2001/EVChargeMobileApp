package com.example.evchargingapp.data.api

import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponseData>>
    
    @POST("auth/evowner/login")
    suspend fun loginWithNic(@Body request: NicLoginRequest): Response<ApiResponse<LoginResponseData>>
    
    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Any>>
    
    @GET("auth/verify")
    suspend fun verifyToken(): Response<ApiResponse<VerifyTokenResponseData>>
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<LoginResponseData>>
    
    @POST("EVOwner")
    suspend fun registerEvOwner(@Body request: EvOwnerRegisterRequest): Response<ApiResponse<EvOwnerRegisterResponseData>>
    
    // Operator specific endpoints
    @POST("auth/login")
    suspend fun operatorLogin(@Body request: OperatorLoginRequest): Response<ApiResponse<OperatorLoginResponseData>>
    
    @POST("operator/confirm")
    suspend fun confirmBooking(@Body request: OperatorConfirmRequest): Response<ApiResponse<OperatorSessionResponse>>
    
    @POST("operator/finalize")
    suspend fun finalizeBooking(@Body request: OperatorFinalizeRequest): Response<ApiResponse<OperatorSessionResponse>>
    
    @GET("operator/booking/{qrCode}")
    suspend fun getBookingByQr(@Path("qrCode") qrCode: String): Response<ApiResponse<BookingDetails>>
    
    @GET("operator/profile")
    suspend fun getOperatorProfile(): Response<ApiResponse<OperatorUser>>
    
    @PUT("operator/profile")
    suspend fun updateOperatorProfile(@Body request: OperatorProfileUpdateRequest): Response<ApiResponse<OperatorUser>>
}