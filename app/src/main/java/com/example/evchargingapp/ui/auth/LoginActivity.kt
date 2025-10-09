package com.example.evchargingapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.example.evchargingapp.ui.dashboard.MainActivity
import com.example.evchargingapp.R
import com.example.evchargingapp.data.local.User
import com.example.evchargingapp.data.local.UserType
import com.example.evchargingapp.data.repository.AuthRepository
import com.example.evchargingapp.data.repository.AuthResult
import com.example.evchargingapp.data.repository.NicValidationResult
import com.example.evchargingapp.data.repository.EVOwnerRepository
import com.example.evchargingapp.data.api.EVOwnerApiService
import com.example.evchargingapp.utils.SessionManager
import com.example.evchargingapp.utils.OfflineManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var identifierInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: TextView
    private lateinit var btnEvOwner: MaterialButton
    private lateinit var btnStationOperator: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var inputLayoutIdentifier: com.google.android.material.textfield.TextInputLayout
    private lateinit var inputLayoutPassword: com.google.android.material.textfield.TextInputLayout
    
    private var currentUserType: UserType = UserType.EV_OWNER
    
    // EV Owner two-step login state
    private var evOwnerValidatedEmail: String? = null
    private var evOwnerValidatedNic: String? = null // Store the original NIC
    private var isNicValidated: Boolean = false
    
    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var evOwnerRepository: EVOwnerRepository
    private lateinit var offlineManager: OfflineManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        initializeComponents()
        setupListeners()
        updateUIForUserType()
        checkOfflineStatus()
    }
    
    private fun initializeComponents() {
        // Initialize views
        identifierInput = findViewById(R.id.input_identifier)
        passwordInput = findViewById(R.id.input_password)
        progressBar = findViewById(R.id.progress_bar)
        btnLogin = findViewById(R.id.btn_login)
        btnRegister = findViewById(R.id.btn_register)
        btnEvOwner = findViewById(R.id.btn_ev_owner)
        btnStationOperator = findViewById(R.id.btn_station_operator)
        statusText = findViewById(R.id.status_text)
        inputLayoutIdentifier = findViewById(R.id.input_layout_identifier)
        inputLayoutPassword = findViewById(R.id.input_layout_password)
        
        // Initialize components
        sessionManager = SessionManager(this)
        authRepository = AuthRepository(this)
        evOwnerRepository = EVOwnerRepository(
            com.example.evchargingapp.utils.ApiConfig.getAuthenticatedRetrofit(this)
                .create(com.example.evchargingapp.data.api.EVOwnerApiService::class.java)
        )
        offlineManager = OfflineManager(this)
    }
    
    private fun setupListeners() {
        btnLogin.setOnClickListener { attemptLogin() }
        btnRegister.setOnClickListener { 
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        // User type selection listeners
        btnEvOwner.setOnClickListener {
            selectUserType(UserType.EV_OWNER)
        }
        
        btnStationOperator.setOnClickListener {
            selectUserType(UserType.STATION_OPERATOR)
        }
        
        // Add listener for NIC field when disabled (to go back to NIC validation step)
        identifierInput.setOnClickListener {
            if (currentUserType == UserType.EV_OWNER && isNicValidated && !identifierInput.isEnabled) {
                // Allow user to go back and change NIC
                isNicValidated = false
                evOwnerValidatedEmail = null
                evOwnerValidatedNic = null
                updateUIForUserType()
                Toast.makeText(this, "Please enter your NIC again", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun selectUserType(userType: UserType) {
        currentUserType = userType
        
        // Reset EV Owner two-step login state when switching user types
        evOwnerValidatedEmail = null
        evOwnerValidatedNic = null
        isNicValidated = false
        
        updateUIForUserType()
    }
    
    private fun updateUIForUserType() {
        val isEvOwner = currentUserType == UserType.EV_OWNER
        
        // Update button styles for EV Owner
        if (isEvOwner) {
            btnEvOwner.backgroundTintList = resources.getColorStateList(R.color.primary_blue, null)
            btnEvOwner.setTextColor(resources.getColor(R.color.text_white, null))
            btnEvOwner.strokeColor = resources.getColorStateList(R.color.primary_blue, null)
        } else {
            btnEvOwner.backgroundTintList = resources.getColorStateList(android.R.color.transparent, null)
            btnEvOwner.setTextColor(resources.getColor(R.color.text_secondary, null))
            btnEvOwner.strokeColor = resources.getColorStateList(R.color.border_light, null)
        }
        
        // Update button styles for Station Operator
        if (!isEvOwner) {
            btnStationOperator.backgroundTintList = resources.getColorStateList(R.color.primary_blue, null)
            btnStationOperator.setTextColor(resources.getColor(R.color.text_white, null))
            btnStationOperator.strokeColor = resources.getColorStateList(R.color.primary_blue, null)
        } else {
            btnStationOperator.backgroundTintList = resources.getColorStateList(android.R.color.transparent, null)
            btnStationOperator.setTextColor(resources.getColor(R.color.text_secondary, null))
            btnStationOperator.strokeColor = resources.getColorStateList(R.color.border_light, null)
        }
        
        // Update UI based on user type and two-step login state
        if (isEvOwner) {
            if (isNicValidated) {
                // Step 2: Show password field
                inputLayoutIdentifier.hint = "NIC"
                identifierInput.isEnabled = false // Disable NIC field
                inputLayoutPassword.visibility = View.VISIBLE
                btnLogin.text = "Login"
            } else {
                // Step 1: Show only NIC field
                inputLayoutIdentifier.hint = "NIC"
                identifierInput.isEnabled = true
                inputLayoutPassword.visibility = View.GONE
                btnLogin.text = "Next"
            }
            identifierInput.inputType = android.text.InputType.TYPE_CLASS_TEXT
        } else {
            // Station Operator: Show both fields normally
            inputLayoutIdentifier.hint = "Email"
            identifierInput.isEnabled = true
            inputLayoutPassword.visibility = View.VISIBLE
            btnLogin.text = "Login"
            identifierInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
            
        // Clear inputs when switching user types
        if (!isNicValidated) {
            identifierInput.text?.clear()
        }
        passwordInput.text?.clear()
    }
    
    private fun checkOfflineStatus() {
        offlineManager.showOfflineNotification { message ->
            statusText.text = message
            statusText.visibility = View.VISIBLE
        }
    }
    
    private fun attemptLogin() {
        val identifier = identifierInput.text.toString().trim()
        val password = passwordInput.text.toString()
        
        if (currentUserType == UserType.EV_OWNER) {
            if (!isNicValidated) {
                // Step 1: Validate NIC
                attemptNicValidation(identifier)
            } else {
                // Step 2: Login with email and password
                attemptEmailPasswordLogin()
            }
        } else {
            // Station Operator: Normal login flow
            if (!validateInput(identifier, password)) return
            
            setLoadingState(true)
            lifecycleScope.launch {
                try {
                    val result = authRepository.loginHybrid(identifier, password, currentUserType)
                    when (result) {
                        is AuthResult.Success -> handleLoginSuccess(result.user, result.token, result.message)
                        is AuthResult.Failure -> handleLoginFailure(result.error)
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Login error", e)
                    handleLoginFailure("Login failed: ${e.message}")
                } finally {
                    setLoadingState(false)
                }
            }
        }
    }
    
    private fun attemptNicValidation(nic: String) {
        if (TextUtils.isEmpty(nic)) {
            identifierInput.error = "NIC required"
            return
        }
        
        if (nic.length < 10) {
            identifierInput.error = "Please enter a valid NIC"
            return
        }
        
        setLoadingState(true)
        lifecycleScope.launch {
            try {
                val result = authRepository.validateNic(nic)
                when (result) {
                    is NicValidationResult.Success -> {
                        evOwnerValidatedEmail = result.email
                        evOwnerValidatedNic = nic // Store the original NIC
                        isNicValidated = true
                        updateUIForUserType()
                        Toast.makeText(this@LoginActivity, "NIC validated successfully. Please enter your password.", Toast.LENGTH_SHORT).show()
                    }
                    is NicValidationResult.Failure -> {
                        handleNicValidationFailure(result.error)
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "NIC validation error", e)
                handleNicValidationFailure("Validation failed: ${e.message}")
            } finally {
                setLoadingState(false)
            }
        }
    }
    
    private fun attemptEmailPasswordLogin() {
        val password = passwordInput.text.toString()
        
        if (TextUtils.isEmpty(password)) {
            passwordInput.error = "Password required"
            return
        }
        
        if (evOwnerValidatedEmail == null) {
            Toast.makeText(this, "Please validate NIC first", Toast.LENGTH_SHORT).show()
            return
        }
        
        setLoadingState(true)
        lifecycleScope.launch {
            try {
                val result = authRepository.loginWithEmail(evOwnerValidatedEmail!!, password)
                when (result) {
                    is AuthResult.Success -> handleLoginSuccess(result.user, result.token, result.message)
                    is AuthResult.Failure -> handleLoginFailure(result.error)
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Email login error", e)
                handleLoginFailure("Login failed: ${e.message}")
            } finally {
                setLoadingState(false)
            }
        }
    }
    
    private fun handleNicValidationFailure(error: String) {
        Log.d("LoginActivity", "NIC validation failed: $error")
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        
        // Clear NIC field on failure
        identifierInput.text?.clear()
        identifierInput.error = error
    }
    
    private fun validateInput(identifier: String, password: String): Boolean {
        val fieldName = if (currentUserType == UserType.EV_OWNER) "NIC" else "Email"
        
        when {
            TextUtils.isEmpty(identifier) -> {
                identifierInput.error = "$fieldName required"
                return false
            }
            currentUserType == UserType.STATION_OPERATOR && 
                !android.util.Patterns.EMAIL_ADDRESS.matcher(identifier).matches() -> {
                identifierInput.error = "Please enter a valid email address"
                return false
            }
            currentUserType == UserType.EV_OWNER && identifier.length < 10 -> {
                identifierInput.error = "Please enter a valid NIC"
                return false
            }
            TextUtils.isEmpty(password) -> {
                passwordInput.error = "Password required"
                return false
            }
        }
        return true
    }
    
    private fun handleLoginSuccess(user: User, token: String?, message: String?) {
        Log.d("LoginActivity", "Login successful for user: ${user.name}")
        
        // Check account status
        if (!user.canLogin()) {
            Toast.makeText(this, "Account is deactivated. Please contact support.", Toast.LENGTH_LONG).show()
            return
        }
        
        // For EV Owners, immediately fetch and save complete profile before proceeding
        if (!user.isStationOperator()) {
            fetchAndSaveEVOwnerProfile(user, token, message)
        } else {
            // For Station Operators, save session and proceed
            sessionManager.saveLogin(user, token)
            
            // Show success message if any
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
            
            // Navigate to operator dashboard
            val intent = Intent(this, com.example.evchargingapp.ui.operator.OperatorDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    private fun fetchAndSaveEVOwnerProfile(user: User, token: String?, message: String?) {
        lifecycleScope.launch {
            try {
                val nic = evOwnerValidatedNic
                if (nic == null) {
                    Log.w("LoginActivity", "No validated NIC available for profile fetch")
                    // Fallback to basic login
                    completeLoginProcess(user, token, message)
                    return@launch
                }
                
                Log.d("LoginActivity", "Fetching EV Owner profile for NIC: $nic")
                val result = evOwnerRepository.getEVOwnerByNIC(nic)
                
                result.onSuccess { evOwner ->
                    Log.d("LoginActivity", "Successfully fetched EV Owner profile: ${evOwner.firstName} ${evOwner.lastName}")
                    
                    // Save session with the actual NIC and complete profile
                    sessionManager.saveLogin(user, token, nic)
                    sessionManager.saveEVOwnerProfile(evOwner)
                    
                    // Complete login process first (navigate to MainActivity)
                    completeLoginProcess(user, token, message)
                    
                    // Notify MainActivity to refresh name after a short delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        MainActivity.refreshUserNameIfActive()
                        Log.d("LoginActivity", "User name refresh triggered")
                    }, 500) // 500ms delay
                    
                }.onFailure { exception ->
                    Log.w("LoginActivity", "Failed to fetch EV Owner profile: ${exception.message}")
                    // Save basic session and proceed (profile can be fetched later)
                    sessionManager.saveLogin(user, token, nic)
                    completeLoginProcess(user, token, message)
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error fetching EV Owner profile: ${e.message}")
                // Save basic session and proceed
                sessionManager.saveLogin(user, token, evOwnerValidatedNic)
                completeLoginProcess(user, token, message)
            }
        }
    }
    
    private fun completeLoginProcess(user: User, token: String?, message: String?) {
        // Show success message if any
        message?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        
        // Navigate to main dashboard
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun handleLoginFailure(error: String) {
        Log.d("LoginActivity", "Login failed: $error")
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        
        // Clear password field on failure
        passwordInput.text.clear()
    }
    
    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        val enabled = !isLoading
        listOf(btnLogin, btnRegister, btnEvOwner, btnStationOperator, identifierInput, passwordInput)
            .forEach { it.isEnabled = enabled }
        
        // Set appropriate button text based on loading state and current step
        if (isLoading) {
            btnLogin.text = if (currentUserType == UserType.EV_OWNER && !isNicValidated) {
                "Validating..."
            } else {
                "Logging in..."
            }
        } else {
            btnLogin.text = if (currentUserType == UserType.EV_OWNER && !isNicValidated) {
                "Next"
            } else {
                "Login"
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (sessionManager.isLoggedIn() && sessionManager.requiresReauth()) {
            sessionManager.logout()
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
        }
        checkOfflineStatus()
    }
}