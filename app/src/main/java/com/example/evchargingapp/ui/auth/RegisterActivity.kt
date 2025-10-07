package com.example.evchargingapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.example.evchargingapp.R
import com.example.evchargingapp.data.local.User
import com.example.evchargingapp.data.repository.AuthRepository
import com.example.evchargingapp.data.repository.AuthResult
import com.example.evchargingapp.ui.dashboard.MainActivity
import com.example.evchargingapp.utils.SessionManager
import com.example.evchargingapp.utils.OfflineManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var nicInput: EditText
    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var addressInput: EditText
    private lateinit var dateOfBirthInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmInput: EditText
    private lateinit var termsCheckbox: CheckBox
    private lateinit var progressBar: ProgressBar
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnLogin: TextView
    private lateinit var statusText: TextView
    
    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var offlineManager: OfflineManager
    
    companion object {
        private val NIC_PATTERN = Pattern.compile("^[0-9]{9}[VvXx]$|^[0-9]{12}$")
        private val PHONE_PATTERN = Pattern.compile("^[0-9]{10}$")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        initializeComponents()
        setupListeners()
        checkOfflineStatus()
    }
    
    private fun initializeComponents() {
        // Initialize views
        nicInput = findViewById(R.id.input_nic)
        firstNameInput = findViewById(R.id.input_first_name)
        lastNameInput = findViewById(R.id.input_last_name)
        emailInput = findViewById(R.id.input_email)
        phoneInput = findViewById(R.id.input_phone)
        addressInput = findViewById(R.id.input_address)
        dateOfBirthInput = findViewById(R.id.input_date_of_birth)
        passwordInput = findViewById(R.id.input_password)
        confirmInput = findViewById(R.id.input_confirm_password)
        termsCheckbox = findViewById(R.id.checkbox_terms)
        progressBar = findViewById(R.id.progress_bar)
        btnRegister = findViewById(R.id.btn_register)
        btnLogin = findViewById(R.id.btn_login)
        statusText = findViewById(R.id.status_text)
        
        // Initialize components
        sessionManager = SessionManager(this)
        authRepository = AuthRepository(this)
        offlineManager = OfflineManager(this)
    }
    
    private fun setupListeners() {
        btnRegister.setOnClickListener { register() }
        btnLogin.setOnClickListener { finish() }
        
        // Date picker for date of birth
        dateOfBirthInput.setOnClickListener { showDatePicker() }
    }
    
    private fun checkOfflineStatus() {
        offlineManager.showOfflineNotification { message ->
            statusText.text = message
            statusText.visibility = View.VISIBLE
        }
    }
    
    private fun register() {
        val formData = gatherFormData()
        if (!validateInputs(formData)) return
        
        setLoadingState(true)
        lifecycleScope.launch {
            try {
                val result = authRepository.registerEvOwnerWithDetails(
                    nic = formData.nic,
                    firstName = formData.firstName,
                    lastName = formData.lastName,
                    email = formData.email,
                    password = formData.password,
                    phone = formData.phone,
                    address = formData.address,
                    dateOfBirth = dateOfBirthInput.tag?.toString() ?: formData.dateOfBirth
                )
                
                when (result) {
                    is AuthResult.Success -> handleRegistrationSuccess(result.user, result.message)
                    is AuthResult.Failure -> handleRegistrationFailure(result.error)
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Registration error", e)
                handleRegistrationFailure("Registration failed: ${e.message}")
            } finally {
                setLoadingState(false)
            }
        }
    }
    
    private fun gatherFormData(): FormData {
        return FormData(
            nic = nicInput.text.toString().trim(),
            firstName = firstNameInput.text.toString().trim(),
            lastName = lastNameInput.text.toString().trim(),
            email = emailInput.text.toString().trim(),
            phone = phoneInput.text.toString().trim(),
            address = addressInput.text.toString().trim(),
            dateOfBirth = dateOfBirthInput.text.toString().trim(),
            password = passwordInput.text.toString(),
            confirmPassword = confirmInput.text.toString()
        )
    }
    
    private data class FormData(
        val nic: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val phone: String,
        val address: String,
        val dateOfBirth: String,
        val password: String,
        val confirmPassword: String
    )
    
    private fun validateInputs(data: FormData): Boolean {
        val validations = listOf(
            ValidationRule(data.firstName, firstNameInput, "First name required") { it.length >= 2 },
            ValidationRule(data.lastName, lastNameInput, "Last name required") { it.length >= 2 },
            ValidationRule(data.email, emailInput, "Email required") { Patterns.EMAIL_ADDRESS.matcher(it).matches() },
            ValidationRule(data.nic, nicInput, "NIC required") { NIC_PATTERN.matcher(it).matches() },
            ValidationRule(data.phone, phoneInput, "Phone required") { PHONE_PATTERN.matcher(it).matches() },
            ValidationRule(data.address, addressInput, "Address required") { it.length >= 10 },
            ValidationRule(data.dateOfBirth, dateOfBirthInput, "Date of birth required") { it.isNotEmpty() },
            ValidationRule(data.password, passwordInput, "Password required") { it.length >= 6 }
        )
        
        for (rule in validations) {
            if (!rule.validate()) return false
        }
        
        if (data.password != data.confirmPassword) {
            confirmInput.error = "Passwords do not match"
            confirmInput.requestFocus()
            return false
        }
        
        if (!termsCheckbox.isChecked) {
            Toast.makeText(this, "Please agree to Terms and Conditions", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private data class ValidationRule(
        val value: String,
        val view: EditText,
        val errorMessage: String,
        val validator: (String) -> Boolean
    ) {
        fun validate(): Boolean {
            return when {
                value.isEmpty() -> {
                    view.error = errorMessage
                    view.requestFocus()
                    false
                }
                !validator(value) -> {
                    view.error = getSpecificError()
                    view.requestFocus()
                    false
                }
                else -> true
            }
        }
        
        private fun getSpecificError(): String {
            return when (view.id) {
                R.id.input_first_name, R.id.input_last_name -> "Must be at least 2 characters"
                R.id.input_email -> "Enter valid email address"
                R.id.input_nic -> "Enter valid NIC (9 digits + V/X or 12 digits)"
                R.id.input_phone -> "Enter valid 10-digit phone number"
                R.id.input_address -> "Please enter a complete address"
                R.id.input_password -> "Password must be at least 6 characters"
                else -> errorMessage
            }
        }
    }

    
    private fun handleRegistrationSuccess(user: User, message: String?) {
        Log.d("RegisterActivity", "Registration successful for user: ${user.name}")
        
        // Save session
        sessionManager.saveLogin(user)
        
        // Show success message
        val successMessage = message ?: "Account created successfully!"
        Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show()
        
        // Navigate to main activity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun handleRegistrationFailure(error: String) {
        Log.d("RegisterActivity", "Registration failed: $error")
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        passwordInput.text.clear()
        confirmInput.text.clear()
    }
    
    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date of Birth")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = Date(selection)
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val displayFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
            dateOfBirthInput.setText(displayFormatter.format(date))
            // Store the ISO format for API
            dateOfBirthInput.tag = formatter.format(date)
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        val enabled = !isLoading
        listOf(
            btnRegister, btnLogin, nicInput, firstNameInput, lastNameInput,
            emailInput, phoneInput, addressInput, dateOfBirthInput,
            passwordInput, confirmInput, termsCheckbox
        ).forEach { it.isEnabled = enabled }
        btnRegister.text = if (isLoading) "Creating Account..." else "Create Account"
    }
    
    override fun onResume() {
        super.onResume()
        // Update offline status
        checkOfflineStatus()
    }
}