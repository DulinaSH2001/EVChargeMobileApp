package com.example.evchargingapp.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
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
import com.example.evchargingapp.ui.dashboard.adapter.UnifiedSearchAdapter
import com.example.evchargingapp.ui.dashboard.adapter.LocationSearchResult
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class EnhancedDashboardFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var chargingStationRepository: ChargingStationRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var chargingStationAdapter: ChargingStationAdapter
    private lateinit var placesClient: PlacesClient
    
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
    
    // Map Search Components
    private lateinit var etMapSearch: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var fabCurrentLocation: FloatingActionButton
    private lateinit var rvSearchSuggestions: RecyclerView
    private lateinit var cardSearchSuggestions: MaterialCardView
    private lateinit var cardOfflineIndicator: MaterialCardView
    private lateinit var unifiedSearchAdapter: UnifiedSearchAdapter
    
    // Search functionality
    private var searchJob: Job? = null
    
    // Offline fallback data for common Sri Lankan locations
    private val offlineLocations = listOf(
        // Major Cities
        OfflineLocation("Colombo", "Western Province, Sri Lanka", 6.9271, 79.8612),
        OfflineLocation("Kandy", "Central Province, Sri Lanka", 7.2906, 80.6337),
        OfflineLocation("Galle", "Southern Province, Sri Lanka", 6.0535, 80.2210),
        OfflineLocation("Jaffna", "Northern Province, Sri Lanka", 9.6615, 80.0255),
        OfflineLocation("Negombo", "Western Province, Sri Lanka", 7.2083, 79.8358),
        
        // Provincial Capitals
        OfflineLocation("Anuradhapura", "North Central Province, Sri Lanka", 8.3114, 80.4037),
        OfflineLocation("Batticaloa", "Eastern Province, Sri Lanka", 7.7102, 81.6924),
        OfflineLocation("Matara", "Southern Province, Sri Lanka", 5.9549, 80.5550),
        OfflineLocation("Kurunegala", "North Western Province, Sri Lanka", 7.4818, 80.3609),
        OfflineLocation("Ratnapura", "Sabaragamuwa Province, Sri Lanka", 6.6828, 80.3992),
        OfflineLocation("Trincomalee", "Eastern Province, Sri Lanka", 8.5874, 81.2152),
        OfflineLocation("Badulla", "Uva Province, Sri Lanka", 6.9934, 81.0550),
        
        // Other Important Cities
        OfflineLocation("Kalutara", "Western Province, Sri Lanka", 6.5854, 79.9607),
        OfflineLocation("Panadura", "Western Province, Sri Lanka", 6.7132, 79.9026),
        OfflineLocation("Moratuwa", "Western Province, Sri Lanka", 6.7730, 79.9816),
        OfflineLocation("Dehiwala", "Western Province, Sri Lanka", 6.8571, 79.8612),
        OfflineLocation("Kotte", "Western Province, Sri Lanka", 6.8905, 79.9015),
        OfflineLocation("Maharagama", "Western Province, Sri Lanka", 6.8482, 79.9267),
        OfflineLocation("Kelaniya", "Western Province, Sri Lanka", 6.9553, 79.9221),
        OfflineLocation("Gampaha", "Western Province, Sri Lanka", 7.0873, 79.9999),
        
        // Tourist Destinations
        OfflineLocation("Nuwara Eliya", "Central Province, Sri Lanka", 6.9497, 80.7891),
        OfflineLocation("Ella", "Uva Province, Sri Lanka", 6.8692, 81.0456),
        OfflineLocation("Sigiriya", "Central Province, Sri Lanka", 7.9568, 80.7592),
        OfflineLocation("Dambulla", "Central Province, Sri Lanka", 7.8731, 80.6511),
        OfflineLocation("Polonnaruwa", "North Central Province, Sri Lanka", 7.9403, 81.0188),
        OfflineLocation("Bentota", "Southern Province, Sri Lanka", 6.4168, 79.9968),
        OfflineLocation("Hikkaduwa", "Southern Province, Sri Lanka", 6.1408, 80.1003),
        OfflineLocation("Unawatuna", "Southern Province, Sri Lanka", 6.0108, 80.2494),
        
        // Business Districts
        OfflineLocation("Fort", "Colombo, Western Province", 6.9344, 79.8428),
        OfflineLocation("Pettah", "Colombo, Western Province", 6.9388, 79.8542),
        OfflineLocation("Bambalapitiya", "Colombo, Western Province", 6.8918, 79.8564),
        OfflineLocation("Wellawatte", "Colombo, Western Province", 6.8775, 79.8584),
        OfflineLocation("Mount Lavinia", "Western Province, Sri Lanka", 6.8327, 79.8637)
    )
    
    data class OfflineLocation(
        val name: String,
        val description: String,
        val latitude: Double,
        val longitude: Double
    )
    
    // Network connectivity functions
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
                   capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                   capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo?.isConnected == true
        }
    }
    
    private fun checkNetworkStatus() {
        if (isNetworkAvailable()) {
            cardOfflineIndicator.visibility = View.GONE
            Log.d("Dashboard", "Network available - hiding offline indicator")
        } else {
            cardOfflineIndicator.visibility = View.VISIBLE
            Log.d("Dashboard", "Network unavailable - showing offline indicator")
        }
    }
    
    override fun onResume() {
        super.onResume()
        checkNetworkStatus()
    }
    
    // Google Maps
    private var googleMap: GoogleMap? = null
    private var currentLocation: Location? = null
    private var isUsingSearchedLocation: Boolean = false
    
    // Selected location variables
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    
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
        
        // Initialize Google Places API
        try {
            val apiKey = getString(R.string.google_maps_key)
            Log.d(TAG, "🔑 Using API key: ${apiKey.take(20)}...")
            
            if (!Places.isInitialized()) {
                Places.initialize(requireContext(), apiKey)
                Log.d(TAG, "✅ Places API initialized successfully")
            } else {
                Log.d(TAG, "✅ Places API already initialized")
            }
            placesClient = Places.createClient(requireContext())
            Log.d(TAG, "✅ Places client created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing Places API: ${e.javaClass.simpleName}: ${e.message}", e)
            Toast.makeText(requireContext(), "Error initializing Places API: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
        // Log session debug info
        sessionManager.logSessionDebugInfo()
        
        // Initialize API service with authentication
        val authenticatedRetrofit = ApiConfig.getAuthenticatedRetrofit(requireContext())
        val apiService = authenticatedRetrofit.create(ChargingStationApiService::class.java)
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
        
        // Map Search Components
        etMapSearch = view.findViewById(R.id.et_map_search)
        btnClearSearch = view.findViewById(R.id.btn_clear_search)
        fabCurrentLocation = view.findViewById(R.id.fab_current_location)
        rvSearchSuggestions = view.findViewById(R.id.rv_search_suggestions)
        cardSearchSuggestions = view.findViewById(R.id.card_search_suggestions)
        cardOfflineIndicator = view.findViewById(R.id.card_offline_indicator)
        
        setupClickListeners()
        setupUserInterface()
        setupMapSearch()
        checkNetworkStatus()
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
    
    private fun setupMapSearch() {
        Log.d(TAG, "Setting up map search functionality")
        
        // Setup unified search adapter
        unifiedSearchAdapter = UnifiedSearchAdapter { result ->
            Log.d(TAG, "Location selected: ${result.primaryText}")
            handleLocationSelection(result)
        }
        
        rvSearchSuggestions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = unifiedSearchAdapter
        }
        Log.d(TAG, "Search suggestions RecyclerView setup complete")
        
        // Setup search text watcher
        etMapSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                Log.d(TAG, "Text changed to: '$query'")
                
                searchJob?.cancel()
                
                if (query.isEmpty()) {
                    hideSuggestions()
                    btnClearSearch.visibility = View.GONE
                } else {
                    btnClearSearch.visibility = View.VISIBLE
                    // Show suggestions immediately for better user experience
                    searchJob = lifecycleScope.launch {
                        delay(150) // Shorter debounce for more responsive UI
                        Log.d(TAG, "Executing search for: '$query'")
                        performUnifiedSearch(query)
                    }
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Setup focus listener to show suggestions when focused
        etMapSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && etMapSearch.text.isNotEmpty()) {
                // Re-show suggestions if text exists and field is focused
                performUnifiedSearch(etMapSearch.text.toString().trim())
            } else if (!hasFocus) {
                // Hide suggestions when focus is lost (with small delay)
                lifecycleScope.launch {
                    delay(200) // Small delay to allow item clicks
                    hideSuggestions()
                }
            }
        }
        
        // Clear search button
        btnClearSearch.setOnClickListener {
            Log.d(TAG, "Clear search button clicked")
            etMapSearch.text.clear()
            hideSuggestions()
        }
        
        Log.d(TAG, "Map search setup completed")
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
            map.uiSettings.isMyLocationButtonEnabled = false // Disable default button since we have our own
            
            // Set default location (Colombo, Sri Lanka)
            val defaultLocation = LatLng(6.9271, 79.8612)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
            
            // Set map click listener
            map.setOnMarkerClickListener { marker ->
                val station = marker.tag as? ChargingStationDto
                station?.let { navigateToStationDetails(it) }
                true
            }
            
            // Handle map load error gracefully
            map.setOnMapLoadedCallback {
                Log.d(TAG, "Map loaded successfully")
            }
            
            // Add offline mode indicator if needed
            try {
                // Test if we can show map tiles
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL)
            } catch (e: Exception) {
                Log.e(TAG, "Map setup error", e)
                showOfflineMapMessage()
            }
        }
    }
    
    private fun showOfflineMapMessage() {
        context?.let { ctx ->
            Toast.makeText(ctx, "Map is running in offline mode. Search still works!", Toast.LENGTH_LONG).show()
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
            // Navigate to new booking screen
            (activity as? MainActivity)?.createNewBooking()
        }
        
        btnMyBookings.setOnClickListener {
            // Navigate to my bookings screen
            (activity as? MainActivity)?.navigateToBookings()
        }
        
        btnScanQr.setOnClickListener {
            // Navigate to QR scanner
            (activity as? MainActivity)?.scanQRCode()
        }
        
        btnViewAll.setOnClickListener {
            // Navigate to all stations view - for now, scroll to stations list
            rvChargingStations.smoothScrollToPosition(0)
        }
        
        cardPending.setOnClickListener {
            // Navigate to pending bookings - same as My Bookings for now
            (activity as? MainActivity)?.navigateToBookings()
        }
        
        cardApproved.setOnClickListener {
            // Navigate to approved bookings - same as My Bookings for now
            (activity as? MainActivity)?.navigateToBookings()
        }
        
        // Current location FAB
        fabCurrentLocation.setOnClickListener {
            getCurrentLocation()
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
        // Use placeholder values since API endpoints were removed
        // These can be replaced with actual data sources or removed entirely
        tvPendingCount.text = "0"
        tvApprovedCount.text = "0"
        
        Log.d(TAG, "Dashboard stats loaded with placeholder values")
    }
    
    private fun loadIndividualStats() {
        // Use placeholder values since API endpoints were removed
        tvPendingCount.text = "0"
        tvApprovedCount.text = "0"
        
        Log.d(TAG, "Individual stats loaded with placeholder values")
    }
    
    private fun loadNearbyStations(latitude: Double, longitude: Double) {
        Log.d(TAG, "Loading nearby stations for location: $latitude, $longitude")
        lifecycleScope.launch {
            try {
                chargingStationRepository.getNearbyStations(latitude, longitude)
                    .onSuccess { stations ->
                        Log.d(TAG, "Loaded ${stations.size} nearby stations")
                        chargingStations.clear()
                        chargingStations.addAll(stations)
                        updateStationsList()
                        updateMapMarkers()
                        
                        Toast.makeText(context, "Found ${stations.size} nearby stations", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Failed to load nearby stations", exception)
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Failed to load nearby stations: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                        loadAllStations() // Fallback to all stations
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading nearby stations", e)
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
                val lat = station.getCoordinateLatitude()
                val lng = station.getCoordinateLongitude()
                
                if (lat != null && lng != null) {
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
    
    private fun performUnifiedSearch(query: String) {
        if (query.isEmpty()) return
        
        Log.d(TAG, "🔍 PERFORMING UNIFIED SEARCH FOR: '$query'")
        Log.d(TAG, "🌐 Network available: ${isNetworkAvailable()}")
        Log.d(TAG, "📱 Places client initialized: ${::placesClient.isInitialized}")
        
        // First try offline search for immediate results
        val offlineResults = searchOffline(query)
        
        // Always show offline results immediately if available
        if (offlineResults.isNotEmpty()) {
            Log.d(TAG, "✅ Found ${offlineResults.size} offline results, showing immediately")
            showSuggestions(offlineResults)
        }
        
        // Then try online search if network is available (for more comprehensive results)
        if (query.length >= 2 && isNetworkAvailable()) { // Only search online for 2+ characters and if network is available
            Log.d(TAG, "🌐 ATTEMPTING ONLINE SEARCH...")
            searchOnline(query) { onlineResults ->
                Log.d(TAG, "🌐 ONLINE SEARCH CALLBACK: Found ${onlineResults.size} results")
                if (onlineResults.isNotEmpty()) {
                    Log.d(TAG, "✅ Processing ${onlineResults.size} online results")
                    // Combine offline and online results, prioritizing online but keeping offline as fallback
                    val combinedResults = mutableListOf<LocationSearchResult>()
                    
                    // Add online results first
                    combinedResults.addAll(onlineResults)
                    
                    // Add offline results that don't duplicate online results
                    offlineResults.forEach { offline ->
                        val isDuplicate = onlineResults.any { online ->
                            offline.primaryText.equals(online.primaryText, ignoreCase = true)
                        }
                        if (!isDuplicate) {
                            combinedResults.add(offline)
                        }
                    }
                    
                    // Limit to reasonable number of results
                    val finalResults = combinedResults.take(8)
                    Log.d(TAG, "✅ Showing ${finalResults.size} combined results")
                    showSuggestions(finalResults)
                } else if (offlineResults.isEmpty()) {
                    hideSuggestions()
                }
            }
        } else if (query.length >= 2 && !isNetworkAvailable() && offlineResults.isEmpty()) {
            Log.d(TAG, "⚠️ No network connection and no offline results for: $query")
        } else if (query.length < 2) {
            Log.d(TAG, "⚠️ Query too short for online search: '$query'")
        } else {
            Log.d(TAG, "⚠️ Skipping online search - network: ${isNetworkAvailable()}, query length: ${query.length}")
        }
    }
    
    private fun searchOffline(query: String): List<LocationSearchResult> {
        return offlineLocations.filter { location ->
            location.name.contains(query, ignoreCase = true) ||
            location.description.contains(query, ignoreCase = true) ||
            // Also match partial words for better search experience
            location.name.split(" ").any { it.startsWith(query, ignoreCase = true) }
        }.sortedBy { location ->
            // Sort by relevance: exact matches first, then starts with, then contains
            when {
                location.name.equals(query, ignoreCase = true) -> 0
                location.name.startsWith(query, ignoreCase = true) -> 1
                location.name.contains(query, ignoreCase = true) -> 2
                else -> 3
            }
        }.map { location ->
            LocationSearchResult.OfflineResult(
                primaryText = location.name,
                secondaryText = location.description,
                latitude = location.latitude,
                longitude = location.longitude
            )
        }.take(5) // Limit offline results to top 5
    }
    
    private fun searchOnline(query: String, callback: (List<LocationSearchResult>) -> Unit) {
        Log.d(TAG, "🌐 STARTING ONLINE SEARCH for: '$query'")
        
        // Don't try online search if no network
        if (!isNetworkAvailable()) {
            Log.d(TAG, "❌ No network available, skipping online search")
            callback(emptyList())
            return
        }
        
        try {
            // Check if Places client is initialized
            if (!::placesClient.isInitialized) {
                Log.e(TAG, "❌ Places client not initialized")
                callback(emptyList())
                return
            }
            
            Log.d(TAG, "🔧 Building Places API request...")
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setCountries("LK") // Restrict to Sri Lanka
                .build()
                
            Log.d(TAG, "📡 Sending request to Places API...")
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    Log.d(TAG, "✅ PLACES API SUCCESS! Found ${response.autocompletePredictions.size} predictions")
                    
                    // Log each prediction for debugging
                    response.autocompletePredictions.forEachIndexed { index, prediction ->
                        Log.d(TAG, "   $index: ${prediction.getPrimaryText(null)} - ${prediction.getSecondaryText(null)}")
                    }
                    
                    // Convert predictions to LocationSearchResult and fetch coordinates
                    val onlineResults = mutableListOf<LocationSearchResult>()
                    var pendingRequests = response.autocompletePredictions.size
                    
                    if (pendingRequests == 0) {
                        Log.d(TAG, "⚠️ No predictions returned")
                        callback(emptyList())
                        return@addOnSuccessListener
                    }
                    
                    Log.d(TAG, "🔄 Fetching coordinates for ${pendingRequests} predictions...")
                    response.autocompletePredictions.forEach { prediction ->
                        fetchPlaceCoordinates(prediction) { lat, lng ->
                            if (lat != null && lng != null) {
                                Log.d(TAG, "✅ Got coordinates for ${prediction.getPrimaryText(null)}: $lat, $lng")
                                onlineResults.add(
                                    LocationSearchResult.OnlineResult(
                                        primaryText = prediction.getPrimaryText(null).toString(),
                                        secondaryText = prediction.getSecondaryText(null).toString(),
                                        latitude = lat,
                                        longitude = lng,
                                        placeId = prediction.placeId
                                    )
                                )
                            } else {
                                Log.w(TAG, "⚠️ No coordinates for ${prediction.getPrimaryText(null)}")
                            }
                            pendingRequests--
                            Log.d(TAG, "🔢 Pending requests: $pendingRequests")
                            if (pendingRequests == 0) {
                                Log.d(TAG, "✅ All coordinates fetched, returning ${onlineResults.size} results")
                                callback(onlineResults)
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "❌ PLACES API FAILED: ${exception.javaClass.simpleName}: ${exception.message}", exception)
                    when {
                        exception.message?.contains("UnknownHostException") == true ||
                        exception.message?.contains("NO_CONNECTIVITY") == true -> {
                            Log.i(TAG, "🌐 Network connectivity issue detected")
                        }
                        exception.message?.contains("OVER_QUERY_LIMIT") == true -> {
                            Log.w(TAG, "⚠️ Google Places API quota exceeded")
                        }
                        exception.message?.contains("REQUEST_DENIED") == true -> {
                            Log.e(TAG, "🔑 API key issue - Places API access denied")
                        }
                        exception.message?.contains("INVALID_REQUEST") == true -> {
                            Log.e(TAG, "❌ Invalid Places API request")
                        }
                        else -> {
                            Log.w(TAG, "❓ Unknown Places API error: ${exception.message}")
                        }
                    }
                    callback(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Exception in online search: ${e.javaClass.simpleName}: ${e.message}", e)
            callback(emptyList())
        }
    }
    
    private fun fetchPlaceCoordinates(prediction: AutocompletePrediction, callback: (Double?, Double?) -> Unit) {
        val placeFields = listOf(Place.Field.ID, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
        
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val latLng = response.place.latLng
                callback(latLng?.latitude, latLng?.longitude)
            }
            .addOnFailureListener {
                callback(null, null)
            }
    }
    
    private fun handleLocationSelection(result: LocationSearchResult) {
        Log.d(TAG, "Handling location selection: ${result.primaryText}")
        
        selectedLatitude = result.latitude
        selectedLongitude = result.longitude
        
        // Update search field with selected place name and clear focus
        etMapSearch.setText(result.primaryText)
        etMapSearch.clearFocus()
        
        // Hide suggestions immediately
        hideSuggestions()
        
        // Move map to selected location
        moveToLocation(result.latitude, result.longitude, result.primaryText)
        
        // Fetch nearby charging stations
        fetchNearbyChargingStations()
        
        // Show confirmation message
        Toast.makeText(context, "Selected: ${result.primaryText}", Toast.LENGTH_SHORT).show()
    }
    
    private fun showSuggestions(results: List<LocationSearchResult>) {
        Log.d(TAG, "Showing ${results.size} search results")
        if (results.isNotEmpty()) {
            unifiedSearchAdapter.updateResults(results)
            cardSearchSuggestions.visibility = View.VISIBLE
            Log.d(TAG, "Search suggestions card is now visible")
        } else {
            Log.d(TAG, "No results to show")
            hideSuggestions()
        }
    }
    
    private fun hideSuggestions() {
        cardSearchSuggestions.visibility = View.GONE
    }
    
    private fun moveToLocation(latitude: Double, longitude: Double, title: String) {
        Log.d(TAG, "Moving map to location: $latitude, $longitude with title: $title")
        val latLng = LatLng(latitude, longitude)
        googleMap?.apply {
            animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            // Add a marker for the selected location
            addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            Log.d(TAG, "Map camera moved and marker added")
        } ?: Log.e(TAG, "GoogleMap is null, cannot move camera")
    }
    
    private fun fetchNearbyChargingStations() {
        if (selectedLatitude != null && selectedLongitude != null) {
            Log.d(TAG, "Fetching nearby charging stations for: $selectedLatitude, $selectedLongitude")
            loadNearbyStations(selectedLatitude!!, selectedLongitude!!)
        } else {
            Log.e(TAG, "Selected coordinates are null, cannot fetch nearby stations")
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
        private const val TAG = "EnhancedDashboardFragment"
        
        fun newInstance(): EnhancedDashboardFragment {
            return EnhancedDashboardFragment()
        }
    }
}