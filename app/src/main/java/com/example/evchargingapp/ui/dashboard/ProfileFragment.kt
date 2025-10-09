package com.example.evchargingapp.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.evchargingapp.R
import com.example.evchargingapp.utils.SessionManager
import com.example.evchargingapp.ui.auth.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class ProfileFragment : Fragment() {
    
    companion object {
        private const val EDIT_PROFILE_REQUEST_CODE = 1001
    }
    
    private lateinit var sessionManager: SessionManager
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var cardEditProfile: MaterialCardView
    private lateinit var cardDarkTheme: MaterialCardView
    private lateinit var switchDarkTheme: SwitchMaterial
    private lateinit var btnLogout: MaterialButton
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupUserInfo()
        setupClickListeners()
    }
    
    private fun initViews(view: View) {
        sessionManager = SessionManager(requireContext())
        
        tvUserName = view.findViewById(R.id.tv_user_name)
        tvUserEmail = view.findViewById(R.id.tv_user_email)
        cardEditProfile = view.findViewById(R.id.card_edit_profile)
        cardDarkTheme = view.findViewById(R.id.card_dark_theme)
        switchDarkTheme = view.findViewById(R.id.switch_dark_theme)
        btnLogout = view.findViewById(R.id.btn_logout)
    }
    
    private fun setupUserInfo() {
        tvUserName.text = sessionManager.getUserName()
        tvUserEmail.text = sessionManager.getUserEmail()
        
        // Set up dark theme switch state based on saved preference
        val isDarkThemeEnabled = sessionManager.isDarkThemeEnabled()
        switchDarkTheme.isChecked = isDarkThemeEnabled
    }
    
    private fun setupClickListeners() {
        cardEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivityForResult(intent, EDIT_PROFILE_REQUEST_CODE)
        }
        
        // Dark theme toggle - handle both card click and switch
        cardDarkTheme.setOnClickListener {
            switchDarkTheme.isChecked = !switchDarkTheme.isChecked
        }
        
        // Prevent double toggle by temporarily disabling listener
        switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            toggleDarkTheme(isChecked)
        }
        
        btnLogout.setOnClickListener {
            performLogout()
        }
    }
    
    private fun performLogout() {
        sessionManager.logout()
        
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    private fun toggleDarkTheme(isDarkMode: Boolean) {
        // Save preference first
        sessionManager.saveDarkThemePreference(isDarkMode)
        
        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        
        // Debug logging
        android.util.Log.d("DarkTheme", "Applying dark theme: $isDarkMode, nightMode: $nightMode")
        
        // Apply theme globally
        AppCompatDelegate.setDefaultNightMode(nightMode)
        
        // Show confirmation
        showToast(if (isDarkMode) "Dark theme enabled" else "Light theme enabled")
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh user info when profile is updated
            setupUserInfo()
            showToast("Profile updated successfully")
        }
    }
}