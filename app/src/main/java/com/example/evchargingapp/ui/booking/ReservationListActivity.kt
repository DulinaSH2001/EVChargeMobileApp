package com.example.evchargingapp.ui.booking

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.*
import com.example.evchargingapp.data.repository.BookingRepository
import com.example.evchargingapp.ui.booking.adapter.BookingAdapter
import com.example.evchargingapp.utils.ApiConfig
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ReservationListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ReservationListActivity"
    }

    private lateinit var bookingRepository: BookingRepository
    private lateinit var bookingAdapter: BookingAdapter

    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rvBookings: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var fabNewBooking: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_list)

        initializeComponents()
        setupViews()
        loadBookings()
    }

    private fun initializeComponents() {
        val authenticatedRetrofit = ApiConfig.getAuthenticatedRetrofit(this)
        val apiService = authenticatedRetrofit.create(BookingApiService::class.java)
        bookingRepository = BookingRepository(apiService)
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        rvBookings = findViewById(R.id.rv_bookings)
        progressBar = findViewById(R.id.progress_bar)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        fabNewBooking = findViewById(R.id.fab_new_booking)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Setup RecyclerView
        bookingAdapter = BookingAdapter(
            onEditClick = { booking -> editBooking(booking) },
            onCancelClick = { booking -> showCancelConfirmation(booking) },
            onItemClick = { booking -> showBookingDetails(booking) }
        )

        rvBookings.apply {
            layoutManager = LinearLayoutManager(this@ReservationListActivity)
            adapter = bookingAdapter
        }

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            loadBookings()
        }

        // Setup FAB
        fabNewBooking.setOnClickListener {
            // Navigate to main activity to select a station
            Toast.makeText(this, "Please select a charging station to create a booking", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadBookings() {
        Log.d(TAG, "Loading user bookings")
        showLoading(true)

        lifecycleScope.launch {
            try {
                bookingRepository.getMyBookings()
                    .onSuccess { bookings ->
                        Log.d(TAG, "Loaded ${bookings.size} bookings")
                        showLoading(false)
                        updateUI(bookings)
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Failed to load bookings", exception)
                        showLoading(false)
                        showError("Failed to load bookings: ${exception.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading bookings", e)
                showLoading(false)
                showError("Error loading bookings")
            }
        }
    }

    private fun editBooking(booking: BookingDto) {
        if (booking.status == "PENDING" || booking.status == "APPROVED") {
            val intent = Intent(this, EditReservationActivity::class.java).apply {
                putExtra("booking_id", booking.id)
                putExtra("booking_data", booking)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Cannot edit ${booking.status.lowercase()} booking", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCancelConfirmation(booking: BookingDto) {
        if (booking.status != "PENDING" && booking.status != "APPROVED") {
            Toast.makeText(this, "Cannot cancel ${booking.status.lowercase()} booking", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setPositiveButton("Cancel Booking") { _, _ ->
                cancelBooking(booking)
            }
            .setNegativeButton("Keep Booking", null)
            .show()
    }

    private fun cancelBooking(booking: BookingDto) {
        Log.d(TAG, "Cancelling booking: ${booking.id}")
        showLoading(true)

        lifecycleScope.launch {
            try {
                bookingRepository.cancelBooking(booking.id)
                    .onSuccess { cancelled ->
                        Log.d(TAG, "Booking cancelled successfully")
                        showLoading(false)
                        if (cancelled) {
                            Toast.makeText(this@ReservationListActivity, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                            loadBookings() // Refresh the list
                        } else {
                            showError("Failed to cancel booking")
                        }
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Failed to cancel booking", exception)
                        showLoading(false)
                        showError("Failed to cancel booking: ${exception.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while cancelling booking", e)
                showLoading(false)
                showError("Error cancelling booking")
            }
        }
    }

    private fun showBookingDetails(booking: BookingDto) {
        // For now, just show a toast with booking details
        // You can implement a detailed view dialog or activity later
        val details = """
            Booking ID: ${booking.id}
            Station: ${booking.chargingStationName ?: "Unknown"}
            Slot: ${booking.slotNumber}
            Date/Time: ${booking.reservationDateTime}
            Duration: ${booking.duration} minutes
            Status: ${booking.status}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Booking Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateUI(bookings: List<BookingDto>) {
        if (bookings.isEmpty()) {
            rvBookings.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            rvBookings.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            bookingAdapter.submitList(bookings)
        }
    }

    private fun showLoading(loading: Boolean) {
        swipeRefreshLayout.isRefreshing = loading
        progressBar.visibility = if (loading && bookingAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh bookings when returning to this activity
        loadBookings()
    }
}