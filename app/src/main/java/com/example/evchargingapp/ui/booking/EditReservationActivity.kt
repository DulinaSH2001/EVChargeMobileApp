package com.example.evchargingapp.ui.booking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.*
import com.example.evchargingapp.data.repository.BookingRepository
import com.example.evchargingapp.data.repository.ChargingStationRepository
import com.example.evchargingapp.data.manager.BookingDataManager
import com.example.evchargingapp.utils.ApiConfig
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditReservationActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val TAG = "EditReservationActivity"
        private const val EXTRA_BOOKING_ID = "booking_id"
        private const val EXTRA_BOOKING_DATA = "booking_data"
    }

    private lateinit var bookingRepository: BookingRepository
    private lateinit var chargingStationRepository: ChargingStationRepository
    private lateinit var bookingDataManager: BookingDataManager

    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var ivQrCode: ImageView
    private lateinit var tvStationName: TextView
    private lateinit var tvStationLocation: TextView
    private lateinit var tvBookingId: TextView
    private lateinit var btnViewMap: MaterialButton
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var btnSelectDate: MaterialButton
    private lateinit var btnSelectTime: MaterialButton
    private lateinit var spinnerDuration: MaterialAutoCompleteTextView
    private lateinit var spinnerSlotNumber: MaterialAutoCompleteTextView
    private lateinit var etNotes: TextInputEditText
    private lateinit var tilNotes: TextInputLayout
    private lateinit var btnUpdateBooking: MaterialButton
    private lateinit var progressBar: ProgressBar

    // Maps
    private var googleMap: GoogleMap? = null
    private var stationLatLng: LatLng? = null

    // Data
    private var bookingId: String? = null
    private var booking: BookingDto? = null
    private var chargingStation: ChargingStationDto? = null
    private var selectedDate: Calendar? = null
    private var selectedTime: Calendar? = null
    private var selectedDuration: Int = 60
    private var selectedSlotNumber: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_reservation)

        extractIntentData()
        initializeComponents()
        setupViews()
        populateFields()
    }

    private fun extractIntentData() {
        bookingId = intent.getStringExtra(EXTRA_BOOKING_ID)
        booking = intent.getSerializableExtra(EXTRA_BOOKING_DATA) as? BookingDto

        if (bookingId == null || booking == null) {
            Log.e(TAG, "No booking data provided")
            Toast.makeText(this, "Booking information not found", Toast.LENGTH_SHORT).show()
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
        ivQrCode = findViewById(R.id.iv_qr_code)
        tvStationName = findViewById(R.id.tv_station_name)
        tvStationLocation = findViewById(R.id.tv_station_location)
        tvBookingId = findViewById(R.id.tv_booking_id)
        btnViewMap = findViewById(R.id.btn_view_map)
        btnSelectDate = findViewById(R.id.btn_select_date)
        btnSelectTime = findViewById(R.id.btn_select_time)
        spinnerDuration = findViewById(R.id.spinner_duration)
        spinnerSlotNumber = findViewById(R.id.spinner_slot)
        etNotes = findViewById(R.id.et_notes)
        tilNotes = findViewById(R.id.til_notes)
        btnUpdateBooking = findViewById(R.id.btn_update_booking)
        progressBar = findViewById(R.id.progress_bar)

        // Setup Map Fragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        setupClickListeners()
        setupSpinners()
        generateQRCode()
    }

    private fun setupClickListeners() {
        btnSelectDate.setOnClickListener { showDatePicker() }
        btnSelectTime.setOnClickListener { showTimePicker() }
        btnUpdateBooking.setOnClickListener { updateBooking() }
        btnViewMap.setOnClickListener { 
            // Toggle map visibility
            if (mapFragment.view?.visibility == View.VISIBLE) {
                mapFragment.view?.visibility = View.GONE
                btnViewMap.text = "Show Map"
            } else {
                mapFragment.view?.visibility = View.VISIBLE
                btnViewMap.text = "Hide Map"
                // Only update map if we have station data and map is ready
                chargingStation?.let { station ->
                    googleMap?.let { map ->
                        updateMapWithStationCoordinates(station)
                    }
                }
            }
        }
    }

    private fun setupSpinners() {
        setupDurationSpinner()
        setupSlotNumberSpinner()
    }

    private fun generateQRCode() {
        booking?.let { booking ->
            // Generate QR code with only booking ID as requested
            val qrContent = booking.id
            
            try {
                val writer = MultiFormatWriter()
                val bitMatrix: BitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
                
                ivQrCode.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating QR code", e)
                ivQrCode.visibility = View.GONE
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        
        // Setup map UI and settings
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.uiSettings.isCompassEnabled = true
        
        // Set map type to normal
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        
        // Initially hide the map
        mapFragment.view?.visibility = View.GONE
        btnViewMap.text = "Show Map"
        
        // If we already have charging station data, update the map
        chargingStation?.let { station ->
            updateMapWithStationCoordinates(station)
        }
    }

    private fun parseLocationCoordinates(location: String): Pair<Double, Double>? {
        return try {
            // Parse coordinates from location string like "6.9319, 79.8478, Colombo 07, Sri Lanka"
            val parts = location.split(",")
            if (parts.size >= 2) {
                val lat = parts[0].trim().toDouble()
                val lng = parts[1].trim().toDouble()
                Pair(lat, lng)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing coordinates: ${e.message}")
            null
        }
    }

    private fun populateFields() {
        booking?.let { booking ->
            tvStationName.text = booking.chargingStationName ?: "Unknown Station"
            tvStationLocation.text = booking.getParsedAddress()
            tvBookingId.text = "Booking ID: ${booking.id}"

            // Parse and populate date/time
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(booking.reservationDateTime)
                
                if (date != null) {
                    selectedDate = Calendar.getInstance().apply { time = date }
                    selectedTime = Calendar.getInstance().apply { time = date }
                    updateDateButton()
                    updateTimeButton()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date/time", e)
            }

            // Set duration
            selectedDuration = booking.duration
            setDurationSpinnerSelection(booking.duration)

            // Set slot number
            selectedSlotNumber = booking.slotNumber
            setSlotSpinnerSelection(booking.slotNumber)

            // Set notes
            etNotes.setText(booking.notes ?: "")
            
            // Fetch charging station details for proper coordinates and slot count
            fetchChargingStationDetails(booking.chargingStationId)
        }
    }
    
    private fun fetchChargingStationDetails(stationId: String) {
        Log.d(TAG, "Fetching charging station details for ID: $stationId")
        lifecycleScope.launch {
            try {
                val result = chargingStationRepository.getStationById(stationId)
                result.onSuccess { station: ChargingStationDto ->
                    Log.d(TAG, "Successfully fetched station: ${station.name}")
                    Log.d(TAG, "Station location: ${station.location}")
                    Log.d(TAG, "Station coordinates - lat: ${station.getCoordinateLatitude()}, lng: ${station.getCoordinateLongitude()}")
                    
                    chargingStation = station
                    
                    // Update location text with clean address from station data
                    tvStationLocation.text = station.getParsedAddress()
                    
                    // Update slot spinner with correct slot count
                    setupSlotNumberSpinnerWithStationData(station.totalSlots)
                    
                    // Update map with proper coordinates
                    updateMapWithStationCoordinates(station)
                    
                }.onFailure { exception: Throwable ->
                    Log.e(TAG, "Failed to fetch charging station details", exception)
                    // Continue with basic setup if station fetch fails
                    setupSlotNumberSpinner()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while fetching station details", e)
                setupSlotNumberSpinner()
            }
        }
    }

    private fun setupDurationSpinner() {
        val durations = arrayOf("30 minutes", "60 minutes", "90 minutes", "120 minutes", "180 minutes")
        val durationValues = arrayOf(30, 60, 90, 120, 180)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, durations)
        spinnerDuration.setAdapter(adapter)
        
        spinnerDuration.setOnItemClickListener { _, _, position, _ ->
            selectedDuration = durationValues[position]
        }
    }

    private fun setupSlotNumberSpinner() {
        // Default fallback - allow selecting from slots 1-10
        val slotNumbers = Array(10) { "Slot ${it + 1}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, slotNumbers)
        spinnerSlotNumber.setAdapter(adapter)

        spinnerSlotNumber.setOnItemClickListener { _, _, position, _ ->
            selectedSlotNumber = position + 1
        }
    }
    
    private fun setupSlotNumberSpinnerWithStationData(totalSlots: Int) {
        // Use actual slot count from charging station
        val slotNumbers = Array(totalSlots) { "Slot ${it + 1}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, slotNumbers)
        spinnerSlotNumber.setAdapter(adapter)

        spinnerSlotNumber.setOnItemClickListener { _, _, position, _ ->
            selectedSlotNumber = position + 1
        }
        
        // Re-set the current selection if it's still valid
        if (selectedSlotNumber <= totalSlots) {
            setSlotSpinnerSelection(selectedSlotNumber)
        }
    }
    
    private fun updateMapWithStationCoordinates(station: ChargingStationDto) {
        googleMap?.let { map ->
            Log.d(TAG, "Updating map for station: ${station.name}")
            
            // Clear existing markers
            map.clear()
            
            val lat = station.getCoordinateLatitude()
            val lng = station.getCoordinateLongitude()
            
            if (lat != null && lng != null) {
                Log.d(TAG, "Setting map position to: $lat, $lng")
                val position = LatLng(lat, lng)
                stationLatLng = position
                
                // Add marker with custom styling
                map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(station.name)
                        .snippet("${station.getParsedAddress()} • ${station.type}")
                )
                
                // Animate camera to station location with proper zoom
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(position, 16f), 
                    1500, // 1.5 seconds animation
                    object : GoogleMap.CancelableCallback {
                        override fun onFinish() {
                            Log.d(TAG, "Map camera animation completed")
                        }
                        override fun onCancel() {
                            Log.d(TAG, "Map camera animation cancelled")
                        }
                    }
                )
            } else {
                Log.w(TAG, "Could not parse coordinates from location: ${station.location}")
                Toast.makeText(this, "Location coordinates not available", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.w(TAG, "Google Map not initialized yet")
        }
    }

    private fun setDurationSpinnerSelection(duration: Int) {
        val durationValues = arrayOf(30, 60, 90, 120, 180)
        val durations = arrayOf("30 minutes", "60 minutes", "90 minutes", "120 minutes", "180 minutes")
        val index = durationValues.indexOf(duration)
        if (index >= 0) {
            spinnerDuration.setText(durations[index], false)
        }
    }

    private fun setSlotSpinnerSelection(slotNumber: Int) {
        // Check if the slot number is valid for current station
        val maxSlots = chargingStation?.totalSlots ?: 10
        if (slotNumber <= maxSlots) {
            spinnerSlotNumber.setText("Slot $slotNumber", false)
        }
    }

    private fun showDatePicker() {
        val calendar = selectedDate ?: Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                updateDateButton()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = Calendar.getInstance().timeInMillis
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = selectedTime ?: Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                updateTimeButton()
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
            btnSelectDate.text = dateFormat.format(calendar.time)
        }
    }

    private fun updateTimeButton() {
        selectedTime?.let { calendar ->
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            btnSelectTime.text = timeFormat.format(calendar.time)
        }
    }

    private fun updateBooking() {
        if (!validateInputs()) {
            return
        }

        val reservationDateTime = combineDateTime()
        val request = UpdateBookingRequest(
            slotNumber = selectedSlotNumber,
            reservationDateTime = reservationDateTime,
            duration = selectedDuration,
            notes = etNotes.text.toString().trim().takeIf { it.isNotEmpty() }
        )

        showLoading(true)

        lifecycleScope.launch {
            try {
                bookingDataManager.updateBooking(bookingId!!, request)
                    .onSuccess { updatedBooking ->
                        Log.d(TAG, "Booking updated successfully: ${updatedBooking.id}")
                        showLoading(false)
                        Toast.makeText(
                            this@EditReservationActivity,
                            "Booking updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Return to booking list
                        setResult(RESULT_OK)
                        finish()
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Failed to update booking", exception)
                        showLoading(false)
                        Toast.makeText(
                            this@EditReservationActivity,
                            "Failed to update booking: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while updating booking", e)
                showLoading(false)
                Toast.makeText(
                    this@EditReservationActivity,
                    "Error updating booking",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedTime == null) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if selected date/time is in the future
        val selectedDateTime = combineCalendar()
        val now = Calendar.getInstance()
        if (selectedDateTime.before(now)) {
            Toast.makeText(this, "Please select a future date and time", Toast.LENGTH_SHORT).show()
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
        btnUpdateBooking.isEnabled = !loading
    }
}