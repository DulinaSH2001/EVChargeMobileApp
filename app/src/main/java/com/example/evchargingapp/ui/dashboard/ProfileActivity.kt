package com.example.evchargingapp.ui.dashboard

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.evchargingapp.R
import com.example.evchargingapp.data.local.UserDbHelper
import com.example.evchargingapp.utils.SessionManager

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: UserDbHelper
    private lateinit var tvNic: TextView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        sessionManager = SessionManager(this)
        dbHelper = UserDbHelper(this)
        
        tvNic = findViewById(R.id.tv_nic)
        tvName = findViewById(R.id.tv_name)
        tvEmail = findViewById(R.id.tv_email)
        tvPhone = findViewById(R.id.tv_phone)
        
        val btnUpdate = findViewById<MaterialButton>(R.id.btn_update_profile)
        val btnDeactivate = findViewById<MaterialButton>(R.id.btn_deactivate)
        val btnBack = findViewById<MaterialButton>(R.id.btn_back)
        
        loadProfile()
        
        btnUpdate.setOnClickListener {
            Toast.makeText(this, "Update flow not implemented", Toast.LENGTH_SHORT).show()
        }
        btnDeactivate.setOnClickListener { confirmDeactivate() }
        btnBack.setOnClickListener { finish() }
    }
    
    private fun loadProfile() {
        val nic = sessionManager.getNic()
        if (nic == null) {
            Toast.makeText(this, "No user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val user = dbHelper.getUserByNic(nic)
        user?.let {
            tvNic.text = it.nic
            tvName.text = it.name
            tvEmail.text = it.email
            tvPhone.text = it.phone
        }
    }
    
    private fun confirmDeactivate() {
        AlertDialog.Builder(this)
            .setTitle("Deactivate account")
            .setMessage("Are you sure you want to deactivate your account?")
            .setPositiveButton("Yes") { _, _ ->
                // remove locally by clearing session; real API call should be added
                sessionManager.logout()
                Toast.makeText(this, "Account deactivated locally", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}