package com.example.evchargingapp.ui.operator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.*
import com.example.evchargingapp.data.repository.OperatorRepository
import com.example.evchargingapp.ui.auth.LoginActivity
import com.example.evchargingapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import kotlinx.coroutines.launch

class OperatorDashboardActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var operatorRepository: OperatorRepository
    
    // UI Components
    private lateinit var tvWelcome: TextView
    private lateinit var tvOperatorEmail: TextView
    private lateinit var btnScanQR: MaterialButton
    private lateinit var btnProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var progressBar: ProgressBar
    
    // Current session data
    private var currentBooking: BookingDetails? = null
    private var currentSession: OperatorSessionResponse? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_dashboard)
        
        sessionManager = SessionManager(this)
        operatorRepository = OperatorRepository(this)
        
        // Check if user is logged in and is an operator
        if (!sessionManager.isLoggedIn() || sessionManager.getCurrentUser()?.isStationOperator() != true) {
            redirectToLogin()
            return
        }
        
        initViews()
        setupUI()
        setupClickListeners()
    }
    
    private fun initViews() {
        tvWelcome = findViewById(R.id.tv_welcome)
        tvOperatorEmail = findViewById(R.id.tv_operator_email)
        btnScanQR = findViewById(R.id.btn_scan_qr)
        btnProfile = findViewById(R.id.btn_profile)
        btnLogout = findViewById(R.id.btn_logout)
        progressBar = findViewById(R.id.progress_bar)
    }
    
    private fun setupUI() {
        val currentUser = sessionManager.getCurrentUser() ?: return
        tvWelcome.text = "Welcome, ${currentUser.name.ifEmpty { "Operator" }}"
        tvOperatorEmail.text = currentUser.email
    }
    
    private fun setupClickListeners() {
        btnScanQR.setOnClickListener {
            startQRScanner()
        }
        
        btnProfile.setOnClickListener {
            openProfile()
        }
        
        btnLogout.setOnClickListener {
            performLogout()
        }
    }
    
    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan booking QR code")
        integrator.setCameraId(0) // Use back camera
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                showToast("Scan cancelled")
            } else {
                val qrCode = result.contents
                showToast("Scanned: $qrCode")
                processQRCode(qrCode)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
    
    private fun processQRCode(qrCode: String) {
        setLoadingState(true)
        lifecycleScope.launch {
            when (val result = operatorRepository.getBookingDetails(qrCode)) {
                is OperatorResult.BookingFound -> {
                    currentBooking = result.booking
                    showBookingDetails(result.booking)
                }
                is OperatorResult.Failure -> {
                    showToast("Error: ${result.error}")
                }
                else -> {
                    showToast("Unexpected result")
                }
            }
            setLoadingState(false)
        }
    }
    
    private fun showBookingDetails(booking: BookingDetails) {
        val intent = Intent(this, BookingDetailsActivity::class.java)
        intent.putExtra("booking_id", booking.id)
        intent.putExtra("booking_data", booking)
        startActivityForResult(intent, REQUEST_BOOKING_DETAILS)
    }
    
    private fun openProfile() {
        val intent = Intent(this, OperatorProfileActivity::class.java)
        startActivity(intent)
    }
    
    private fun performLogout() {
        sessionManager.logout()
        redirectToLogin()
    }
    
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnScanQR.isEnabled = !isLoading
        btnProfile.isEnabled = !isLoading
        btnLogout.isEnabled = !isLoading
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    companion object {
        private const val REQUEST_BOOKING_DETAILS = 1001
    }
}