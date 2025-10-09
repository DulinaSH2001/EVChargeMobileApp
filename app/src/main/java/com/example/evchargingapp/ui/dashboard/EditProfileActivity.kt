package com.example.evchargingapp.ui.dashboard

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.*
import com.example.evchargingapp.data.repository.EVOwnerRepository
import com.example.evchargingapp.utils.ApiConfig
import com.example.evchargingapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditProfileActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var evOwnerRepository: EVOwnerRepository
    
    // Input fields
    private lateinit var tilFirstName: TextInputLayout
    private lateinit var etFirstName: TextInputEditText
    private lateinit var tilLastName: TextInputLayout
    private lateinit var etLastName: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPhone: TextInputLayout
    private lateinit var etPhone: TextInputEditText
    private lateinit var tilAddress: TextInputLayout
    private lateinit var etAddress: TextInputEditText
    private lateinit var tilDateOfBirth: TextInputLayout
    private lateinit var etDateOfBirth: TextInputEditText
    
    // Buttons
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    
    // Current user data
    private var currentEVOwner: EVOwnerDto? = null
    private var userNic: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        
        initViews()
        setupRepository()
        setupClickListeners()
        fetchUserData()
    }
    
    private fun initViews() {
        sessionManager = SessionManager(this)
        
        // Initialize input fields
        tilFirstName = findViewById(R.id.til_first_name)
        etFirstName = findViewById(R.id.et_first_name)
        tilLastName = findViewById(R.id.til_last_name)
        etLastName = findViewById(R.id.et_last_name)
        tilEmail = findViewById(R.id.til_email)
        etEmail = findViewById(R.id.et_email)
        tilPhone = findViewById(R.id.til_phone)
        etPhone = findViewById(R.id.et_phone)
        tilAddress = findViewById(R.id.til_address)
        etAddress = findViewById(R.id.et_address)
        tilDateOfBirth = findViewById(R.id.til_date_of_birth)
        etDateOfBirth = findViewById(R.id.et_date_of_birth)
        
        // Initialize buttons
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
        
        // Setup toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"
    }
    
    private fun setupRepository() {
        val apiService = ApiConfig.getAuthenticatedRetrofit(this).create(EVOwnerApiService::class.java)
        evOwnerRepository = EVOwnerRepository(apiService)
        userNic = sessionManager.getActualNic()
    }
    
    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveProfile()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
        
        // Date picker for date of birth
        etDateOfBirth.setOnClickListener {
            showDatePicker()
        }
        
        // Disable text input for date field
        etDateOfBirth.isFocusable = false
        etDateOfBirth.isClickable = true
    }
    
    private fun fetchUserData() {
        val nic = userNic
        if (nic == null) {
            // Try to get NIC from EV Owner profile if not available directly
            val evOwnerProfile = sessionManager.getEVOwnerProfile()
            val fallbackNic = evOwnerProfile?.nic
            
            if (fallbackNic == null) {
                showError("User session not found. Please log in again.")
                finish()
                return
            } else {
                // Use the NIC from the EV Owner profile
                fetchUserDataWithNic(fallbackNic)
                return
            }
        }
        
        fetchUserDataWithNic(nic)
    }
    
    private fun fetchUserDataWithNic(nic: String) {
        lifecycleScope.launch {
            try {
                showLoading(true)
                val result = evOwnerRepository.getEVOwnerByNIC(nic)
                result.onSuccess { evOwner ->
                    currentEVOwner = evOwner
                    populateFields(evOwner)
                    showLoading(false)
                }.onFailure { error ->
                    showLoading(false)
                    showError("Failed to load profile: ${error.message}")
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("Failed to load profile: ${e.message}")
            }
        }
    }
    
    private fun populateFields(evOwner: EVOwnerDto) {
        etFirstName.setText(evOwner.firstName)
        etLastName.setText(evOwner.lastName)
        etEmail.setText(evOwner.email)
        etPhone.setText(evOwner.phone)
        etAddress.setText(evOwner.address ?: "")
        
        // Format date of birth for display
        evOwner.dateOfBirth?.let { dateString ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { etDateOfBirth.setText(outputFormat.format(it)) }
            } catch (e: Exception) {
                // Fallback for different date formats
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = inputFormat.parse(dateString)
                    date?.let { etDateOfBirth.setText(outputFormat.format(it)) }
                } catch (e: Exception) {
                    etDateOfBirth.setText("")
                }
            }
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        // Try to parse current date if available
        currentEVOwner?.dateOfBirth?.let { dateString ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { calendar.time = it }
            } catch (e: Exception) {
                // Use current date if parsing fails
            }
        }
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                etDateOfBirth.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun saveProfile() {
        if (!validateInputs()) return
        
        var nic = userNic
        if (nic == null) {
            // Try to get NIC from EV Owner profile if not available directly
            val evOwnerProfile = sessionManager.getEVOwnerProfile()
            nic = evOwnerProfile?.nic
            
            if (nic == null) {
                showError("User session not found. Please log in again.")
                return
            }
        }
        
        // Convert date of birth to API format
        val dateOfBirthFormatted = formatDateForAPI(etDateOfBirth.text.toString())
        
        val updateRequest = UpdateEVOwnerRequest(
            firstName = etFirstName.text.toString().trim(),
            lastName = etLastName.text.toString().trim(),
            email = etEmail.text.toString().trim(),
            phone = etPhone.text.toString().trim(),
            address = etAddress.text.toString().trim().takeIf { it.isNotEmpty() },
            dateOfBirth = dateOfBirthFormatted
        )
        
        lifecycleScope.launch {
            try {
                showLoading(true)
                val result = evOwnerRepository.updateEVOwner(nic, updateRequest)
                result.onSuccess { updatedEvOwner ->
                    showLoading(false)
                    // Update session manager with new data
                    sessionManager.updateEVOwnerProfile(updatedEvOwner)
                    showSuccess("Profile updated successfully")
                    setResult(RESULT_OK)
                    finish()
                }.onFailure { error ->
                    showLoading(false)
                    showError("Failed to update profile: ${error.message}")
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("Failed to update profile: ${e.message}")
            }
        }
    }
    
    private fun formatDateForAPI(dateString: String): String? {
        if (dateString.isEmpty()) return null
        
        try {
            val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            apiFormat.timeZone = TimeZone.getTimeZone("UTC")
            
            val date = displayFormat.parse(dateString)
            return date?.let { apiFormat.format(it) }
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Clear previous errors
        tilFirstName.error = null
        tilLastName.error = null
        tilEmail.error = null
        tilPhone.error = null
        
        // Validate first name
        if (etFirstName.text.toString().trim().isEmpty()) {
            tilFirstName.error = "First name is required"
            isValid = false
        }
        
        // Validate last name
        if (etLastName.text.toString().trim().isEmpty()) {
            tilLastName.error = "Last name is required"
            isValid = false
        }
        
        // Validate email
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Please enter a valid email"
            isValid = false
        }
        
        // Validate phone
        val phone = etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            tilPhone.error = "Phone number is required"
            isValid = false
        } else if (phone.length < 10) {
            tilPhone.error = "Please enter a valid phone number"
            isValid = false
        }
        
        return isValid
    }
    
    private fun showLoading(show: Boolean) {
        btnSave.isEnabled = !show
        btnSave.text = if (show) "Saving..." else "Save Changes"
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}