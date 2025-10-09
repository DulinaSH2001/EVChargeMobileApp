package com.example.evchargingapp.data.manager

import android.util.Log
import com.example.evchargingapp.data.api.BookingDto
import com.example.evchargingapp.data.api.ChargingStationApiService
import com.example.evchargingapp.data.api.UpdateBookingRequest
import com.example.evchargingapp.data.repository.BookingRepository
import com.example.evchargingapp.data.repository.ChargingStationRepository
import com.example.evchargingapp.utils.ApiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton data manager for booking operations that provides caching,
 * reactive updates, and efficient data management across the app.
 */
class BookingDataManager private constructor(
    private val bookingRepository: BookingRepository,
    private val chargingStationRepository: ChargingStationRepository
) {
    companion object {
        private const val TAG = "BookingDataManager"
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
        
        @Volatile
        private var INSTANCE: BookingDataManager? = null
        
        fun initialize(bookingRepository: BookingRepository, chargingStationRepository: ChargingStationRepository) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = BookingDataManager(bookingRepository, chargingStationRepository)
                    }
                }
            }
        }
        
        fun getInstance(): BookingDataManager {
            return INSTANCE ?: throw IllegalStateException("BookingDataManager must be initialized first")
        }
    }
    
    // Cache for bookings with timestamp
    private data class CachedBookings(
        val bookings: List<BookingDto>,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isValid(): Boolean = System.currentTimeMillis() - timestamp < CACHE_VALIDITY_MS
    }
    
    // StateFlow for reactive updates
    private val _allBookings = MutableStateFlow<List<BookingDto>>(emptyList())
    private val allBookings: StateFlow<List<BookingDto>> = _allBookings.asStateFlow()
    
    // Cache management
    private var cachedBookings: CachedBookings? = null
    private val bookingCache = ConcurrentHashMap<String, BookingDto>()
    
    /**
     * Get all bookings as a reactive StateFlow
     */
    fun getAllBookings(): StateFlow<List<BookingDto>> {
        // If cache is invalid or empty, refresh in background
        if (cachedBookings?.isValid() != true) {
            refreshBookingsInBackground()
        }
        return allBookings
    }
    
    /**
     * Get bookings filtered by status
     */
    fun getBookingsByStatus(status: String): StateFlow<List<BookingDto>> {
        val filteredFlow = MutableStateFlow<List<BookingDto>>(emptyList())
        
        // Update filtered flow whenever all bookings change
        allBookings.value.let { bookings ->
            filteredFlow.value = bookings.filter { it.status.equals(status, ignoreCase = true) }
        }
        
        return filteredFlow.asStateFlow()
    }
    
    /**
     * Refresh bookings from API and update cache
     */
    suspend fun refreshBookings(): Result<List<BookingDto>> {
        return try {
            Log.d(TAG, "Refreshing bookings from API")
            val result = bookingRepository.getAllMyBookings()
            
            result.onSuccess { bookings ->
                // Fetch charging station details for each booking
                val bookingsWithStationDetails = fetchStationDetailsForBookings(bookings)
                
                // Update cache
                cachedBookings = CachedBookings(bookingsWithStationDetails)
                
                // Update StateFlow
                _allBookings.value = bookingsWithStationDetails
                
                // Update individual booking cache
                bookingCache.clear()
                bookingsWithStationDetails.forEach { booking ->
                    booking.id?.let { id -> bookingCache[id] = booking }
                }
                
                Log.d(TAG, "Successfully refreshed ${bookingsWithStationDetails.size} bookings with station details")
            }.onFailure { exception ->
                Log.e(TAG, "Failed to refresh bookings", exception)
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception while refreshing bookings", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get a specific booking by ID from cache or API
     */
    suspend fun getBookingById(bookingId: String): Result<BookingDto?> {
        // Check cache first
        bookingCache[bookingId]?.let { cachedBooking ->
            Log.d(TAG, "Returning cached booking: $bookingId")
            return Result.success(cachedBooking)
        }
        
        // Fallback to API if not in cache
        return try {
            Log.d(TAG, "Fetching booking from API: $bookingId")
            val result = bookingRepository.getBookingById(bookingId)
            
            result.onSuccess { booking ->
                booking?.let { bookingCache[bookingId] = it }
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get booking by ID: $bookingId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add or update a booking in the cache
     */
    fun updateBookingInCache(booking: BookingDto) {
        booking.id?.let { id ->
            bookingCache[id] = booking
            
            // Update the main list
            val currentBookings = _allBookings.value.toMutableList()
            val existingIndex = currentBookings.indexOfFirst { it.id == id }
            
            if (existingIndex >= 0) {
                currentBookings[existingIndex] = booking
            } else {
                currentBookings.add(booking)
            }
            
            _allBookings.value = currentBookings
            
            // Update cached bookings timestamp to keep it valid
            cachedBookings = cachedBookings?.copy(
                bookings = currentBookings,
                timestamp = System.currentTimeMillis()
            )
            
            Log.d(TAG, "Updated booking in cache: $id")
        }
    }
    
    /**
     * Remove a booking from cache
     */
    fun removeBookingFromCache(bookingId: String) {
        bookingCache.remove(bookingId)
        
        val currentBookings = _allBookings.value.toMutableList()
        currentBookings.removeAll { it.id == bookingId }
        _allBookings.value = currentBookings
        
        // Update cached bookings
        cachedBookings = cachedBookings?.copy(
            bookings = currentBookings,
            timestamp = System.currentTimeMillis()
        )
        
        Log.d(TAG, "Removed booking from cache: $bookingId")
    }
    
    /**
     * Clear all cached data
     */
    fun clearCache() {
        cachedBookings = null
        bookingCache.clear()
        _allBookings.value = emptyList()
        Log.d(TAG, "Cleared all cached booking data")
    }
    
    /**
     * Check if cache is valid and has data
     */
    fun isCacheValid(): Boolean {
        return cachedBookings?.isValid() == true && _allBookings.value.isNotEmpty()
    }
    
    /**
     * Get cache statistics for debugging
     */
    fun getCacheStats(): String {
        val cached = cachedBookings
        return if (cached != null) {
            val age = System.currentTimeMillis() - cached.timestamp
            "Cache: ${cached.bookings.size} bookings, age: ${age}ms, valid: ${cached.isValid()}"
        } else {
            "Cache: empty"
        }
    }
    
    /**
     * Refresh bookings in background without blocking
     */
    private fun refreshBookingsInBackground() {
        // This would typically use a background coroutine scope
        // For now, we'll just mark that a refresh is needed
        Log.d(TAG, "Background refresh needed - cache invalid or empty")
    }
    
    /**
     * Delete/cancel a booking and update cache
     */
    suspend fun deleteBooking(bookingId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Deleting booking: $bookingId")
            val result = bookingRepository.cancelBooking(bookingId)
            
            result.onSuccess { success ->
                if (success) {
                    // Remove from cache
                    removeBookingFromCache(bookingId)
                    Log.d(TAG, "Successfully deleted booking: $bookingId")
                } else {
                    Log.w(TAG, "Delete booking returned false for: $bookingId")
                }
            }.onFailure { exception ->
                Log.e(TAG, "Failed to delete booking: $bookingId", exception)
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception while deleting booking: $bookingId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update a booking and refresh cache
     */
    suspend fun updateBooking(bookingId: String, request: UpdateBookingRequest): Result<BookingDto> {
        return try {
            Log.d(TAG, "Updating booking: $bookingId")
            val result = bookingRepository.updateBooking(bookingId, request)
            
            result.onSuccess { updatedBooking ->
                // Update cache with the new booking data
                updateBookingInCache(updatedBooking)
                Log.d(TAG, "Successfully updated booking: $bookingId")
            }.onFailure { exception ->
                Log.e(TAG, "Failed to update booking: $bookingId", exception)
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception while updating booking: $bookingId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fetch charging station details for a list of bookings
     */
    private suspend fun fetchStationDetailsForBookings(bookings: List<BookingDto>): List<BookingDto> {
        return coroutineScope {
            val updatedBookings = bookings.map { booking ->
                async {
                    if (booking.chargingStationId != null && 
                        (booking.chargingStationName.isNullOrEmpty() || booking.chargingStationLocation.isNullOrEmpty())) {
                        
                        try {
                            val stationResult = chargingStationRepository.getStationById(booking.chargingStationId)
                            stationResult.onSuccess { station ->
                                Log.d(TAG, "Fetched station details for booking ${booking.id}: ${station.name}")
                                return@async booking.copy(
                                    chargingStationName = station.name,
                                    chargingStationLocation = station.location
                                )
                            }.onFailure { exception ->
                                Log.w(TAG, "Failed to fetch station details for ${booking.chargingStationId}", exception)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Exception fetching station details for ${booking.chargingStationId}", e)
                        }
                    }
                    booking
                }
            }
            updatedBookings.awaitAll()
        }
    }
}