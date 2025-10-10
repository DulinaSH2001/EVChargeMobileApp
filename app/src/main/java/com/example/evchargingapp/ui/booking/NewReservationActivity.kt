package com.example.evchargingapp.ui.booking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.*
import com.example.evchargingapp.data.repository.BookingRepository
import com.example.evchargingapp.data.repository.ChargingStationRepository
import com.example.evchargingapp.data.manager.BookingDataManager
import com.example.evchargingapp.utils.ApiConfig
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NewReservationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NewReservationActivity"
        private const val EXTRA_STATION_ID = "extra_station_id"
        private const val EXTRA_STATION_NAME = "extra_station_name"
        private const val EXTRA_STATION_LOCATION = "extra_station_location"
        private const val EXTRA_AVAILABLE_SLOTS = "extra_available_slots"

        fun newIntent(
            context: Context,
            stationId: String,
            stationName: String,
            stationLocation: String,
            availableSlots: Int
        ): Intent {
            return Intent(context, NewReservationActivity::class.java).apply {
                putExtra(EXTRA_STATION_ID, stationId)
                putExtra(EXTRA_STATION_NAME, stationName)
                putExtra(EXTRA_STATION_LOCATION, stationLocation)
                putExtra(EXTRA_AVAILABLE_SLOTS, availableSlots)
            }
        }
    }

    private lateinit var bookingRepository: BookingRepository
    private lateinit var chargingStationRepository: ChargingStationRepository
    private lateinit var bookingDataManager: BookingDataManager

    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvStationName: TextView
    private lateinit var tvStationLocation: TextView
    private lateinit var btnSelectDate: MaterialCardView
    private lateinit var btnSelectTime: MaterialCardView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvSelectedTime: TextView
    private lateinit var spinnerDuration: Spinner
    private lateinit var spinnerSlotNumber: Spinner
    private lateinit var etNotes: TextInputEditText
    private lateinit var tilNotes: TextInputLayout
    private lateinit var tvValidationMessage: TextView
    private lateinit var btnCreateBooking: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var qrCodeCard: MaterialCardView
    private lateinit var ivQrCode: ImageView
    private lateinit var tvBookingId: TextView

    // Data
    private var stationId: String? = null
    private var stationName: String? = null
    private var stationLocation: String? = null
    private var availableSlots: Int = 0
    private var selectedDate: Calendar? = null
    private var selectedTime: Calendar? = null
    private var selectedDuration: Int = 60 // Default 60 minutes
    private var selectedSlotNumber: Int = 1
    
    // Validation state
    private var isValidatingTimeSlot = false
    private var isTimeSlotValid = false
    private var validationHandler = Handler(Looper.getMainLooper())
    private var validationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_reservation)

        extractIntentData()
        initializeComponents()
        setupViews()
        populateStationInfo()
    }

    private fun extractIntentData() {
        stationId = intent.getStringExtra(EXTRA_STATION_ID)
        stationName = intent.getStringExtra(EXTRA_STATION_NAME)
        stationLocation = intent.getStringExtra(EXTRA_STATION_LOCATION)
        availableSlots = intent.getIntExtra(EXTRA_AVAILABLE_SLOTS, 0)

        if (stationId == null) {
            Log.e(TAG, "No station ID provided")
            Toast.makeText(this, "Station information not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    private fun initializeComponents() {
        val authenticatedRetrofit = ApiConfig.getAuthenticatedRetrofit(this)
        val apiService = authenticatedRetrofit.create(BookingApiService::class.java)
        val stationApiService = authenticatedRetrofit.create(ChargingStationApiService::class.java)
        bookingRepository = BookingRepository(apiService)
        chargingStationRepository = ChargingStationRepository(stationApiService)
        
        // Initialize data manager
        BookingDataManager.initialize(bookingRepository, chargingStationRepository)
        bookingDataManager = BookingDataManager.getInstance()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        tvStationName = findViewById(R.id.tv_station_name)
        tvStationLocation = findViewById(R.id.tv_station_location)
        btnSelectDate = findViewById(R.id.btn_select_date)
        btnSelectTime = findViewById(R.id.btn_select_time)
        tvSelectedDate = findViewById(R.id.tv_selected_date)
        tvSelectedTime = findViewById(R.id.tv_selected_time)
        spinnerDuration = findViewById(R.id.spinner_duration)
        spinnerSlotNumber = findViewById(R.id.spinner_slot_number)
        etNotes = findViewById(R.id.et_notes)
        tilNotes = findViewById(R.id.til_notes)
        tvValidationMessage = findViewById(R.id.tv_validation_message)
        btnCreateBooking = findViewById(R.id.btn_create_booking)
        progressBar = findViewById(R.id.progress_bar)
        qrCodeCard = findViewById(R.id.qr_code_card)
        ivQrCode = findViewById(R.id.iv_qr_code)
        tvBookingId = findViewById(R.id.tv_booking_id)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Setup spinners
        setupDurationSpinner()
        setupSlotNumberSpinner()

        // Setup click listeners
        btnSelectDate.setOnClickListener { showDatePicker() }
        btnSelectTime.setOnClickListener { showTimePicker() }
        btnCreateBooking.setOnClickListener { createBooking() }

        // Initially hide QR code card and create booking button
        qrCodeCard.visibility = View.GONE
        btnCreateBooking.visibility = View.GONE
    }

    private fun populateStationInfo() {
        tvStationName.text = stationName
        tvStationLocation.text = stationLocation
    }

    private fun setupDurationSpinner() {
        val durations = arrayOf("30 minutes", "60 minutes", "90 minutes", "120 minutes", "180 minutes")
        val durationValues = arrayOf(30, 60, 90, 120, 180)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDuration.adapter = adapter

        // Set default to 60 minutes
        spinnerDuration.setSelection(1)

        spinnerDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDuration = durationValues[position]
                validateTimeSlotIfReady()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedDuration = 60
            }
        }
    }

    private fun setupSlotNumberSpinner() {
        val slotNumbers = Array(availableSlots) { "Slot ${it + 1}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, slotNumbers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSlotNumber.adapter = adapter

        spinnerSlotNumber.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSlotNumber = position + 1
                validateTimeSlotIfReady()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSlotNumber = 1
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                updateDateButton()
                validateTimeSlotIfReady()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                updateTimeButton()
                validateTimeSlotIfReady()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun updateDateButton() {
        selectedDate?.let { calendar ->
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            tvSelectedDate.text = dateFormat.format(calendar.time)
        }
    }

    private fun updateTimeButton() {
        selectedTime?.let { calendar ->
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvSelectedTime.text = timeFormat.format(calendar.time)
        }
    }

    private fun validateTimeSlotIfReady() {
        // Cancel any previous validation
        validationRunnable?.let { validationHandler.removeCallbacks(it) }
        
        // Check if all required fields are selected
        if (selectedDate != null && selectedTime != null && stationId != null) {
            
            // Check basic validation first
            val selectedDateTime = combineCalendar()
            val now = Calendar.getInstance()
            
            if (selectedDateTime.before(now)) {
                resetValidationState()
                btnCreateBooking.visibility = View.GONE
                tvValidationMessage.text = "✗ Please select a future date and time"
                tvValidationMessage.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
                return
            }
            
            // Check if booking is at least 30 minutes from now
            val minimumBookingTime = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 30)
            }
            
            if (selectedDateTime.before(minimumBookingTime)) {
                resetValidationState()
                btnCreateBooking.visibility = View.GONE
                tvValidationMessage.text = "✗ Bookings must be made at least 30 minutes in advance"
                tvValidationMessage.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
                return
            }
            
            // Debounce the validation to avoid too many API calls
            validationRunnable = Runnable {
                performTimeSlotValidation()
            }
            validationHandler.postDelayed(validationRunnable!!, 1000) // Wait 1 second before validating
        } else {
            resetValidationState()
            btnCreateBooking.visibility = View.GONE
        }
    }
    
    private fun performTimeSlotValidation() {
        if (isValidatingTimeSlot) {
            return // Already validating
        }
        
        isValidatingTimeSlot = true
        btnCreateBooking.isEnabled = false
        btnCreateBooking.text = "Validating..."
        
        // Update validation message
        tvValidationMessage.text = "Validating time slot availability..."
        tvValidationMessage.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, R.color.warning_orange)
        
        val reservationDateTime = combineDateTime()
        
        lifecycleScope.launch {
            try {
                val result = bookingRepository.validateTimeSlot(
                    chargingStationId = stationId!!,
                    slotNumber = selectedSlotNumber,
                    reservationDateTime = reservationDateTime,
                    duration = selectedDuration
                )
                
                result.onSuccess { isValid ->
                    isValidatingTimeSlot = false
                    isTimeSlotValid = isValid
                    
                    if (isValid) {
                        // Time slot is valid - show create booking button
                        tvValidationMessage.text = "✓ Time slot is available!"
                        tvValidationMessage.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this@NewReservationActivity, android.R.color.holo_green_dark)
                        
                        btnCreateBooking.visibility = View.VISIBLE
                        btnCreateBooking.isEnabled = true
                        btnCreateBooking.text = "Create Booking"
                        btnCreateBooking.setBackgroundColor(
                            androidx.core.content.ContextCompat.getColor(this@NewReservationActivity, R.color.primary_blue)
                        )
                    } else {
                        // Time slot is not available
                        showTimeSlotNotAvailable()
                    }
                }.onFailure { exception ->
                    Log.e(TAG, "Error validating time slot", exception)
                    isValidatingTimeSlot = false
                    // On validation error, allow user to proceed but warn them
                    tvValidationMessage.text = "⚠ Could not validate time slot. You can still try to create the booking."
                    tvValidationMessage.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this@NewReservationActivity, R.color.warning_orange)
                    
                    btnCreateBooking.visibility = View.VISIBLE
                    btnCreateBooking.isEnabled = true
                    btnCreateBooking.text = "Create Booking (Validation Failed)"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during validation", e)
                isValidatingTimeSlot = false
                
                tvValidationMessage.text = "⚠ Validation error. You can still try to create the booking."
                tvValidationMessage.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this@NewReservationActivity, R.color.warning_orange)
                
                btnCreateBooking.visibility = View.VISIBLE
                btnCreateBooking.isEnabled = true
                btnCreateBooking.text = "Create Booking"
            }
        }
    }
    
    private fun showTimeSlotNotAvailable() {
        btnCreateBooking.visibility = View.GONE
        
        tvValidationMessage.text = "✗ This time slot is not available. Please select a different date, time, duration, or slot."
        tvValidationMessage.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
        
        // Reset selections to allow user to choose again
        resetSelections()
    }
    
    private fun resetSelections() {
        // Reset UI to initial state
        selectedDate = null
        selectedTime = null
        tvSelectedDate.text = "Choose a date"
        tvSelectedTime.text = "Choose a time"
        
        // Reset spinners to default values
        spinnerDuration.setSelection(1) // 60 minutes
        spinnerSlotNumber.setSelection(0) // Slot 1
        
        resetValidationState()
        
        // Show a helpful message
        Toast.makeText(this, "Please select new date and time", Toast.LENGTH_SHORT).show()
    }
    
    private fun resetValidationState() {
        isValidatingTimeSlot = false
        isTimeSlotValid = false
        btnCreateBooking.text = "Create Booking"
        btnCreateBooking.setBackgroundColor(
            androidx.core.content.ContextCompat.getColor(this, R.color.primary_blue)
        )
        tvValidationMessage.text = "Select date, time, duration, and slot to validate availability"
        tvValidationMessage.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, R.color.primary_blue)
    }

    private fun createBooking() {
        if (!validateInputs()) {
            return
        }

        // If we have already validated and it's valid, proceed directly
        if (isTimeSlotValid) {
            val reservationDateTime = combineDateTime()
            createBookingRequest(reservationDateTime)
        } else {
            // Re-validate one more time before creating booking
            val reservationDateTime = combineDateTime()
            
            showLoading(true)
            
            lifecycleScope.launch {
                try {
                    val isSlotValid = bookingRepository.validateTimeSlot(
                        chargingStationId = stationId!!,
                        slotNumber = selectedSlotNumber,
                        reservationDateTime = reservationDateTime,
                        duration = selectedDuration
                    )
                    
                    isSlotValid.onSuccess { isValid ->
                        if (isValid) {
                            // Time slot is valid, proceed with booking creation
                            createBookingRequest(reservationDateTime)
                        } else {
                            showLoading(false)
                            showTimeSlotNotAvailable()
                        }
                    }.onFailure { exception ->
                        Log.w(TAG, "Could not validate time slot, proceeding anyway", exception)
                        // If validation fails, we'll still try to create the booking
                        // The server will reject it if there's a conflict
                        createBookingRequest(reservationDateTime)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error during slot validation, proceeding with booking", e)
                    createBookingRequest(reservationDateTime)
                }
            }
        }
    }
    
    private fun createBookingRequest(reservationDateTime: String) {
        val request = CreateBookingRequest(
            chargingStationId = stationId!!,
            slotNumber = selectedSlotNumber,
            reservationDateTime = reservationDateTime,
            duration = selectedDuration,
            notes = etNotes.text.toString().trim().takeIf { it.isNotEmpty() }
        )

        lifecycleScope.launch {
            try {
                bookingRepository.createBooking(request)
                    .onSuccess { booking ->
                        Log.d(TAG, "Booking created successfully: ${booking.id}")
                        
                        // Refresh cache to include the new booking
                        bookingDataManager.refreshBookings()
                        
                        showBookingSuccess(booking)
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Failed to create booking", exception)
                        showLoading(false)
                        
                        val errorMessage = when {
                            exception.message?.contains("time slot", ignoreCase = true) == true -> 
                                "The selected time slot is no longer available. Please choose a different time."
                            exception.message?.contains("station", ignoreCase = true) == true -> 
                                "There's an issue with the selected station. Please try again or choose another station."
                            exception.message?.contains("unauthorized", ignoreCase = true) == true -> 
                                "Session expired. Please log in again."
                            else -> "Failed to create booking: ${exception.message}"
                        }
                        
                        Toast.makeText(this@NewReservationActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while creating booking", e)
                showLoading(false)
                Toast.makeText(
                    this@NewReservationActivity,
                    "Error creating booking. Please check your connection and try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            btnSelectDate.requestFocus()
            return false
        }

        if (selectedTime == null) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
            btnSelectTime.requestFocus()
            return false
        }

        // Check if selected date/time is in the future
        val selectedDateTime = combineCalendar()
        val now = Calendar.getInstance()
        
        if (selectedDateTime.before(now)) {
            Toast.makeText(this, "Please select a future date and time", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Check if booking is too far in the future (e.g., more than 30 days)
        val maxAdvanceBooking = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 30)
        }
        
        if (selectedDateTime.after(maxAdvanceBooking)) {
            Toast.makeText(this, "Bookings can only be made up to 30 days in advance", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Check if booking is at least 30 minutes from now
        val minimumBookingTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 30)
        }
        
        if (selectedDateTime.before(minimumBookingTime)) {
            Toast.makeText(this, "Bookings must be made at least 30 minutes in advance", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Validate selected slot number
        if (selectedSlotNumber < 1 || selectedSlotNumber > availableSlots) {
            Toast.makeText(this, "Invalid slot selection", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Validate duration
        if (selectedDuration < 30 || selectedDuration > 480) { // 30 minutes to 8 hours
            Toast.makeText(this, "Duration must be between 30 minutes and 8 hours", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun combineDateTime(): String {
        val combinedCalendar = combineCalendar()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        return isoFormat.format(combinedCalendar.time)
    }

    private fun combineCalendar(): Calendar {
        val combined = Calendar.getInstance()
        selectedDate?.let { date ->
            combined.set(Calendar.YEAR, date.get(Calendar.YEAR))
            combined.set(Calendar.MONTH, date.get(Calendar.MONTH))
            combined.set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
        }
        selectedTime?.let { time ->
            combined.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY))
            combined.set(Calendar.MINUTE, time.get(Calendar.MINUTE))
            combined.set(Calendar.SECOND, 0)
            combined.set(Calendar.MILLISECOND, 0)
        }
        return combined
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnCreateBooking.isEnabled = !loading
    }

    private fun showBookingSuccess(booking: BookingDto) {
        showLoading(false)
        
        // Generate QR code with comprehensive booking data
        val qrCodeData = booking.qrCode ?: generateBookingQRData(booking)
        generateQRCode(qrCodeData)

        // Update UI to show success with detailed information
        tvBookingId.text = buildString {
            append("Booking ID: ${booking.id}\n")
            append("Status: ${booking.status}\n")
            append("Station: $stationName\n")
            append("Slot: ${booking.slotNumber}\n")
            
            // Format the reservation date/time for display
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = isoFormat.parse(booking.reservationDateTime)
                
                val displayFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                append("Date & Time: ${displayFormat.format(date)}\n")
            } catch (e: Exception) {
                append("Date & Time: ${booking.reservationDateTime}\n")
            }
            
            append("Duration: ${booking.duration} minutes")
            
            if (!booking.notes.isNullOrEmpty()) {
                append("\nNotes: ${booking.notes}")
            }
        }
        
        qrCodeCard.visibility = View.VISIBLE
        btnCreateBooking.text = "Booking Created Successfully ✓"
        btnCreateBooking.isEnabled = false
        btnCreateBooking.setBackgroundColor(
            androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark)
        )

        // Show success message
        Toast.makeText(this, "Booking created successfully!", Toast.LENGTH_LONG).show()
        
        // Optional: Set result to notify parent activity
        setResult(RESULT_OK, Intent().apply {
            putExtra("booking_id", booking.id)
            putExtra("booking_status", booking.status)
        })
    }
    
    private fun generateBookingQRData(booking: BookingDto): String {
        // Create a structured QR code data containing booking information
        return buildString {
            append("BOOKING_ID:${booking.id}")
            append("|STATION_ID:${booking.chargingStationId}")
            append("|SLOT:${booking.slotNumber}")
            append("|DATE_TIME:${booking.reservationDateTime}")
            append("|DURATION:${booking.duration}")
            append("|STATUS:${booking.status}")
        }
    }

    private fun generateQRCode(data: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400)
            ivQrCode.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            Log.e(TAG, "Error generating QR code", e)
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show()
        }
    }
}