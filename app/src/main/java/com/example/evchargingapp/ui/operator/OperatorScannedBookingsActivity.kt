package com.example.evchargingapp.ui.operator

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.evchargingapp.R
import com.example.evchargingapp.data.local.ScannedBookingEntity
import com.example.evchargingapp.data.repository.OperatorRepository
import com.example.evchargingapp.data.repository.ScannedBookingRepository
import com.example.evchargingapp.data.api.OperatorResult
import com.example.evchargingapp.ui.operator.adapter.ScannedBookingAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class OperatorScannedBookingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OperatorScannedBookings"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var rvScannedBookings: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvInprogressCount: TextView
    private lateinit var tvCompletedCount: TextView

    private lateinit var scannedBookingRepository: ScannedBookingRepository
    private lateinit var operatorRepository: OperatorRepository
    private lateinit var adapter: ScannedBookingAdapter

    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_scanned_bookings)

        initViews()
        initRepositories()
        setupToolbar()
        setupRecyclerView()
        setupTabLayout()
        setupSwipeRefresh()
        observeBookings()
        loadStatistics()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        tabLayout = findViewById(R.id.tab_layout)
        rvScannedBookings = findViewById(R.id.rv_scanned_bookings)
        layoutEmptyState = findViewById(R.id.layout_empty_state)
        progressBar = findViewById(R.id.progress_bar)
        tvInprogressCount = findViewById(R.id.tv_inprogress_count)
        tvCompletedCount = findViewById(R.id.tv_completed_count)
    }

    private fun initRepositories() {
        scannedBookingRepository = ScannedBookingRepository(this)
        operatorRepository = OperatorRepository(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ScannedBookingAdapter { booking ->
            completeBooking(booking)
        }
        
        rvScannedBookings.layoutManager = LinearLayoutManager(this)
        rvScannedBookings.adapter = adapter
    }

    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("All"))
        tabLayout.addTab(tabLayout.newTab().setText("In Progress"))
        tabLayout.addTab(tabLayout.newTab().setText("Completed"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    0 -> "All"
                    1 -> "InProgress"
                    2 -> "Completed"
                    else -> "All"
                }
                observeBookings()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadStatistics()
            observeBookings()
            swipeRefresh.isRefreshing = false
        }
    }

    private fun observeBookings() {
        lifecycleScope.launch {
            val flow = when (currentFilter) {
                "All" -> scannedBookingRepository.getAllScannedBookings()
                else -> scannedBookingRepository.getBookingsByStatus(currentFilter)
            }

            flow.collect { bookings ->
                updateUI(bookings)
            }
        }
    }

    private fun updateUI(bookings: List<ScannedBookingEntity>) {
        if (bookings.isEmpty()) {
            rvScannedBookings.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            rvScannedBookings.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
            adapter.submitList(bookings)
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val inProgressCount = scannedBookingRepository.getInProgressBookingsCount()
                val completedCount = scannedBookingRepository.getCompletedBookingsCount()

                tvInprogressCount.text = inProgressCount.toString()
                tvCompletedCount.text = completedCount.toString()
            } catch (e: Exception) {
                showToast("Error loading statistics: ${e.message}")
            }
        }
    }

    private fun completeBooking(booking: ScannedBookingEntity) {
        setLoadingState(true)
        
        lifecycleScope.launch {
            try {
                // Update status via API
                when (val result = operatorRepository.updateBookingStatusToCompleted(booking.bookingId)) {
                    is OperatorResult.StatusUpdateSuccess -> {
                        // Update local database
                        scannedBookingRepository.updateBookingStatus(booking.bookingId, "Completed")
                        showToast("Booking marked as completed successfully")
                        loadStatistics()
                    }
                    is OperatorResult.Failure -> {
                        // Still update local database even if API fails (offline capability)
                        scannedBookingRepository.updateBookingStatus(booking.bookingId, "Completed")
                        showToast("Booking updated locally. Will sync when online.")
                        loadStatistics()
                    }
                    else -> {
                        showToast("Unexpected result")
                    }
                }
            } catch (e: Exception) {
                // Fallback to local update only
                try {
                    scannedBookingRepository.updateBookingStatus(booking.bookingId, "Completed")
                    showToast("Booking updated locally. Error: ${e.message}")
                    loadStatistics()
                } catch (localError: Exception) {
                    showToast("Failed to update booking: ${localError.message}")
                }
            } finally {
                setLoadingState(false)
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        swipeRefresh.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}