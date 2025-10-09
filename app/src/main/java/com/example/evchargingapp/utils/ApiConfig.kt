package com.example.evchargingapp.utils

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {
    
    // TODO: Replace with your actual API base URL
    // Using localhost for development - update this when you have a live server
    const val BASE_URL = "https://ev-charging-api.azurewebsites.net/api/"  // Android emulator localhost
    
    // For local testing with different IP configurations
    private const val LOCAL_BASE_URL = "http://192.168.1.100:5000/api/"
    
    // Alternative URLs for testing
    private const val EMULATOR_LOCALHOST = "https://ev-charging-api.azurewebsites.net/api/"  // For Android emulator
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
        const val USE_EMULATOR = true  // Set to true for emulator, false for physical device
    }
    
    // HTTP client configuration
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Create HTTP client with auth interceptor
    private fun createHttpClient(context: Context? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
        
        // Add auth interceptor if context is provided
        context?.let { 
            builder.addInterceptor(AuthInterceptor(it))
        }
        
        return builder.build()
    }

    // Default HTTP client (without auth interceptor)
    private val httpClient = createHttpClient()

    // Retrofit instance (will be updated to use authenticated client)
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

    // Create authenticated retrofit instance with context
    fun getAuthenticatedRetrofit(context: Context): Retrofit {
        val authenticatedClient = createHttpClient(context)
        val baseUrl = when {
            !Environment.USE_LOCALHOST -> BASE_URL  // Remote server
            Environment.USE_EMULATOR -> EMULATOR_LOCALHOST  // Emulator: 10.0.2.2:5000
            else -> DEVICE_LOCALHOST  // Physical device: 192.168.1.100:5000
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authenticatedClient)
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