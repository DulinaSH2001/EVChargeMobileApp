package com.example.evchargingapp.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.*
import com.example.evchargingapp.data.repository.ChargingStationRepository
import com.example.evchargingapp.ui.dashboard.adapter.ChargingSlotsAdapter
import com.example.evchargingapp.ui.booking.NewReservationActivity
import com.example.evchargingapp.utils.ApiConfig
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat

class ChargingStationDetailsActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "StationDetailsActivity"
        private const val EXTRA_STATION_ID = "extra_station_id"
        
        fun newIntent(context: Context, stationId: String): Intent {
            return Intent(context, ChargingStationDetailsActivity::class.java).apply {
                putExtra(EXTRA_STATION_ID, stationId)
            }
        }
    }
    
    private lateinit var chargingStationRepository: ChargingStationRepository
    private lateinit var chargingSlotsAdapter: ChargingSlotsAdapter
    
    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvStationName: TextView
    private lateinit var tvStationLocation: TextView
    private lateinit var tvStationType: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvAvailableSlots: TextView
    private lateinit var tvTotalSlots: TextView
    private lateinit var tvStatus: TextView
    private lateinit var indicatorStatus: View
    private lateinit var rvSlots: RecyclerView
    private lateinit var btnGetDirections: MaterialButton
    private lateinit var btnBookSlot: MaterialButton
    
    // Google Maps
    private var googleMap: GoogleMap? = null
    
    // Data
    private var stationId: String? = null
    private var currentStation: ChargingStationDto? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charging_station_details)
        
        // Get station ID from intent
        stationId = intent.getStringExtra(EXTRA_STATION_ID)
        Log.d(TAG, "Station ID from intent: $stationId")
        
        if (stationId == null) {
            Log.e(TAG, "No station ID provided in intent")
            Toast.makeText(this, "Station not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        initializeComponents()
        setupViews()
        setupMapFragment()
        loadStationDetails()
    }
    
    private fun initializeComponents() {
        // Initialize API service with authentication
        val authenticatedRetrofit = ApiConfig.getAuthenticatedRetrofit(this)
        val apiService = authenticatedRetrofit.create(ChargingStationApiService::class.java)
        chargingStationRepository = ChargingStationRepository(apiService)
    }
    
    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        tvStationName = findViewById(R.id.tv_station_name)
        tvStationLocation = findViewById(R.id.tv_station_location)
        tvStationType = findViewById(R.id.tv_station_type)
        tvDistance = findViewById(R.id.tv_distance)
        tvAvailableSlots = findViewById(R.id.tv_available_slots)
        tvTotalSlots = findViewById(R.id.tv_total_slots)
        tvStatus = findViewById(R.id.tv_status)
        indicatorStatus = findViewById(R.id.indicator_status)
        rvSlots = findViewById(R.id.rv_slots)
        btnGetDirections = findViewById(R.id.btn_get_directions)
        btnBookSlot = findViewById(R.id.btn_book_slot)
        
        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        
        // Setup RecyclerView
        chargingSlotsAdapter = ChargingSlotsAdapter()
        rvSlots.apply {
            layoutManager = LinearLayoutManager(this@ChargingStationDetailsActivity)
            adapter = chargingSlotsAdapter
        }
        
        // Setup click listeners
        btnGetDirections.setOnClickListener {
            openDirections()
        }
        
        btnBookSlot.setOnClickListener {
            bookSlot()
        }
    }
    
    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync { map ->
            googleMap = map
            setupGoogleMap()
        }
    }
    
    private fun setupGoogleMap() {
        googleMap?.let { map ->
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isMapToolbarEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = false
            map.uiSettings.isCompassEnabled = true
            
            // Set map type to normal
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            
            // Set initial camera position to Sri Lanka center if no station loaded yet
            val sriLankaCenter = LatLng(7.8731, 80.7718) // Center of Sri Lanka
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(sriLankaCenter, 8f))
        }
    }
    
    private fun loadStationDetails() {
        stationId?.let { id ->
            Log.d(TAG, "Loading station details for ID: $id")
            lifecycleScope.launch {
                try {
                    chargingStationRepository.getStationById(id)
                        .onSuccess { station ->
                            Log.d(TAG, "Station loaded successfully: ${station.name}")
                            Log.d(TAG, "Station coordinates: ${station.latitude}, ${station.longitude}")
                            currentStation = station
                            updateUI(station)
                            updateMap(station)
                        }
                        .onFailure { exception ->
                            Log.e(TAG, "Failed to load station details", exception)
                            Toast.makeText(
                                this@ChargingStationDetailsActivity,
                                "Failed to load station details: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while loading station details", e)
                    Toast.makeText(
                        this@ChargingStationDetailsActivity,
                        "Error loading station details",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun updateUI(station: ChargingStationDto) {
        Log.d(TAG, "Updating UI with station data: ${station.name}")
        Log.d(TAG, "Raw location string: ${station.location}")
        
        tvStationName.text = station.name
        // Use parsed address instead of raw location string
        tvStationLocation.text = station.getParsedAddress()
        tvStationType.text = station.type
        
        Log.d(TAG, "Parsed address: ${station.getParsedAddress()}")
        Log.d(TAG, "Parsed coordinates: ${station.getCoordinateLatitude()}, ${station.getCoordinateLongitude()}")
        
        // Format distance
        station.distance?.let { distance ->
            tvDistance.text = "${String.format("%.1f", distance)} km away"
            tvDistance.visibility = View.VISIBLE
        } ?: run {
            tvDistance.visibility = View.GONE
        }
        
        // Update slot counts
        tvAvailableSlots.text = station.availableSlots.toString()
        tvTotalSlots.text = station.totalSlots.toString()
        
        // Update status
        val isAvailable = station.availableSlots > 0 && station.isActive
        Log.d(TAG, "Station status - Available slots: ${station.availableSlots}, Is active: ${station.isActive}")
        
        when {
            isAvailable -> {
                tvStatus.text = "Available Now"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_available))
                indicatorStatus.backgroundTintList = 
                    ContextCompat.getColorStateList(this, R.color.status_available)
                btnBookSlot.isEnabled = true
            }
            !station.isActive -> {
                tvStatus.text = "Under Maintenance"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_maintenance))
                indicatorStatus.backgroundTintList = 
                    ContextCompat.getColorStateList(this, R.color.status_maintenance)
                btnBookSlot.isEnabled = false
            }
            else -> {
                tvStatus.text = "Fully Occupied"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_occupied))
                indicatorStatus.backgroundTintList = 
                    ContextCompat.getColorStateList(this, R.color.status_occupied)
                btnBookSlot.isEnabled = false
            }
        }
        
        // Update slots list - handle null schedule
        station.schedule?.let { slots ->
            Log.d(TAG, "Updating slots list with ${slots.size} items")
            chargingSlotsAdapter.submitList(slots)
        } ?: run {
            Log.d(TAG, "Schedule is null, showing empty list")
            // Create empty list or show message that detailed schedule is not available
            chargingSlotsAdapter.submitList(emptyList())
        }
    }
    
    private fun updateMap(station: ChargingStationDto) {
        googleMap?.let { map ->
            Log.d(TAG, "Updating map for station: ${station.name}")
            
            // Clear existing markers
            map.clear()
            
            val lat = station.getCoordinateLatitude()
            val lng = station.getCoordinateLongitude()
            
            if (lat != null && lng != null) {
                Log.d(TAG, "Setting map position to: $lat, $lng")
                val position = LatLng(lat, lng)
                
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
    
    private fun openDirections() {
        currentStation?.let { station ->
            val lat = station.getCoordinateLatitude()
            val lng = station.getCoordinateLongitude()
            
            if (lat != null && lng != null) {
                try {
                    // First try to open in Google Maps app
                    val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${station.name})")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    
                    if (mapIntent.resolveActivity(packageManager) != null) {
                        startActivity(mapIntent)
                    } else {
                        // Fallback: Try generic navigation intent
                        val genericIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                        )
                        
                        if (genericIntent.resolveActivity(packageManager) != null) {
                            startActivity(Intent.createChooser(genericIntent, "Open with"))
                        } else {
                            // Final fallback: Open in web browser
                            val webUri = Uri.parse(
                                "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
                            )
                            startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Unable to open directions: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(this, "Location coordinates not available", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Station information not available", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun bookSlot() {
        currentStation?.let { station ->
            if (station.availableSlots > 0 && station.isActive && station.id != null) {
                // Navigate to booking screen with selected station
                val intent = NewReservationActivity.newIntent(
                    context = this,
                    stationId = station.id,
                    stationName = station.name,
                    stationLocation = station.getParsedAddress(),
                    availableSlots = station.availableSlots
                )
                startActivity(intent)
                
            } else if (!station.isActive) {
                Toast.makeText(
                    this,
                    "Station is currently under maintenance",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (station.availableSlots <= 0) {
                Toast.makeText(
                    this,
                    "No slots available at this time",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Station information incomplete",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } ?: run {
            Toast.makeText(this, "Station information not available", Toast.LENGTH_SHORT).show()
        }
    }
}