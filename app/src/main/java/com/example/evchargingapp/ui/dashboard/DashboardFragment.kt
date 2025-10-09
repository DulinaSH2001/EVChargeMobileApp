package com.example.evchargingapp.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.BookingApiService
import com.example.evchargingapp.data.api.BookingSearchRequest
import com.example.evchargingapp.data.api.ChargingStationApiService
import com.example.evchargingapp.data.repository.BookingRepository
import com.example.evchargingapp.data.repository.ChargingStationRepository
import com.example.evchargingapp.data.manager.BookingDataManager
import com.example.evchargingapp.utils.ApiConfig
import com.example.evchargingapp.utils.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class DashboardFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var bookingRepository: BookingRepository
    private lateinit var chargingStationRepository: ChargingStationRepository
    private lateinit var bookingDataManager: BookingDataManager
    private lateinit var tvPendingCount: TextView
    private lateinit var tvApprovedCount: TextView
    private lateinit var tvPastCount: TextView
    private lateinit var btnNewBooking: MaterialButton
    private lateinit var btnMyBookings: MaterialButton
    private lateinit var btnScanQr: MaterialButton
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = SessionManager(requireContext())
        
        // Initialize views
        tvPendingCount = view.findViewById(R.id.tv_pending_count)
        tvApprovedCount = view.findViewById(R.id.tv_approved_count)
        tvPastCount = view.findViewById(R.id.tv_past_count)
        btnNewBooking = view.findViewById(R.id.btn_new_booking)
        btnMyBookings = view.findViewById(R.id.btn_my_bookings)
        btnScanQr = view.findViewById(R.id.btn_scan_qr)
        
        // Setup UI based on user type
        setupUserInterface()
        
        // Initialize repository
        setupRepository()
        
        // Load statistics
        loadStatistics()
        
        // Setup click listeners
        setupClickListeners()
    }
    
    private fun setupRepository() {
        val authenticatedRetrofit = ApiConfig.getAuthenticatedRetrofit(requireContext())
        val apiService = authenticatedRetrofit.create(BookingApiService::class.java)
        val stationApiService = authenticatedRetrofit.create(ChargingStationApiService::class.java)
        bookingRepository = BookingRepository(apiService)
        chargingStationRepository = ChargingStationRepository(stationApiService)
        
        // Initialize data manager
        BookingDataManager.initialize(bookingRepository, chargingStationRepository)
        bookingDataManager = BookingDataManager.getInstance()
    }
    
    private fun setupUserInterface() {
        // Show/hide scan QR button for operators
        if (sessionManager.isOperator()) {
            btnScanQr.visibility = View.VISIBLE
        } else {
            btnScanQr.visibility = View.GONE
        }
    }
    
    private fun loadStatistics() {
        // Use BookingDataManager for efficient data loading with caching
        lifecycleScope.launch {
            try {
                // Observe bookings flow for reactive updates
                bookingDataManager.getAllBookings().collectLatest { allBookings ->
                    // Filter bookings by status locally using normalized status
                    val pendingBookings = allBookings.filter { getStatusText(it.status).equals("Pending", ignoreCase = true) }
                    val approvedBookings = allBookings.filter { getStatusText(it.status).equals("Approved", ignoreCase = true) }
                    val completedBookings = allBookings.filter { getStatusText(it.status).equals("Completed", ignoreCase = true) }
                    
                    // Update UI with counts
                    tvPendingCount.text = pendingBookings.size.toString()
                    tvApprovedCount.text = approvedBookings.size.toString()
                    tvPastCount.text = completedBookings.size.toString()
                    
                    Log.d(TAG, "Statistics loaded: ${pendingBookings.size} pending, ${approvedBookings.size} approved, ${completedBookings.size} completed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading statistics", e)
                // Set default values on error
                tvPendingCount.text = "0"
                tvApprovedCount.text = "0"
                tvPastCount.text = "0"
            }
        }
    }
    
    private fun setupClickListeners() {
        btnNewBooking.setOnClickListener {
            onNewBookingClick()
        }
        
        btnMyBookings.setOnClickListener {
            onMyBookingsClick()
        }
        
        btnScanQr.setOnClickListener {
            onScanQRClick()
        }
    }
    
    private fun onNewBookingClick() {
        (activity as? MainActivity)?.createNewBooking()
    }
    
    private fun onMyBookingsClick() {
        (activity as? MainActivity)?.navigateToBookings()
    }
    
    private fun onScanQRClick() {
        (activity as? MainActivity)?.scanQRCode()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        if (::bookingDataManager.isInitialized) {
            lifecycleScope.launch {
                bookingDataManager.refreshBookings()
            }
        }
    }
    
    private fun getStatusText(status: String): String {
        return when (status.trim().uppercase()) {
            // Handle string status values from API
            "PENDING" -> "Pending"
            "APPROVED" -> "Approved"
            "CANCELLED" -> "Cancelled"
            "COMPLETED" -> "Completed"
            // Handle numeric status values (backward compatibility)
            "0" -> "Pending"
            "1" -> "Approved"
            "2" -> "Cancelled"
            "3" -> "Completed"
            else -> status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
    }
    
    companion object {
        private const val TAG = "DashboardFragment"
        
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }
    }
}