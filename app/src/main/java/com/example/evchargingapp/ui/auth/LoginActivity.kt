package com.example.evchargingapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.evchargingapp.ui.dashboard.MainActivity
import com.example.evchargingapp.R
import com.example.evchargingapp.data.local.UserDbHelper
import com.example.evchargingapp.utils.SessionManager

class LoginActivity : AppCompatActivity() {
    
    private lateinit var nicInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: UserDbHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        nicInput = findViewById(R.id.input_nic)
        passwordInput = findViewById(R.id.input_password)
        sessionManager = SessionManager(this)
        dbHelper = UserDbHelper(this)
        
        // Add test users for demonstration
        addTestUsers()
        
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login)
        val btnRegister = findViewById<TextView>(R.id.btn_register)
        val btnOperator = findViewById<MaterialButton?>(R.id.btn_operator)
        
        btnLogin.setOnClickListener { attemptLogin() }
        btnRegister.setOnClickListener { 
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        btnOperator?.setOnClickListener {
            // Quick operator login
            sessionManager.setOperatorLogin("Operator")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    
    private fun attemptLogin() {
        val nic = nicInput.text.toString().trim()
        val password = passwordInput.text.toString()
        
        if (TextUtils.isEmpty(nic)) {
            nicInput.error = "NIC required"
            return
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.error = "Password required"
            return
        }
        
        // Try local DB first (since API not available here)
        Log.d("LoginActivity", "Attempting login for NIC: $nic")
        val user = dbHelper.getUserByNic(nic)
        Log.d("LoginActivity", "Found user: ${user?.name}")
        
        if (user != null && password == user.password) {
            Log.d("LoginActivity", "Login successful for user: ${user.name}")
            sessionManager.saveLogin(user)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        Log.d("LoginActivity", "Login failed - Invalid credentials")
        // TODO: Call remote API here. For now, show error.
        Toast.makeText(this, "Invalid credentials. Try: test/123 or demo/demo", Toast.LENGTH_LONG).show()
    }
    
    private fun addTestUsers() {
        // Add test users for demonstration (only if they don't exist)
        val testUser1 = com.example.evchargingapp.data.local.User(
            nic = "test",
            name = "Test User",
            email = "test@example.com",
            phone = "1234567890",
            password = "123"
        )
        
        val testUser2 = com.example.evchargingapp.data.local.User(
            nic = "demo",
            name = "Demo User", 
            email = "demo@example.com",
            phone = "0987654321",
            password = "demo"
        )
        
        // Insert test users (will be ignored if they already exist)
        dbHelper.insertUser(testUser1)
        dbHelper.insertUser(testUser2)
    }
}