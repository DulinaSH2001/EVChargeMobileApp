package com.example.evchargingapp.ui.dashboard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.evchargingapp.R
import com.example.evchargingapp.utils.SessionManager
import com.example.evchargingapp.ui.auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var tvUserName: TextView
    private lateinit var tvPageTitle: TextView
    private lateinit var layoutWelcomeSection: LinearLayout
    private lateinit var bottomNavigation: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        sessionManager = SessionManager(this)
        
        // Apply saved dark theme preference
        applyDarkThemePreference()
        
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_main)
        
        // Enable full screen and handle system bars (after content view is set)
        enableFullScreen()
        
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
            loadFragment(EnhancedDashboardFragment.newInstance())
        }
    }
    
    private fun enableFullScreen() {
        try {
            // Enable edge-to-edge display
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            
            // Make status bar transparent for newer versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                @Suppress("DEPRECATION")
                window.statusBarColor = android.graphics.Color.TRANSPARENT
            }
            
            // Set light status bar content for newer versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        } catch (e: Exception) {
            // Fallback: just set transparent status bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                @Suppress("DEPRECATION")
                window.statusBarColor = android.graphics.Color.TRANSPARENT
            }
        }
    }
    
    private fun initViews() {
        tvUserName = findViewById(R.id.tv_user_name)
        tvPageTitle = findViewById(R.id.tv_page_title)
        layoutWelcomeSection = findViewById(R.id.layout_welcome_section)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        
        // Set user name in header
        val userName = sessionManager.getUserName()
        tvUserName.text = "Hi, $userName!"
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            val currentSelectedItemId = bottomNavigation.selectedItemId
            val animationType = getAnimationTypeForNavigation(currentSelectedItemId, item.itemId)
            
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragmentWithAnimation(EnhancedDashboardFragment.newInstance(), animationType)
                    updateHeader(showWelcome = true, pageTitle = "")
                    true
                }
                R.id.nav_create_booking -> {
                    loadFragmentWithAnimation(BookingFragment(), animationType)
                    updateHeader(showWelcome = false, pageTitle = "New Booking")
                    true
                }
                R.id.nav_my_bookings -> {
                    loadFragmentWithAnimation(MyBookingsFragment(), animationType)
                    updateHeader(showWelcome = false, pageTitle = "My Bookings")
                    true
                }
                R.id.nav_profile -> {
                    loadFragmentWithAnimation(ProfileFragment(), animationType)
                    updateHeader(showWelcome = false, pageTitle = "Profile")
                    true
                }
                else -> false
            }
        }
        
        // Set dashboard as selected by default
        bottomNavigation.selectedItemId = R.id.nav_dashboard
    }
    
    private fun getAnimationTypeForNavigation(fromItemId: Int, toItemId: Int): AnimationType {
        val navigationOrder = listOf(
            R.id.nav_dashboard,
            R.id.nav_create_booking,
            R.id.nav_my_bookings,
            R.id.nav_profile
        )
        
        val fromIndex = navigationOrder.indexOf(fromItemId)
        val toIndex = navigationOrder.indexOf(toItemId)
        
        return when {
            fromIndex == -1 || toIndex == -1 -> AnimationType.FADE
            fromIndex < toIndex -> AnimationType.SLIDE_LEFT_TO_RIGHT
            fromIndex > toIndex -> AnimationType.SLIDE_RIGHT_TO_LEFT
            else -> AnimationType.FADE
        }
    }
    
    private fun updateHeader(showWelcome: Boolean, pageTitle: String) {
        if (showWelcome) {
            // Show welcome section, hide page title
            layoutWelcomeSection.visibility = View.VISIBLE
            tvPageTitle.visibility = View.GONE
        } else {
            // Hide welcome section, show page title
            layoutWelcomeSection.visibility = View.GONE
            tvPageTitle.visibility = View.VISIBLE
            tvPageTitle.text = pageTitle
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        loadFragmentWithAnimation(fragment, AnimationType.FADE)
    }
    
    private fun loadFragmentWithAnimation(fragment: Fragment, animationType: AnimationType) {
        val transaction = supportFragmentManager.beginTransaction()
        
        when (animationType) {
            AnimationType.SLIDE_LEFT_TO_RIGHT -> {
                transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
            }
            AnimationType.SLIDE_RIGHT_TO_LEFT -> {
                transaction.setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            }
            AnimationType.FADE -> {
                transaction.setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
            }
        }
        
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }
    
    enum class AnimationType {
        SLIDE_LEFT_TO_RIGHT,
        SLIDE_RIGHT_TO_LEFT,
        FADE
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
    
    // Method to be called from EnhancedDashboardFragment for navigation
    fun navigateToBookings() {
        bottomNavigation.selectedItemId = R.id.nav_my_bookings
    }
    
    // Method to handle new booking creation
    fun createNewBooking() {
        bottomNavigation.selectedItemId = R.id.nav_create_booking
    }
    
    // Method to handle QR scanning with smooth transition
    fun scanQRCode() {
        // TODO: Open QR scanner
        android.widget.Toast.makeText(
            this,
            "Opening QR scanner...",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun applyDarkThemePreference() {
        val isDarkMode = sessionManager.isDarkThemeEnabled()
        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        
        // Only apply if it's different from current mode
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
}