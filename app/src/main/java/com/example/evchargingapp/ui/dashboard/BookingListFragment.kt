package com.example.evchargingapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.evchargingapp.R
import com.example.evchargingapp.data.local.Booking
import com.example.evchargingapp.data.local.BookingStatus
import com.example.evchargingapp.utils.SessionManager
import com.google.android.material.button.MaterialButton

class BookingListFragment : Fragment() {
    
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var loadingState: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyMessage: TextView
    private lateinit var btnCreateBooking: MaterialButton
    
    private lateinit var sessionManager: SessionManager
    private lateinit var bookingAdapter: BookingAdapter
    
    private var bookingStatus: BookingStatus? = null
    
    companion object {
        private const val ARG_STATUS = "booking_status"
        
        fun newInstance(status: BookingStatus): BookingListFragment {
            val fragment = BookingListFragment()
            val args = Bundle()
            args.putString(ARG_STATUS, status.name)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val statusName = it.getString(ARG_STATUS)
            bookingStatus = statusName?.let { name -> BookingStatus.valueOf(name) }
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
            mutableListOf(),
            onItemClick = { booking ->
                // Navigate to booking details
                showBookingDetails(booking)
            },
            onCancelClick = { booking ->
                // Handle booking cancellation
                cancelBooking(booking)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bookingAdapter
        }
    }
    
    private fun setupEmptyState() {
        when (bookingStatus) {
            BookingStatus.PENDING -> {
                tvEmptyTitle.text = "No Pending Bookings"
                tvEmptyMessage.text = "You don't have any pending bookings at the moment."
            }
            BookingStatus.APPROVED -> {
                tvEmptyTitle.text = "No Approved Bookings"
                tvEmptyMessage.text = "You don't have any approved bookings at the moment."
            }
            BookingStatus.PAST -> {
                tvEmptyTitle.text = "No Past Bookings"
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
        
        // Simulate loading with mock data
        val mockBookings = generateMockBookings()
        val filteredBookings = mockBookings.filter { 
            bookingStatus == null || it.status == bookingStatus 
        }
        
        // Simulate network delay
        view?.postDelayed({
            showContent(filteredBookings)
        }, 1000)
    }
    
    private fun generateMockBookings(): List<Booking> {
        val currentUser = sessionManager.getUserName()
        return listOf(
            Booking(
                "1",
                "Central Station",
                "123 Main St, Downtown",
                "2024-01-15",
                "14:30",
                "2 hours",
                BookingStatus.PENDING,
                currentUser
            ),
            Booking(
                "2",
                "Mall Charging Hub",
                "456 Shopping Blvd",
                "2024-01-10",
                "10:00",
                "1.5 hours",
                BookingStatus.APPROVED,
                currentUser
            ),
            Booking(
                "3",
                "Airport Terminal",
                "789 Airport Rd",
                "2024-01-05",
                "16:45",
                "3 hours",
                BookingStatus.PAST,
                currentUser
            ),
            Booking(
                "4",
                "Office Complex",
                "321 Business Ave",
                "2024-01-12",
                "09:15",
                "4 hours",
                BookingStatus.APPROVED,
                currentUser
            )
        )
    }
    
    private fun showLoadingState() {
        swipeRefresh.isRefreshing = false
        loadingState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE
    }
    
    private fun showContent(bookings: List<Booking>) {
        loadingState.visibility = View.GONE
        
        if (bookings.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            bookingAdapter.updateBookings(bookings)
        }
    }
    
    private fun showBookingDetails(booking: Booking) {
        // TODO: Navigate to booking details screen
        // For now, show a simple message
        android.widget.Toast.makeText(
            context,
            "Viewing details for ${booking.stationName}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun cancelBooking(booking: Booking) {
        // TODO: Implement booking cancellation
        android.widget.Toast.makeText(
            context,
            "Cancelling booking at ${booking.stationName}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        
        // Refresh the list after cancellation
        loadBookings()
    }
    
    private fun navigateToNewBooking() {
        // TODO: Navigate to new booking screen
        android.widget.Toast.makeText(
            context,
            "Opening new booking form",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}