package com.example.evchargingapp.data.api

import retrofit2.Response
import retrofit2.http.*

interface BookingApiService {
    
    @POST("Booking")
    suspend fun createBooking(
        @Body request: CreateBookingRequest
    ): Response<ApiResponse<BookingDto>>
    
    @PUT("Booking/{id}")
    suspend fun updateBooking(
        @Path("id") bookingId: String,
        @Body request: UpdateBookingRequest
    ): Response<ApiResponse<BookingDto>>
    
    @DELETE("Booking/{id}")
    suspend fun cancelBooking(
        @Path("id") bookingId: String
    ): Response<ApiResponse<Boolean>>
    
    @GET("Booking/{id}")
    suspend fun getBookingById(
        @Path("id") bookingId: String
    ): Response<ApiResponse<BookingDto>>
    
    @GET("Booking/my-bookings")
    suspend fun getMyBookings(
        @Query("status") status: String? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null,
        @Query("chargingStationId") chargingStationId: String? = null
    ): Response<ApiResponse<List<BookingDto>>>
    
    @GET("api/Booking")
    suspend fun getAllBookings(
        @Query("status") status: String? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null,
        @Query("chargingStationId") chargingStationId: String? = null
    ): Response<ApiResponse<List<BookingDto>>>
    
    @GET("Booking/validate-time-slot")
    suspend fun validateTimeSlot(
        @Query("chargingStationId") chargingStationId: String,
        @Query("slotNumber") slotNumber: Int,
        @Query("reservationDateTime") reservationDateTime: String,
        @Query("duration") duration: Int
    ): Response<ApiResponse<Boolean>>
    
    // Operator booking status update endpoint
    @PATCH("Booking/{id}/status")
    suspend fun updateBookingStatus(
        @Path("id") bookingId: String,
        @Body request: BookingStatusUpdateRequest
    ): Response<ApiResponse<BookingDto>>
}