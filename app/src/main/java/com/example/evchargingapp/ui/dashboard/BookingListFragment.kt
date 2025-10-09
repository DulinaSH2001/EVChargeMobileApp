package com.example.evchargingapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.BookingApiService
import com.example.evchargingapp.data.api.BookingDto
import com.example.evchargingapp.data.api.BookingSearchRequest
import com.example.evchargingapp.data.api.ChargingStationApiService
import com.example.evchargingapp.data.repository.BookingRepository
import com.example.evchargingapp.data.repository.ChargingStationRepository
import com.example.evchargingapp.ui.booking.adapter.BookingAdapter
import com.example.evchargingapp.utils.ApiConfig
import com.example.evchargingapp.data.manager.BookingDataManager
import com.example.evchargingapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class BookingListFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var loadingState: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyMessage: TextView
    private lateinit var btnCreateBooking: MaterialButton

    private lateinit var sessionManager: SessionManager
    private lateinit var bookingRepository: BookingRepository
    private lateinit var chargingStationRepository: ChargingStationRepository
    private lateinit var bookingDataManager: BookingDataManager
    private lateinit var bookingAdapter: BookingAdapter

    private var bookingStatus: String? = null

    companion object {
        private const val TAG = "BookingListFragment"
        private const val ARG_STATUS = "booking_status"

        fun newInstance(status: String): BookingListFragment {
            val fragment = BookingListFragment()
            val args = Bundle()
            args.putString(ARG_STATUS, status)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bookingStatus = it.getString(ARG_STATUS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_booking_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupEmptyState()
        loadBookings()
    }

    private fun initViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        recyclerView = view.findViewById(R.id.rv_bookings)
        emptyState = view.findViewById(R.id.empty_state)
        loadingState = view.findViewById(R.id.loading_state)
        tvEmptyTitle = view.findViewById(R.id.tv_empty_title)
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)
        btnCreateBooking = view.findViewById(R.id.btn_create_booking)

        sessionManager = SessionManager(requireContext())

        // Initialize repository
        val authenticatedRetrofit = ApiConfig.getAuthenticatedRetrofit(requireContext())
        val apiService = authenticatedRetrofit.create(BookingApiService::class.java)
        val stationApiService = authenticatedRetrofit.create(ChargingStationApiService::class.java)
        bookingRepository = BookingRepository(apiService)
        chargingStationRepository = ChargingStationRepository(stationApiService)
        
        // Initialize data manager
        // Set up data manager in companion object
        BookingDataManager.initialize(bookingRepository, chargingStationRepository)
        
        // Get the initialized instance
        bookingDataManager = BookingDataManager.getInstance()

        swipeRefresh.setOnRefreshListener {
            loadBookings()
        }

        swipeRefresh.setColorSchemeResources(
            R.color.primary_blue,
            R.color.primary_blue_dark
        )
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter(
            onEditClick = { booking ->
                // Navigate to edit booking after fetching fresh data
                val normalizedStatus = getStatusText(booking.status).uppercase()
                if (normalizedStatus in listOf("PENDING", "CONFIRMED")) {
                    fetchBookingDetailsAndEdit(booking.id)
                } else {
                    Toast.makeText(context, "This booking cannot be edited", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            onCancelClick = { booking ->
                // Handle booking cancellation
                val normalizedStatus = getStatusText(booking.status).uppercase()
                if (normalizedStatus in listOf("PENDING", "CONFIRMED")) {
                    cancelBooking(booking)
                } else {
                    Toast.makeText(context, "This booking cannot be cancelled", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            onItemClick = { booking ->
                // Navigate to booking details
                showBookingDetails(booking)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bookingAdapter
        }
    }

    private fun setupEmptyState() {
        when (bookingStatus) {
            "PENDING" -> {
                tvEmptyTitle.text = "No Pending Bookings"
                tvEmptyMessage.text = "You don't have any pending bookings at the moment."
            }

            "INPROGRESS" -> {
                tvEmptyTitle.text = "No In-Progress Bookings"
                tvEmptyMessage.text = "You don't have any bookings currently in progress."
            }

            "COMPLETED" -> {
                tvEmptyTitle.text = "No Completed Bookings"
                tvEmptyMessage.text = "You haven't completed any bookings yet."
                btnCreateBooking.visibility = View.GONE
            }

            else -> {
                tvEmptyTitle.text = "No Bookings Found"
                tvEmptyMessage.text = "You don't have any bookings yet."
            }
        }

        btnCreateBooking.setOnClickListener {
            // Navigate to new booking creation
            navigateToNewBooking()
        }
    }

    private fun loadBookings() {
        showLoadingState()
        
        lifecycleScope.launch {
            try {
                // Get all bookings from API
                val result = bookingRepository.getAllMyBookings()
                
                result.onSuccess { allBookings ->
                    Log.d(TAG, "Loaded ${allBookings.size} total bookings")
                    
                    // Filter bookings locally based on the fragment's status
                    val filteredBookings = if (bookingStatus != null) {
                        allBookings.filter { booking ->
                            val normalizedStatus = getStatusText(booking.status).uppercase()
                            normalizedStatus.equals(bookingStatus, ignoreCase = true)
                        }
                    } else {
                        allBookings
                    }
                    
                    Log.d(TAG, "Filtered ${filteredBookings.size} bookings for status: $bookingStatus")
                    showContent(filteredBookings)
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to load bookings", exception)
                    showError("Failed to load bookings: ${exception.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading bookings", e)
                showError("Error loading bookings")
            }
        }
    }    private fun showError(message: String) {
        loadingState.visibility = View.GONE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        tvEmptyTitle.text = "Error"
        tvEmptyMessage.text = message
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoadingState() {
        swipeRefresh.isRefreshing = false
        loadingState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE
    }

    private fun showContent(bookings: List<BookingDto>) {
        loadingState.visibility = View.GONE

        if (bookings.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            bookingAdapter.submitList(bookings)
        }
    }

    private fun showBookingDetails(booking: BookingDto) {
        // Navigate to BookingDetailsActivity
        com.example.evchargingapp.ui.booking.BookingDetailsActivity.start(requireContext(), booking)
    }

    private fun cancelBooking(booking: BookingDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel your booking at ${booking.chargingStationName}?")
            .setPositiveButton("Cancel Booking") { _, _ ->
                performBookingCancellation(booking)
            }
            .setNegativeButton("Keep Booking", null)
            .show()
    }

    private fun performBookingCancellation(booking: BookingDto) {
        lifecycleScope.launch {
            try {
                val result = bookingDataManager.deleteBooking(booking.id)
                result.onSuccess { success ->
                    if (success) {
                        Log.d(TAG, "Booking cancelled successfully: ${booking.id}")
                        Toast.makeText(
                            context,
                            "Booking cancelled successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadBookings() // Refresh the list
                    } else {
                        Toast.makeText(context, "Failed to cancel booking", Toast.LENGTH_SHORT)
                            .show()
                    }
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to cancel booking", exception)
                    Toast.makeText(
                        context,
                        "Failed to cancel booking: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while cancelling booking", e)
                Toast.makeText(context, "Error cancelling booking", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToNewBooking() {
        // Navigate to the booking tab by using the main activity method
        (activity as? MainActivity)?.createNewBooking()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK) {
            // Refresh the booking list after edit/cancel
            loadBookings()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh bookings when fragment becomes visible
        if (::bookingRepository.isInitialized) {
            loadBookings()
        }
    }
    
    private fun fetchBookingDetailsAndEdit(bookingId: String) {
        // Show loading indication
        Toast.makeText(context, "Loading booking details...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val result = bookingRepository.getBookingById(bookingId)
                
                result.onSuccess { bookingDetails ->
                    try {
                        // Navigate to edit with fresh booking data using the same pattern as ReservationListActivity
                        val intent = Intent(requireContext(), com.example.evchargingapp.ui.booking.EditReservationActivity::class.java).apply {
                            putExtra("booking_id", bookingId)
                            putExtra("booking_data", bookingDetails)
                        }
                        startActivityForResult(intent, 1001)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start EditReservationActivity", e)
                        Toast.makeText(
                            context,
                            "Error opening edit screen: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to fetch booking details for edit", exception)
                    Toast.makeText(
                        context,
                        "Failed to load booking details: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while fetching booking details", e)
                Toast.makeText(context, "Error loading booking details", Toast.LENGTH_SHORT).show()
            }
        }
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
}

