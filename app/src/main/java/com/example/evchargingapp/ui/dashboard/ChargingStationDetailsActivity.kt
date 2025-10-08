package com.example.evchargingapp.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.example.evchargingapp.utils.ApiConfig
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.core.content.ContextCompat

class ChargingStationDetailsActivity : AppCompatActivity() {
    
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
        
        if (stationId == null) {
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
        // Initialize API service
        val retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val apiService = retrofit.create(ChargingStationApiService::class.java)
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
        }
    }
    
    private fun loadStationDetails() {
        stationId?.let { id ->
            lifecycleScope.launch {
                try {
                    chargingStationRepository.getStationById(id)
                        .onSuccess { station ->
                            currentStation = station
                            updateUI(station)
                            updateMap(station)
                        }
                        .onFailure { exception ->
                            Toast.makeText(
                                this@ChargingStationDetailsActivity,
                                "Failed to load station details: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } catch (e: Exception) {
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
        tvStationName.text = station.name
        tvStationLocation.text = station.location
        tvStationType.text = station.type
        
        // Format distance
        station.distance?.let { distance ->
            tvDistance.text = "${String.format("%.1f", distance)} km away"
        } ?: run {
            tvDistance.visibility = View.GONE
        }
        
        // Update slot counts
        tvAvailableSlots.text = station.availableSlots.toString()
        tvTotalSlots.text = station.totalSlots.toString()
        
        // Update status
        val isAvailable = station.availableSlots > 0 && station.isActive
        
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
        
        // Update slots list
        station.schedule?.let { slots ->
            chargingSlotsAdapter.submitList(slots)
        }
    }
    
    private fun updateMap(station: ChargingStationDto) {
        googleMap?.let { map ->
            station.latitude?.let { lat ->
                station.longitude?.let { lng ->
                    val position = LatLng(lat, lng)
                    
                    // Add marker
                    map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(station.name)
                            .snippet(station.location)
                    )
                    
                    // Move camera to station location
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
                }
            }
        }
    }
    
    private fun openDirections() {
        currentStation?.let { station ->
            station.latitude?.let { lat ->
                station.longitude?.let { lng ->
                    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${station.name})")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")
                    
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        // Fallback to web browser
                        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                        startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                }
            }
        }
    }
    
    private fun bookSlot() {
        currentStation?.let { station ->
            // TODO: Navigate to booking screen with selected station
            Toast.makeText(this, "Booking slot at ${station.name}", Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        private const val EXTRA_STATION_ID = "extra_station_id"
        
        fun newIntent(context: Context, stationId: String): Intent {
            return Intent(context, ChargingStationDetailsActivity::class.java).apply {
                putExtra(EXTRA_STATION_ID, stationId)
            }
        }
    }
}