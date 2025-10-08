package com.example.evchargingapp.ui.operator

import android.app.Activity
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
import com.example.evchargingapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class BookingDetailsActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var operatorRepository: OperatorRepository
    
    // UI Components
    private lateinit var tvBookingId: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerContact: TextView
    private lateinit var tvVehicleNumber: TextView
    private lateinit var tvBookingDate: TextView
    private lateinit var tvTimeSlot: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnConfirm: MaterialButton
    private lateinit var btnFinalize: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: ProgressBar
    
    // Finalize inputs
    private lateinit var cardFinalizeInputs: CardView
    private lateinit var etEnergyConsumed: TextInputEditText
    private lateinit var etTotalCost: TextInputEditText
    
    private var bookingDetails: BookingDetails? = null
    private var isSessionActive = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_details)
        
        sessionManager = SessionManager(this)
        operatorRepository = OperatorRepository(this)
        
        initViews()
        setupClickListeners()
        loadBookingData()
    }
    
    private fun initViews() {
        tvBookingId = findViewById(R.id.tv_booking_id)
        tvCustomerName = findViewById(R.id.tv_customer_name)
        tvCustomerContact = findViewById(R.id.tv_customer_contact)
        tvVehicleNumber = findViewById(R.id.tv_vehicle_number)
        tvBookingDate = findViewById(R.id.tv_booking_date)
        tvTimeSlot = findViewById(R.id.tv_time_slot)
        tvStatus = findViewById(R.id.tv_status)
        btnConfirm = findViewById(R.id.btn_confirm)
        btnFinalize = findViewById(R.id.btn_finalize)
        btnCancel = findViewById(R.id.btn_cancel)
        progressBar = findViewById(R.id.progress_bar)
        cardFinalizeInputs = findViewById(R.id.card_finalize_inputs)
        etEnergyConsumed = findViewById(R.id.et_energy_consumed)
        etTotalCost = findViewById(R.id.et_total_cost)
    }
    
    private fun setupClickListeners() {
        btnConfirm.setOnClickListener {
            confirmBooking()
        }
        
        btnFinalize.setOnClickListener {
            finalizeBooking()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun loadBookingData() {
        // Get booking data from intent
        bookingDetails = intent.getSerializableExtra("booking_data") as? BookingDetails
        
        bookingDetails?.let { booking ->
            populateBookingDetails(booking)
            updateUIBasedOnStatus(booking.status)
        } ?: run {
            showToast("Error loading booking details")
            finish()
        }
    }
    
    private fun populateBookingDetails(booking: BookingDetails) {
        tvBookingId.text = "Booking ID: ${booking.id}"
        tvCustomerName.text = booking.evOwnerName
        tvCustomerContact.text = booking.contactNumber ?: "Not provided"
        tvVehicleNumber.text = booking.vehicleNumber ?: "Not provided"
        tvBookingDate.text = booking.bookingDate
        tvTimeSlot.text = "${booking.startTime} - ${booking.endTime}"
        tvStatus.text = booking.status.uppercase()
        
        // Update status text color based on status
        when (booking.status.uppercase()) {
            "CONFIRMED" -> tvStatus.setTextColor(getColor(R.color.success_color))
            "PENDING" -> tvStatus.setTextColor(getColor(R.color.warning_color))
            "COMPLETED" -> tvStatus.setTextColor(getColor(R.color.primary_color))
            "CANCELLED" -> tvStatus.setTextColor(getColor(R.color.error_color))
            else -> tvStatus.setTextColor(getColor(R.color.text_secondary))
        }
    }
    
    private fun updateUIBasedOnStatus(status: String) {
        when (status.uppercase()) {
            "PENDING" -> {
                btnConfirm.visibility = View.VISIBLE
                btnFinalize.visibility = View.GONE
                cardFinalizeInputs.visibility = View.GONE
                isSessionActive = false
            }
            "CONFIRMED", "IN_PROGRESS" -> {
                btnConfirm.visibility = View.GONE
                btnFinalize.visibility = View.VISIBLE
                cardFinalizeInputs.visibility = View.VISIBLE
                isSessionActive = true
            }
            "COMPLETED", "CANCELLED" -> {
                btnConfirm.visibility = View.GONE
                btnFinalize.visibility = View.GONE
                cardFinalizeInputs.visibility = View.GONE
                isSessionActive = false
            }
            else -> {
                btnConfirm.visibility = View.VISIBLE
                btnFinalize.visibility = View.GONE
                cardFinalizeInputs.visibility = View.GONE
                isSessionActive = false
            }
        }
    }
    
    private fun confirmBooking() {
        val booking = bookingDetails ?: return
        val operatorId = sessionManager.getCurrentUser()?.id ?: return
        
        setLoadingState(true)
        lifecycleScope.launch {
            when (val result = operatorRepository.confirmBooking(booking.id, operatorId)) {
                is OperatorResult.SessionResult -> {
                    showToast("Booking confirmed successfully!")
                    
                    // Update booking status and UI
                    bookingDetails = booking.copy(status = "CONFIRMED")
                    populateBookingDetails(bookingDetails!!)
                    updateUIBasedOnStatus("CONFIRMED")
                    
                    // Set result for parent activity
                    setResult(Activity.RESULT_OK)
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
    
    private fun finalizeBooking() {
        val booking = bookingDetails ?: return
        val operatorId = sessionManager.getCurrentUser()?.id ?: return
        
        // Get input values
        val energyText = etEnergyConsumed.text.toString().trim()
        val costText = etTotalCost.text.toString().trim()
        
        val energyConsumed = if (energyText.isNotEmpty()) {
            try {
                energyText.toDouble()
            } catch (e: NumberFormatException) {
                showToast("Please enter a valid energy amount")
                return
            }
        } else null
        
        val totalCost = if (costText.isNotEmpty()) {
            try {
                costText.toDouble()
            } catch (e: NumberFormatException) {
                showToast("Please enter a valid cost amount")
                return
            }
        } else null
        
        setLoadingState(true)
        lifecycleScope.launch {
            when (val result = operatorRepository.finalizeBooking(booking.id, operatorId, energyConsumed, totalCost)) {
                is OperatorResult.SessionResult -> {
                    showToast("Booking finalized successfully!")
                    
                    // Update booking status and UI
                    bookingDetails = booking.copy(status = "COMPLETED")
                    populateBookingDetails(bookingDetails!!)
                    updateUIBasedOnStatus("COMPLETED")
                    
                    // Set result for parent activity
                    setResult(Activity.RESULT_OK)
                    
                    // Delay and close activity
                    android.os.Handler(mainLooper).postDelayed({
                        finish()
                    }, 2000)
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
    
    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnConfirm.isEnabled = !isLoading
        btnFinalize.isEnabled = !isLoading
        btnCancel.isEnabled = !isLoading
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}