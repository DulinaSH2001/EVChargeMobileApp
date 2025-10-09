package com.example.evchargingapp.ui.booking

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val TAG = "BookingDetailsActivity"
        private const val EXTRA_BOOKING_ID = "booking_id"
        private const val EXTRA_BOOKING_DATA = "booking_data"

        fun start(context: Context, bookingId: String) {
            val intent = Intent(context, BookingDetailsActivity::class.java)
            intent.putExtra(EXTRA_BOOKING_ID, bookingId)
            context.startActivity(intent)
        }

        fun start(context: Context, booking: BookingDto) {
            val intent = Intent(context, BookingDetailsActivity::class.java)
            intent.putExtra(EXTRA_BOOKING_DATA, booking)
            context.startActivity(intent)
        }
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
    private lateinit var tvBookingDate: TextView
    private lateinit var tvBookingTime: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvSlotNumber: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvNotes: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var qrCodeCard: CardView

    // Maps
    private var googleMap: GoogleMap? = null
    private var stationLatLng: LatLng? = null

    // Data
    private var bookingId: String? = null
    private var booking: BookingDto? = null
    private var chargingStation: ChargingStationDto? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_details_new)

        extractIntentData()
        initializeComponents()
        setupViews()
        populateFields()
    }

    private fun extractIntentData() {
        bookingId = intent.getStringExtra(EXTRA_BOOKING_ID)
        booking = intent.getSerializableExtra(EXTRA_BOOKING_DATA) as? BookingDto

        if (bookingId == null && booking == null) {
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
        // Initialize UI components
        toolbar = findViewById(R.id.toolbar)
        ivQrCode = findViewById(R.id.iv_qr_code)
        tvStationName = findViewById(R.id.tv_station_name)
        tvStationLocation = findViewById(R.id.tv_station_location)
        tvBookingId = findViewById(R.id.tv_booking_id)
        btnViewMap = findViewById(R.id.btn_view_map)
        tvBookingDate = findViewById(R.id.tv_booking_date)
        tvBookingTime = findViewById(R.id.tv_booking_time)
        tvDuration = findViewById(R.id.tv_duration)
        tvSlotNumber = findViewById(R.id.tv_slot_number)
        tvStatus = findViewById(R.id.tv_status)
        tvNotes = findViewById(R.id.tv_notes)
        progressBar = findViewById(R.id.progress_bar)
        qrCodeCard = findViewById(R.id.qr_code_card)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Setup map fragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Setup view map button
        btnViewMap.setOnClickListener {
            toggleMapVisibility()
        }
    }

    private fun populateFields() {
        booking?.let { bookingData ->
            Log.d(TAG, "Populating fields with booking data: ${bookingData.id}")
            
            // Set basic information
            tvBookingId.text = bookingData.id
            tvStationName.text = bookingData.chargingStationName ?: "Unknown Station"
            tvStationLocation.text = bookingData.getParsedAddress()

            // Parse and format date/time
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(bookingData.reservationDateTime)
                
                if (date != null) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    
                    tvBookingDate.text = dateFormat.format(date)
                    tvBookingTime.text = timeFormat.format(date)
                } else {
                    tvBookingDate.text = "Invalid date"
                    tvBookingTime.text = "Invalid time"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date", e)
                tvBookingDate.text = bookingData.reservationDateTime.substringBefore('T')
                tvBookingTime.text = bookingData.reservationDateTime.substringAfter('T').substringBefore('Z')
            }

            // Set duration
            tvDuration.text = "${bookingData.duration} min"

            // Set slot number
            tvSlotNumber.text = "Slot ${bookingData.slotNumber}"

            // Set status with appropriate styling
            val statusText = getStatusText(bookingData.status)
            tvStatus.text = statusText
            
            // Apply improved status styling
            applyStatusStyling(tvStatus, statusText)

            // Set notes
            tvNotes.text = if (bookingData.notes.isNullOrEmpty()) "No additional notes" else bookingData.notes
            
            // Fetch charging station details for proper coordinates
            fetchChargingStationDetails(bookingData.chargingStationId)
            
            // Generate QR code
            val qrCodeData = bookingData.qrCode ?: generateBookingQRData(bookingData)
            generateQRCode(qrCodeData)
        } ?: run {
            // Load booking by ID if not provided directly
            bookingId?.let { id ->
                loadBookingById(id)
            }
        }
    }

    private fun loadBookingById(bookingId: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val result = bookingRepository.getBookingById(bookingId)
                result.onSuccess { bookingData ->
                    booking = bookingData
                    populateFields()
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to load booking", exception)
                    showError("Failed to load booking details: ${exception.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading booking", e)
                showError("Error loading booking details")
            } finally {
                showLoading(false)
            }
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
                    
                    chargingStation = station
                    
                    // Update station location text
                    tvStationLocation.text = station.getParsedAddress()
                    
                    // Update map with station coordinates
                    updateMapWithStationCoordinates(station)
                    
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to fetch station details", exception)
                    // Continue with existing data
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while fetching station details", e)
            }
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
                    null
                )
            } else {
                Log.w(TAG, "No valid coordinates found for station: ${station.name}")
            }
        } ?: run {
            Log.d(TAG, "Map not ready yet, storing station for later update")
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Setup map UI and settings
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isCompassEnabled = true
        
        // Set map type to normal
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        
        // Initially hide the map
        mapFragment.view?.visibility = View.GONE
        btnViewMap.text = "View Map"
        
        // If we already have charging station data, update the map
        chargingStation?.let { station ->
            updateMapWithStationCoordinates(station)
        }
    }

    private fun toggleMapVisibility() {
        val mapView = findViewById<View>(R.id.map_fragment)
        if (mapView.visibility == View.GONE) {
            mapView.visibility = View.VISIBLE
            btnViewMap.text = "Hide Map"
        } else {
            mapView.visibility = View.GONE
            btnViewMap.text = "View Map"
        }
    }

    private fun applyStatusStyling(statusView: TextView, statusText: String) {
        // Set common properties
        statusView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        statusView.setPadding(24, 12, 24, 12)
        
        when (statusText.uppercase()) {
            "PENDING" -> {
                statusView.setTextColor(ContextCompat.getColor(this, R.color.status_pending_text))
                statusView.setBackgroundResource(R.drawable.status_pending_background)
                statusView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_pending, 0, 0, 0
                )
            }
            "CONFIRMED" -> {
                statusView.setTextColor(ContextCompat.getColor(this, R.color.status_confirmed_text))
                statusView.setBackgroundResource(R.drawable.status_confirmed_background)
                statusView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_confirmed, 0, 0, 0
                )
            }
            "INPROGRESS" -> {
                statusView.setTextColor(ContextCompat.getColor(this, R.color.status_inprogress_text))
                statusView.setBackgroundResource(R.drawable.status_inprogress_background)
                statusView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_inprogress, 0, 0, 0
                )
            }
            "CANCELLED" -> {
                statusView.setTextColor(ContextCompat.getColor(this, R.color.status_cancelled_text))
                statusView.setBackgroundResource(R.drawable.status_cancelled_background)
                statusView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_cancelled, 0, 0, 0
                )
            }
            "COMPLETED" -> {
                statusView.setTextColor(ContextCompat.getColor(this, R.color.status_completed_text))
                statusView.setBackgroundResource(R.drawable.status_completed_background)
                statusView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_completed, 0, 0, 0
                )
            }
            "NOSHOW" -> {
                statusView.setTextColor(ContextCompat.getColor(this, R.color.status_cancelled_text))
                statusView.setBackgroundResource(R.drawable.status_cancelled_background)
                statusView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_cancelled, 0, 0, 0
                )
            }
            else -> {
                statusView.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                statusView.setBackgroundResource(R.drawable.status_default_background)
                statusView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_info, 0, 0, 0
                )
            }
        }
        
        // Set compound drawable padding
        statusView.compoundDrawablePadding = 12
    }

    private fun getStatusText(status: String): String {
        return when (status.trim().uppercase()) {
            // Handle string status values from API
            "PENDING" -> "Pending"
            "CONFIRMED" -> "Confirmed"
            "INPROGRESS" -> "InProgress"
            "IN_PROGRESS" -> "InProgress"
            "COMPLETED" -> "Completed"
            "CANCELLED" -> "Cancelled"
            "NOSHOW" -> "NoShow"
            "NO_SHOW" -> "NoShow"
            // Handle numeric status values from backend enum
            "0" -> "Pending"
            "1" -> "Confirmed"
            "2" -> "InProgress"
            "3" -> "Completed"
            "4" -> "Cancelled"
            "5" -> "NoShow"
            else -> status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun generateBookingQRData(booking: BookingDto): String {
        return buildString {
            append("${booking.id}")
        }
    }

    private fun generateQRCode(data: String) {
        try {
            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            
            ivQrCode.setImageBitmap(bitmap)
            qrCodeCard.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "Error generating QR code", e)
            qrCodeCard.visibility = View.GONE
        }
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}