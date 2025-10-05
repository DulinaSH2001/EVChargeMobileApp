package com.example.evchargingapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.evchargingapp.R
import com.example.evchargingapp.utils.SessionManager
import com.example.evchargingapp.ui.auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var tvUserName: TextView
    private lateinit var bottomNavigation: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        sessionManager = SessionManager(this)
        
        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin()
            return
        }
        
        initViews()
        setupBottomNavigation()
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
    }
    
    private fun initViews() {
        tvUserName = findViewById(R.id.tv_user_name)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        
        // Set user name in header
        val userName = sessionManager.getUserName()
        tvUserName.text = "Hi, $userName!"
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_create_booking -> {
                    loadFragment(BookingFragment())
                    true
                }
                R.id.nav_my_bookings -> {
                    loadFragment(MyBookingsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
        
        // Set dashboard as selected by default
        bottomNavigation.selectedItemId = R.id.nav_dashboard
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    private fun performLogout() {
        sessionManager.logout()
        redirectToLogin()
    }
    
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    // Method to be called from DashboardFragment for navigation
    fun navigateToBookings() {
        bottomNavigation.selectedItemId = R.id.nav_my_bookings
    }
    
    // Method to handle new booking creation
    fun createNewBooking() {
        bottomNavigation.selectedItemId = R.id.nav_create_booking
    }
    
    // Method to handle QR scanning
    fun scanQRCode() {
        // TODO: Open QR scanner
        android.widget.Toast.makeText(
            this,
            "Opening QR scanner...",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}