package com.example.evchargingapp.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.*
import com.example.evchargingapp.data.repository.ChargingStationRepository
import com.example.evchargingapp.ui.dashboard.adapter.ChargingStationAdapter
import com.example.evchargingapp.utils.ApiConfig
import com.example.evchargingapp.utils.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EnhancedDashboardFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var chargingStationRepository: ChargingStationRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var chargingStationAdapter: ChargingStationAdapter
    
    // UI Components
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvPendingCount: TextView
    private lateinit var tvApprovedCount: TextView
    private lateinit var rvChargingStations: RecyclerView
    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnNewBooking: MaterialButton
    private lateinit var btnMyBookings: MaterialButton
    private lateinit var btnScanQr: MaterialButton
    private lateinit var btnViewAll: MaterialButton
    private lateinit var cardPending: MaterialCardView
    private lateinit var cardApproved: MaterialCardView
    
    // Google Maps
    private var googleMap: GoogleMap? = null
    private var currentLocation: Location? = null
    
    // Data
    private var chargingStations = mutableListOf<ChargingStationDto>()
    
    // Location permission request
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                enableLocationServices()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                enableLocationServices()
            }
            else -> {
                // No location access granted
                showLocationPermissionDeniedMessage()
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard_enhanced, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeComponents()
        setupViews(view)
        setupRecyclerView()
        setupMapFragment()
        checkLocationPermissions()
        loadInitialData()
    }
    
    private fun initializeComponents() {
        sessionManager = SessionManager(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Initialize API service
        val retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val apiService = retrofit.create(ChargingStationApiService::class.java)
        chargingStationRepository = ChargingStationRepository(apiService)
    }
    
    private fun setupViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        tvPendingCount = view.findViewById(R.id.tv_pending_count)
        tvApprovedCount = view.findViewById(R.id.tv_approved_count)
        rvChargingStations = view.findViewById(R.id.rv_charging_stations)
        btnRefresh = view.findViewById(R.id.btn_refresh)
        btnNewBooking = view.findViewById(R.id.btn_new_booking)
        btnMyBookings = view.findViewById(R.id.btn_my_bookings)
        btnScanQr = view.findViewById(R.id.btn_scan_qr)
        btnViewAll = view.findViewById(R.id.btn_view_all)
        cardPending = view.findViewById(R.id.card_pending)
        cardApproved = view.findViewById(R.id.card_approved)
        
        setupClickListeners()
        setupUserInterface()
    }
    
    private fun setupRecyclerView() {
        chargingStationAdapter = ChargingStationAdapter(
            onViewDetailsClick = { station ->
                navigateToStationDetails(station)
            },
            onBookNowClick = { station ->
                navigateToBooking(station)
            }
        )
        
        rvChargingStations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chargingStationAdapter
        }
    }
    
    private fun setupMapFragment() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync { map ->
            googleMap = map
            setupGoogleMap()
        }
    }
    
    private fun setupGoogleMap() {
        googleMap?.let { map ->
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            
            // Set default location (Colombo, Sri Lanka)
            val defaultLocation = LatLng(6.9271, 79.8612)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
            
            // Set map click listener
            map.setOnMarkerClickListener { marker ->
                val station = marker.tag as? ChargingStationDto
                station?.let { navigateToStationDetails(it) }
                true
            }
        }
    }
    
    private fun setupClickListeners() {
        swipeRefresh.setOnRefreshListener {
            refreshData()
        }
        
        btnRefresh.setOnClickListener {
            refreshData()
        }
        
        btnNewBooking.setOnClickListener {
            (activity as? MainActivity)?.createNewBooking()
        }
        
        btnMyBookings.setOnClickListener {
            (activity as? MainActivity)?.navigateToBookings()
        }
        
        btnScanQr.setOnClickListener {
            (activity as? MainActivity)?.scanQRCode()
        }
        
        btnViewAll.setOnClickListener {
            navigateToAllStations()
        }
        
        cardPending.setOnClickListener {
            navigateToReservations(ReservationType.PENDING)
        }
        
        cardApproved.setOnClickListener {
            navigateToReservations(ReservationType.APPROVED)
        }
    }
    
    private fun setupUserInterface() {
        // Show/hide scan QR button for operators
        if (sessionManager.isOperator()) {
            btnScanQr.visibility = View.VISIBLE
        } else {
            btnScanQr.visibility = View.GONE
        }
    }
    
    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableLocationServices()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    private fun enableLocationServices() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            getCurrentLocation()
        }
    }
    
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    currentLocation = location
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        loadNearbyStations(it.latitude, it.longitude)
                    }
                }
                .addOnFailureListener {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Unable to get current location", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
    
    private fun loadInitialData() {
        loadDashboardStats()
        // Load all stations if location is not available
        currentLocation?.let {
            loadNearbyStations(it.latitude, it.longitude)
        } ?: loadAllStations()
    }
    
    private fun refreshData() {
        loadDashboardStats()
        currentLocation?.let {
            loadNearbyStations(it.latitude, it.longitude)
        } ?: loadAllStations()
        swipeRefresh.isRefreshing = false
    }
    
    private fun loadDashboardStats() {
        lifecycleScope.launch {
            try {
                chargingStationRepository.getDashboardStats()
                    .onSuccess { stats ->
                        tvPendingCount.text = stats.pendingReservations.toString()
                        tvApprovedCount.text = stats.approvedReservations.toString()
                    }
                    .onFailure {
                        // Load individual counts as fallback
                        loadIndividualStats()
                    }
            } catch (e: Exception) {
                loadIndividualStats()
            }
        }
    }
    
    private fun loadIndividualStats() {
        lifecycleScope.launch {
            // Load pending reservations count
            chargingStationRepository.getPendingReservations()
                .onSuccess { reservations ->
                    tvPendingCount.text = reservations.size.toString()
                }
                .onFailure {
                    tvPendingCount.text = "0"
                }
            
            // Load approved reservations count
            chargingStationRepository.getApprovedReservations()
                .onSuccess { reservations ->
                    tvApprovedCount.text = reservations.size.toString()
                }
                .onFailure {
                    tvApprovedCount.text = "0"
                }
        }
    }
    
    private fun loadNearbyStations(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            try {
                chargingStationRepository.getNearbyStations(latitude, longitude)
                    .onSuccess { stations ->
                        chargingStations.clear()
                        chargingStations.addAll(stations)
                        updateStationsList()
                        updateMapMarkers()
                    }
                    .onFailure { exception ->
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Failed to load nearby stations: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                        loadAllStations() // Fallback to all stations
                    }
            } catch (e: Exception) {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Error loading stations", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadAllStations() {
        lifecycleScope.launch {
            try {
                chargingStationRepository.getAllStations()
                    .onSuccess { stations ->
                        chargingStations.clear()
                        chargingStations.addAll(stations)
                        updateStationsList()
                        updateMapMarkers()
                    }
                    .onFailure { exception ->
                        context?.let { 
                            Toast.makeText(it, "Failed to load stations: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (e: Exception) {
                context?.let { 
                    Toast.makeText(it, "Error loading stations", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateStationsList() {
        // Show only first 5 stations in the list
        val limitedStations = chargingStations.take(5)
        chargingStationAdapter.submitList(limitedStations)
    }
    
    private fun updateMapMarkers() {
        googleMap?.let { map ->
            map.clear()
            
            chargingStations.forEach { station ->
                station.latitude?.let { lat ->
                    station.longitude?.let { lng ->
                        val position = LatLng(lat, lng)
                        val markerColor = when {
                            station.availableSlots > 0 && station.isActive -> BitmapDescriptorFactory.HUE_GREEN
                            !station.isActive -> BitmapDescriptorFactory.HUE_ORANGE
                            else -> BitmapDescriptorFactory.HUE_RED
                        }
                        
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(station.name)
                                .snippet("${station.availableSlots}/${station.totalSlots} available")
                                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                        )
                        marker?.tag = station
                    }
                }
            }
        }
    }
    
    private fun showLocationPermissionDeniedMessage() {
        context?.let { ctx ->
            Toast.makeText(ctx, "Location permission is required to show nearby stations", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun navigateToStationDetails(station: ChargingStationDto) {
        station.id?.let { stationId ->
            val intent = ChargingStationDetailsActivity.newIntent(requireContext(), stationId)
            startActivity(intent)
        } ?: run {
            context?.let { ctx ->
                Toast.makeText(ctx, "Station details not available", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun navigateToBooking(station: ChargingStationDto) {
        // TODO: Navigate to booking screen with selected station
        context?.let { ctx ->
            Toast.makeText(ctx, "Booking ${station.name}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToAllStations() {
        // TODO: Navigate to all stations screen
        context?.let { ctx ->
            Toast.makeText(ctx, "Viewing all stations", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToReservations(type: ReservationType) {
        // TODO: Navigate to reservations screen filtered by type
        context?.let { ctx ->
            Toast.makeText(ctx, "Viewing ${type.name.lowercase()} reservations", Toast.LENGTH_SHORT).show()
        }
    }
    
    enum class ReservationType {
        PENDING, APPROVED
    }
    
    companion object {
        fun newInstance(): EnhancedDashboardFragment {
            return EnhancedDashboardFragment()
        }
    }
}