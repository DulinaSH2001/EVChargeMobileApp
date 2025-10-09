package com.example.evchargingapp.utils

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/**
 * HTTP Interceptor that automatically adds Bearer token to all authenticated API requests
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        
        // Get the current auth token
        val sessionManager = SessionManager(context)
        val token = sessionManager.getToken()
        
        // Debug logging
        android.util.Log.d("AuthInterceptor", "Intercepting request to: $url")
        android.util.Log.d("AuthInterceptor", "Token available: ${!token.isNullOrEmpty()}")
        if (!token.isNullOrEmpty()) {
            android.util.Log.d("AuthInterceptor", "Token preview: ${token.take(20)}...")
        }
        
        // If no token is available or this is an auth endpoint, proceed without modification
        if (token.isNullOrEmpty() || isAuthEndpoint(url)) {
            android.util.Log.d("AuthInterceptor", "Proceeding without auth header")
            return chain.proceed(originalRequest)
        }
        
        // Add Bearer token to the request
        android.util.Log.d("AuthInterceptor", "Adding Bearer token to request")
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        
        val response = chain.proceed(authenticatedRequest)
        
        // Log response status
        android.util.Log.d("AuthInterceptor", "Response status: ${response.code}")
        if (response.code == 401) {
            android.util.Log.w("AuthInterceptor", "Received 401 Unauthorized - Token may be invalid or expired")
        }
        
        return response
    }
    
    /**
     * Check if the request is to an authentication endpoint that doesn't need token
     */
    private fun isAuthEndpoint(url: String): Boolean {
        val authEndpoints = listOf(
            "/auth/login",
            "/auth/evowner/login", 
            "/auth/register",
            "/EVOwner",  // Registration endpoint
            "/EVOwner/user/",  // NIC validation endpoint
            "login",     // Any URL containing 'login'
            "register"   // Any URL containing 'register'
        )
        
        val isAuthEndpoint = authEndpoints.any { endpoint ->
            url.contains(endpoint, ignoreCase = true)
        }
        
        // Debug logging
        android.util.Log.d("AuthInterceptor", "URL: $url, Is Auth Endpoint: $isAuthEndpoint")
        
        return isAuthEndpoint
    }
}