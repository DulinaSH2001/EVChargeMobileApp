package com.example.evchargingapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.evchargingapp.ui.dashboard.MainActivity
import com.example.evchargingapp.R
import com.example.evchargingapp.utils.SessionManager

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        val sessionManager = SessionManager(this)
        
        // If already logged in, go directly to appropriate activity
        if (sessionManager.isLoggedIn()) {
            val user = sessionManager.getCurrentUser()
            val intent = if (user?.isStationOperator() == true) {
                Intent(this, com.example.evchargingapp.ui.operator.OperatorDashboardActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java)
            }
            startActivity(intent)
            finish()
            return
        }
        
        // Handle Get Started button
        val btnGetStarted = findViewById<MaterialButton>(R.id.btn_get_started)
        btnGetStarted.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        // Handle Log In text
        findViewById<TextView>(R.id.tv_log_in).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}