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
    
    private var currentUserType: UserType = UserType.EV_OWNER
    
    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository
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
        
        // Initialize components
        sessionManager = SessionManager(this)
        authRepository = AuthRepository(this)
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
    }
    
    private fun selectUserType(userType: UserType) {
        currentUserType = userType
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
        
        // Update input field
        inputLayoutIdentifier.hint = if (isEvOwner) "NIC" else "Email"
        identifierInput.inputType = if (isEvOwner) 
            android.text.InputType.TYPE_CLASS_TEXT else 
            android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            
        identifierInput.text?.clear()
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
        
        // Save session with token
        sessionManager.saveLogin(user, token)
        
        // Show success message if any
        message?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        
        // Navigate based on user role
        val intent = if (user.isStationOperator()) {
            Intent(this, com.example.evchargingapp.ui.operator.OperatorDashboardActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        
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
        btnLogin.text = if (isLoading) "Logging in..." else "Login"
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