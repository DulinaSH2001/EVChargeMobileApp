package com.example.evchargingapp.ui.dashboard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.ChargingStationApiService
import com.example.evchargingapp.data.api.ChargingStationDto
import com.example.evchargingapp.data.repository.ChargingStationRepository
import com.example.evchargingapp.ui.dashboard.adapter.ChargingStationSearchAdapter
import com.example.evchargingapp.utils.ApiConfig
import com.example.evchargingapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BookingFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var chargingStationRepository: ChargingStationRepository
    private lateinit var stationSearchAdapter: ChargingStationSearchAdapter
    
    // Search components
    private lateinit var etStationSearch: EditText
    private lateinit var btnClearStationSearch: ImageButton
    private lateinit var rvStationSuggestions: RecyclerView
    private lateinit var cardStationSuggestions: MaterialCardView
    
    // Selected station display
    private lateinit var layoutSelectedStation: MaterialCardView
    private lateinit var tvSelectedStationName: TextView
    private lateinit var tvSelectedStationLocation: TextView
    private lateinit var tvSelectedStationAvailability: TextView
    private lateinit var btnChangeStation: MaterialButton
    
    // Next button for navigation
    private lateinit var btnNext: MaterialButton
    
    private var selectedStation: ChargingStationDto? = null
    private var searchJob: Job? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_booking, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupStationSearch()
        setupClickListeners()
    }
    
    private fun initViews(view: View) {
        sessionManager = SessionManager(requireContext())
        
        // Initialize repository
        val authenticatedRetrofit = ApiConfig.getAuthenticatedRetrofit(requireContext())
        val apiService = authenticatedRetrofit.create(ChargingStationApiService::class.java)
        chargingStationRepository = ChargingStationRepository(apiService)
        
        // Search components
        etStationSearch = view.findViewById(R.id.et_station_search)
        btnClearStationSearch = view.findViewById(R.id.btn_clear_station_search)
        rvStationSuggestions = view.findViewById(R.id.rv_station_suggestions)
        cardStationSuggestions = view.findViewById(R.id.card_station_suggestions)
        
        // Selected station display
        layoutSelectedStation = view.findViewById(R.id.layout_selected_station)
        tvSelectedStationName = view.findViewById(R.id.tv_selected_station_name)
        tvSelectedStationLocation = view.findViewById(R.id.tv_selected_station_location)
        tvSelectedStationAvailability = view.findViewById(R.id.tv_selected_station_availability)
        btnChangeStation = view.findViewById(R.id.btn_change_station)
        
        // Next button
        btnNext = view.findViewById(R.id.btn_next)
    }
    
    private fun setupStationSearch() {
        // Setup search adapter
        stationSearchAdapter = ChargingStationSearchAdapter { station ->
            selectStation(station)
        }
        
        rvStationSuggestions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stationSearchAdapter
        }
        
        // Setup search text watcher
        etStationSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                
                searchJob?.cancel()
                
                if (query.isEmpty()) {
                    hideSuggestions()
                    btnClearStationSearch.visibility = View.GONE
                } else {
                    btnClearStationSearch.visibility = View.VISIBLE
                    searchJob = lifecycleScope.launch {
                        delay(300) // Debounce search
                        searchStations(query)
                    }
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Setup focus listener
        etStationSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && etStationSearch.text.isNotEmpty()) {
                searchStations(etStationSearch.text.toString().trim())
            } else if (!hasFocus) {
                lifecycleScope.launch {
                    delay(200) // Small delay to allow item clicks
                    hideSuggestions()
                }
            }
        }
        
        // Clear search button
        btnClearStationSearch.setOnClickListener {
            etStationSearch.text.clear()
            hideSuggestions()
        }
    }
    
    private fun searchStations(query: String) {
        if (query.length < 2) return
        
        lifecycleScope.launch {
            try {
                // Show loading state for search
                showSearchLoading(true)
                
                val result = chargingStationRepository.searchStations(query, null, null)
                result.onSuccess { stations ->
                    showSearchLoading(false)
                    if (stations.isNotEmpty()) {
                        // Filter only active and available stations
                        val availableStations = stations.filter { station ->
                            station.isActive && (station.availableSlots ?: 0) > 0
                        }
                        
                        if (availableStations.isNotEmpty()) {
                            showSuggestions(availableStations)
                        } else {
                            hideSuggestions()
                            showToast("No available stations found for '$query'")
                        }
                    } else {
                        hideSuggestions()
                        showToast("No stations found for '$query'")
                    }
                }.onFailure { exception ->
                    showSearchLoading(false)
                    hideSuggestions()
                    showToast("Search failed: ${exception.message}")
                    Log.e("BookingFragment", "Search error", exception)
                }
            } catch (e: Exception) {
                showSearchLoading(false)
                hideSuggestions()
                showToast("Search error occurred")
                Log.e("BookingFragment", "Search exception", e)
            }
        }
    }
    
    private fun showSearchLoading(loading: Boolean) {
        // You can add a small progress indicator in the search box if needed
        // For now, we'll just disable the search field during loading
        etStationSearch.isEnabled = !loading
    }
    
    private fun showSuggestions(stations: List<ChargingStationDto>) {
        stationSearchAdapter.updateStations(stations)
        cardStationSuggestions.visibility = View.VISIBLE
    }
    
    private fun hideSuggestions() {
        cardStationSuggestions.visibility = View.GONE
    }
    
    private fun selectStation(station: ChargingStationDto) {
        selectedStation = station
        
        // Update search field and clear focus
        etStationSearch.setText(station.name)
        etStationSearch.clearFocus()
        
        // Hide suggestions
        hideSuggestions()
        
        // Show selected station display
        displaySelectedStation(station)
        
        // Enable Next button
        enableNextButton()
        
        // Show confirmation message
        android.widget.Toast.makeText(
            context,
            "Selected: ${station.name}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun displaySelectedStation(station: ChargingStationDto) {
        tvSelectedStationName.text = station.name
        tvSelectedStationLocation.text = station.getParsedAddress()
        
        val availableSlots = station.availableSlots ?: 0
        val totalSlots = station.totalSlots ?: 0
        
        // Color code availability status
        val availabilityText = "$availableSlots/$totalSlots slots available"
        tvSelectedStationAvailability.text = availabilityText
        
        // Set availability text color based on availability
        val color = when {
            availableSlots == 0 -> android.R.color.holo_red_dark
            availableSlots <= totalSlots * 0.3 -> android.R.color.holo_orange_dark
            else -> com.example.evchargingapp.R.color.status_available
        }
        tvSelectedStationAvailability.setTextColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), color)
        )
        
        // Show additional station info if available
        val stationType = if (station.type.isNotEmpty()) " • ${station.type}" else ""
        val statusText = if (station.isActive) "Active$stationType" else "Inactive$stationType"
        
        // You can add this to a subtitle text view if you have one in your layout
        // tvSelectedStationStatus.text = statusText
        
        layoutSelectedStation.visibility = View.VISIBLE
    }
    
    private fun clearStationSelection() {
        selectedStation = null
        etStationSearch.text.clear()
        layoutSelectedStation.visibility = View.GONE
        hideSuggestions()
        
        // Disable Next button
        disableNextButton()
    }
    
    private fun setupClickListeners() {
        btnNext.setOnClickListener {
            navigateToNewReservation()
        }
        
        btnChangeStation.setOnClickListener {
            clearStationSelection()
        }
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun enableNextButton() {
        btnNext.isEnabled = true
        btnNext.alpha = 1.0f
    }
    
    private fun disableNextButton() {
        btnNext.isEnabled = false
        btnNext.alpha = 0.5f
    }
    
    private fun navigateToNewReservation() {
        // Validation
        if (selectedStation == null) {
            showToast("Please select a charging station")
            etStationSearch.requestFocus()
            return
        }
        
        selectedStation?.let { station ->
            // Additional validation
            if (station.id.isNullOrEmpty()) {
                showToast("Invalid station data. Please select another station.")
                return
            }
            
            if ((station.availableSlots ?: 0) <= 0) {
                showToast("No available slots at this station. Please select another station.")
                return
            }
            
            // Show loading state
            btnNext.isEnabled = false
            btnNext.text = "Loading..."
            
            // Validate station availability in real-time before navigation
            lifecycleScope.launch {
                try {
                    val refreshedResult = chargingStationRepository.getStationById(station.id!!)
                    refreshedResult.onSuccess { refreshedStation ->
                        if ((refreshedStation.availableSlots ?: 0) > 0) {
                            // Navigate with refreshed data
                            val intent = com.example.evchargingapp.ui.booking.NewReservationActivity.newIntent(
                                requireContext(),
                                refreshedStation.id ?: "",
                                refreshedStation.name ?: "",
                                refreshedStation.getParsedAddress(),
                                refreshedStation.availableSlots ?: 0
                            )
                            startActivity(intent)
                            showToast("Opening booking form for ${refreshedStation.name}")
                        } else {
                            showToast("This station is no longer available. Please select another station.")
                            clearStationSelection()
                        }
                    }.onFailure { exception ->
                        showToast("Unable to verify station availability: ${exception.message}")
                    }
                } catch (e: Exception) {
                    showToast("Error checking station availability")
                } finally {
                    // Reset button state
                    btnNext.isEnabled = true
                    btnNext.text = "Next →"
                }
            }
        }
    }
}