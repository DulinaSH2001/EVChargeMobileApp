package com.example.evchargingapp.utils

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {
    
    // TODO: Replace with your actual API base URL
    // Using localhost for development - update this when you have a live server
    private const val BASE_URL = "http://10.0.2.2:5000/api/"  // Android emulator localhost
    
    // For local testing with different IP configurations
    private const val LOCAL_BASE_URL = "http://192.168.1.100:3000/api/"
    
    // Alternative URLs for testing
    private const val EMULATOR_LOCALHOST = "http://10.0.2.2:5000/api/"  // For Android emulator
    private const val DEVICE_LOCALHOST = "http://192.168.1.100:5000/api/"  // For physical device on same network
    
    // API endpoints - Updated to match backend controller
    object Endpoints {
        const val LOGIN = "auth/login"
        const val LOGOUT = "auth/logout"
        const val VERIFY_TOKEN = "auth/verify"
        const val REGISTER = "EVOwner"  // Updated to match actual endpoint used in logs
    }
    
    // Environment configuration
    object Environment {
        const val USE_LOCALHOST = true  // Set to false when using remote server
        const val ENABLE_OFFLINE_MODE = true  // Always allow offline fallback
    }
    
    // HTTP client configuration
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    // Retrofit instance
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // Alternative retrofit for local testing
    val localRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(LOCAL_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // Helper method to switch between local and remote API
    fun getRetrofitInstance(useLocal: Boolean = Environment.USE_LOCALHOST): Retrofit {
        return if (useLocal) localRetrofit else retrofit
    }
    
    // Get the appropriate base URL for current environment
    fun getCurrentBaseUrl(): String {
        return if (Environment.USE_LOCALHOST) LOCAL_BASE_URL else BASE_URL
    }
    
    // Check if we're using offline-first mode
    fun isOfflineModeEnabled(): Boolean = Environment.ENABLE_OFFLINE_MODE
}