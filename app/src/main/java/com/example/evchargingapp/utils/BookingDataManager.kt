package com.example.evchargingapp.utils

import android.util.Log
import com.example.evchargingapp.data.api.BookingDto
import com.example.evchargingapp.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton class to manage booking data across the app
 * Fetches all bookings once and provides filtered views to different fragments
 */
class BookingDataManager private constructor() {
    
    companion object {
        private const val TAG = "BookingDataManager"
        
        @Volatile
        private var INSTANCE: BookingDataManager? = null
        
        fun getInstance(): BookingDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BookingDataManager().also { INSTANCE = it }
            }
        }
    }
    
    private val _allBookings = MutableStateFlow<List<BookingDto>>(emptyList())
    val allBookings: StateFlow<List<BookingDto>> = _allBookings.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var lastRefreshTime = 0L
    private val cacheValidityDuration = 5 * 60 * 1000L // 5 minutes
    
    /**
     * Refresh bookings from API
     */
    suspend fun refreshBookings(bookingRepository: BookingRepository, forceRefresh: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        
        // Check if cache is still valid and not forcing refresh
        if (!forceRefresh && currentTime - lastRefreshTime < cacheValidityDuration && _allBookings.value.isNotEmpty()) {
            Log.d(TAG, "Using cached bookings, last refresh: ${(currentTime - lastRefreshTime) / 1000}s ago")
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        try {
            val result = bookingRepository.getAllMyBookings()
            result.onSuccess { bookings ->
                _allBookings.value = bookings
                lastRefreshTime = currentTime
                Log.d(TAG, "Refreshed ${bookings.size} bookings from API")
            }.onFailure { exception ->
                _error.value = exception.message ?: "Failed to load bookings"
                Log.e(TAG, "Failed to refresh bookings", exception)
            }
        } catch (e: Exception) {
            _error.value = "Error loading bookings"
            Log.e(TAG, "Exception while refreshing bookings", e)
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Get bookings filtered by status
     */
    fun getBookingsByStatus(status: String?): List<BookingDto> {
        return if (status != null) {
            _allBookings.value.filter { it.status.equals(status, ignoreCase = true) }
        } else {
            _allBookings.value
        }
    }
    
    /**
     * Get booking statistics
     */
    fun getBookingStats(): BookingStats {
        val allBookings = _allBookings.value
        return BookingStats(
            pending = allBookings.count { it.status.equals("PENDING", ignoreCase = true) },
            approved = allBookings.count { it.status.equals("APPROVED", ignoreCase = true) },
            completed = allBookings.count { it.status.equals("COMPLETED", ignoreCase = true) },
            total = allBookings.size
        )
    }
    
    /**
     * Clear cache (useful for logout or data reset)
     */
    fun clearCache() {
        _allBookings.value = emptyList()
        _error.value = null
        lastRefreshTime = 0L
        Log.d(TAG, "Cache cleared")
    }
    
    /**
     * Update a specific booking in cache (after edit/cancel operations)
     */
    fun updateBookingInCache(updatedBooking: BookingDto) {
        val currentBookings = _allBookings.value.toMutableList()
        val index = currentBookings.indexOfFirst { it.id == updatedBooking.id }
        if (index != -1) {
            currentBookings[index] = updatedBooking
            _allBookings.value = currentBookings
            Log.d(TAG, "Updated booking ${updatedBooking.id} in cache")
        }
    }
    
    /**
     * Remove a booking from cache (after cancellation)
     */
    fun removeBookingFromCache(bookingId: String) {
        val currentBookings = _allBookings.value.toMutableList()
        val removed = currentBookings.removeAll { it.id == bookingId }
        if (removed) {
            _allBookings.value = currentBookings
            Log.d(TAG, "Removed booking $bookingId from cache")
        }
    }
    
    /**
     * Add a new booking to cache (after creation)
     */
    fun addBookingToCache(newBooking: BookingDto) {
        val currentBookings = _allBookings.value.toMutableList()
        currentBookings.add(0, newBooking) // Add at the beginning
        _allBookings.value = currentBookings
        Log.d(TAG, "Added new booking ${newBooking.id} to cache")
    }
}

/**
 * Data class for booking statistics
 */
data class BookingStats(
    val pending: Int,
    val approved: Int,
    val completed: Int,
    val total: Int
)