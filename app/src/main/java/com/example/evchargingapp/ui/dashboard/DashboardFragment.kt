package com.example.evchargingapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.example.evchargingapp.R
import com.example.evchargingapp.utils.SessionManager

class DashboardFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
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
        
        // Load statistics
        loadStatistics()
        
        // Setup click listeners
        setupClickListeners()
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
        // TODO: Load actual statistics from database/API
        // For now, show placeholder data
        tvPendingCount.text = "2"
        tvApprovedCount.text = "5"
        tvPastCount.text = "12"
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
    
    companion object {
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }
    }
}