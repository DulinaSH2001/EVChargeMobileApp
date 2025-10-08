package com.example.evchargingapp.ui.operator

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.*
import com.example.evchargingapp.data.repository.OperatorRepository
import com.example.evchargingapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class OperatorProfileActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var operatorRepository: OperatorRepository
    
    // UI Components
    private lateinit var tvEmail: TextView
    private lateinit var tvRole: TextView
    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etStationId: TextInputEditText
    private lateinit var btnUpdate: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: ProgressBar
    
    private var currentProfile: OperatorUser? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_profile)
        
        sessionManager = SessionManager(this)
        operatorRepository = OperatorRepository(this)
        
        initViews()
        setupClickListeners()
        loadProfileData()
    }
    
    private fun initViews() {
        tvEmail = findViewById(R.id.tv_email)
        tvRole = findViewById(R.id.tv_role)
        etName = findViewById(R.id.et_name)
        etPhone = findViewById(R.id.et_phone)
        etStationId = findViewById(R.id.et_station_id)
        btnUpdate = findViewById(R.id.btn_update)
        btnCancel = findViewById(R.id.btn_cancel)
        progressBar = findViewById(R.id.progress_bar)
    }
    
    private fun setupClickListeners() {
        btnUpdate.setOnClickListener {
            updateProfile()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun loadProfileData() {
        // Load basic data from session
        val currentUser = sessionManager.getCurrentUser() ?: return
        tvEmail.text = currentUser.email
        tvRole.text = currentUser.role.value.replace("_", " ")
        etName.setText(currentUser.name)
        etPhone.setText(currentUser.phone)
        
        // Try to load additional data from server
        setLoadingState(true)
        lifecycleScope.launch {
            when (val result = operatorRepository.getOperatorProfile()) {
                is OperatorResult.ProfileResult -> {
                    currentProfile = result.profile
                    populateProfile(result.profile)
                }
                is OperatorResult.Failure -> {
                    showToast("Could not load full profile: ${result.error}")
                    // Continue with session data
                }
                else -> {
                    showToast("Unexpected result loading profile")
                }
            }
            setLoadingState(false)
        }
    }
    
    private fun populateProfile(profile: OperatorUser) {
        etName.setText(profile.name ?: "")
        etStationId.setText(profile.stationId ?: "")
        tvEmail.text = profile.email
        tvRole.text = profile.role.replace("_", " ")
    }
    
    private fun updateProfile() {
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val stationId = etStationId.text.toString().trim()
        
        // Basic validation
        if (name.isEmpty()) {
            etName.error = "Name is required"
            etName.requestFocus()
            return
        }
        
        if (phone.isNotEmpty() && phone.length < 10) {
            etPhone.error = "Please enter a valid phone number"
            etPhone.requestFocus()
            return
        }
        
        setLoadingState(true)
        lifecycleScope.launch {
            when (val result = operatorRepository.updateOperatorProfile(
                name = name.ifEmpty { null },
                phone = phone.ifEmpty { null },
                stationId = stationId.ifEmpty { null }
            )) {
                is OperatorResult.ProfileResult -> {
                    showToast("Profile updated successfully!")
                    
                    // Update session data
                    val currentUser = sessionManager.getCurrentUser() ?: return@launch
                    val updatedUser = currentUser.copy(
                        name = result.profile.name ?: name,
                        phone = phone
                    )
                    sessionManager.saveLogin(updatedUser, sessionManager.getToken())
                    
                    // Delay and close
                    android.os.Handler(mainLooper).postDelayed({
                        finish()
                    }, 1500)
                }
                is OperatorResult.Failure -> {
                    showToast("Error: ${result.error}")
                }
                else -> {
                    showToast("Unexpected result")
                }
            }
            setLoadingState(false)
        }
    }
    
    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnUpdate.isEnabled = !isLoading
        btnCancel.isEnabled = !isLoading
        etName.isEnabled = !isLoading
        etPhone.isEnabled = !isLoading
        etStationId.isEnabled = !isLoading
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}