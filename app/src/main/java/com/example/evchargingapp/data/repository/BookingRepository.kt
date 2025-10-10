package com.example.evchargingapp.data.repository

import android.util.Log
import com.example.evchargingapp.data.api.*
import retrofit2.Response

class BookingRepository(private val apiService: BookingApiService) {
    
    suspend fun createBooking(request: CreateBookingRequest): Result<BookingDto> {
        return try {
            Log.d("BookingRepository", "Creating booking for station: ${request.chargingStationId}")
            val response = apiService.createBooking(request)
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error creating booking", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateBooking(bookingId: String, request: UpdateBookingRequest): Result<BookingDto> {
        return try {
            Log.d("BookingRepository", "Updating booking: $bookingId")
            val response = apiService.updateBooking(bookingId, request)
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error updating booking", e)
            Result.failure(e)
        }
    }
    
    suspend fun cancelBooking(bookingId: String): Result<Boolean> {
        return try {
            Log.d("BookingRepository", "Cancelling booking: $bookingId")
            val response = apiService.cancelBooking(bookingId)
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error cancelling booking", e)
            Result.failure(e)
        }
    }
    
    suspend fun getBookingById(bookingId: String): Result<BookingDto> {
        return try {
            Log.d("BookingRepository", "Fetching booking: $bookingId")
            val response = apiService.getBookingById(bookingId)
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error fetching booking", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMyBookings(searchRequest: BookingSearchRequest? = null): Result<List<BookingDto>> {
        return try {
            Log.d("BookingRepository", "Fetching user bookings")
            val response = apiService.getMyBookings(
                status = searchRequest?.status,
                fromDate = searchRequest?.fromDate,
                toDate = searchRequest?.toDate,
                chargingStationId = searchRequest?.chargingStationId
            )
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error fetching user bookings", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAllMyBookings(): Result<List<BookingDto>> {
        return try {
            Log.d("BookingRepository", "Fetching all user bookings")
            // Call the API without status filter to get all bookings
            val response = apiService.getMyBookings()
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error fetching all user bookings", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAllBookings(searchRequest: BookingSearchRequest? = null): Result<List<BookingDto>> {
        return try {
            Log.d("BookingRepository", "Fetching all bookings")
            val response = apiService.getAllBookings(
                status = searchRequest?.status,
                fromDate = searchRequest?.fromDate,
                toDate = searchRequest?.toDate,
                chargingStationId = searchRequest?.chargingStationId
            )
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error fetching all bookings", e)
            Result.failure(e)
        }
    }
    
    suspend fun validateTimeSlot(
        chargingStationId: String,
        slotNumber: Int,
        reservationDateTime: String,
        duration: Int,
        excludeBookingId: String? = null
    ): Result<Boolean> {
        return try {
            Log.d("BookingRepository", "Validating time slot for station: $chargingStationId")
            val response = apiService.validateTimeSlot(chargingStationId, slotNumber, reservationDateTime, duration, excludeBookingId)
            handleApiResponse(response)
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error validating time slot", e)
            Result.failure(e)
        }
    }
    
    private fun <T> handleApiResponse(response: Response<ApiResponse<T>>): Result<T> {
        return if (response.isSuccessful) {
            val apiResponse = response.body()
            if (apiResponse?.success == true && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                val errorMessage = apiResponse?.message ?: "Unknown error occurred"
                Log.e("BookingRepository", "API error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } else {
            val errorMessage = "HTTP error: ${response.code()} ${response.message()}"
            Log.e("BookingRepository", errorMessage)
            Result.failure(Exception(errorMessage))
        }
    }
}