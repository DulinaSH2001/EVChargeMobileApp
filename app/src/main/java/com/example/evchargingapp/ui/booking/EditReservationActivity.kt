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
                // Refresh map if needed
                mapFragment.getMapAsync(this)
            }
        }
    }

    private fun setupSpinners() {
        setupDurationSpinner()
        setupSlotNumberSpinner()
    }

    private fun generateQRCode() {
        booking?.let { booking ->
            val qrContent = "Booking ID: ${booking.id}\n" +
                    "Station: ${booking.chargingStationName}\n" +
                    "Date: ${booking.reservationDateTime}\n" +
                    "Duration: ${booking.duration} minutes\n" +
                    "Slot: ${booking.slotNumber}"
            
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
        
        booking?.let { booking ->
            val coordinates = parseLocationCoordinates(booking.chargingStationLocation ?: "")
            coordinates?.let { (lat, lng) ->
                val location = LatLng(lat, lng)
                stationLatLng = location
                
                googleMap.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title(booking.chargingStationName ?: "Charging Station")
                        .snippet(booking.getParsedAddress())
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                
                // Initially hide the map
                mapFragment.view?.visibility = View.GONE
                btnViewMap.text = "Show Map"
            }
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
            tvStationLocation.text = booking.chargingStationLocation ?: "Location not available"
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
        // For editing, we'll allow selecting from slots 1-10 (you can adjust this based on station info)
        val slotNumbers = Array(10) { "Slot ${it + 1}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, slotNumbers)
        spinnerSlotNumber.setAdapter(adapter)

        spinnerSlotNumber.setOnItemClickListener { _, _, position, _ ->
            selectedSlotNumber = position + 1
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
        if (slotNumber <= 10) {
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