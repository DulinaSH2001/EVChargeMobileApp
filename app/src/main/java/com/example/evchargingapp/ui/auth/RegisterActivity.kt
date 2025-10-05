package com.example.evchargingapp.ui.auth

import android.os.Bundle
import android.text.TextUtils
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.evchargingapp.R
import com.example.evchargingapp.data.local.User
import com.example.evchargingapp.data.local.UserDbHelper
import com.example.evchargingapp.utils.SessionManager
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var nicInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmInput: EditText
    private lateinit var termsCheckbox: CheckBox
    private lateinit var dbHelper: UserDbHelper
    private lateinit var sessionManager: SessionManager
    
    companion object {
        private val NIC_PATTERN = Pattern.compile("^[0-9]{9}[VvXx]$|^[0-9]{12}$")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        // Initialize input fields
        nicInput = findViewById(R.id.input_nic)
        nameInput = findViewById(R.id.input_name)
        emailInput = findViewById(R.id.input_email)
        phoneInput = findViewById(R.id.input_phone)
        passwordInput = findViewById(R.id.input_password)
        confirmInput = findViewById(R.id.input_confirm_password)
        termsCheckbox = findViewById(R.id.checkbox_terms)
        
        dbHelper = UserDbHelper(this)
        sessionManager = SessionManager(this)
        
        // Set up button listeners
        val btnRegister = findViewById<MaterialButton>(R.id.btn_register)
        btnRegister.setOnClickListener { register() }
        
        // Back to login listener
        findViewById<TextView>(R.id.btn_login).setOnClickListener { finish() }
    }
    
    private fun register() {
        val nic = nicInput.text.toString().trim()
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmInput.text.toString()
        
        // Validation
        if (TextUtils.isEmpty(name)) {
            nameInput.error = "Name required"
            return
        }
        if (TextUtils.isEmpty(email)) {
            emailInput.error = "Email required"
            return
        }
        if (TextUtils.isEmpty(nic) || !NIC_PATTERN.matcher(nic).matches()) {
            nicInput.error = "Enter valid NIC (9 digits + V/X or 12 digits)"
            return
        }
        if (TextUtils.isEmpty(phone)) {
            phoneInput.error = "Phone required"
            return
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.error = "Password required"
            return
        }
        if (password != confirmPassword) {
            confirmInput.error = "Passwords do not match"
            return
        }
        if (!termsCheckbox.isChecked) {
            Toast.makeText(this, "Please agree to Terms and Conditions", Toast.LENGTH_SHORT).show()
            return
        }
        
        val user = User(nic, name, email, phone, password)
        val success = dbHelper.insertUser(user)
        if (success) {
            sessionManager.saveLogin(user)
            Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Account with NIC already exists", Toast.LENGTH_SHORT).show()
        }
    }
}