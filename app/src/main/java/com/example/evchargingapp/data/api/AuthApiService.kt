package com.example.evchargingapp.data.api

import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponseData>>
    
    @POST("auth/login")
    suspend fun loginWithNic(@Body request: NicLoginRequest): Response<ApiResponse<LoginResponseData>>
    
    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Any>>
    
    @GET("auth/verify")
    suspend fun verifyToken(): Response<ApiResponse<VerifyTokenResponseData>>
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<LoginResponseData>>
    
    @POST("EVOwner")
    suspend fun registerEvOwner(@Body request: EvOwnerRegisterRequest): Response<ApiResponse<EvOwnerRegisterResponseData>>
}