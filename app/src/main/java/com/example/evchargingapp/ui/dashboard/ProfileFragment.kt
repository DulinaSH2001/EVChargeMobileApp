package com.example.evchargingapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.evchargingapp.R
import com.example.evchargingapp.utils.SessionManager
import com.example.evchargingapp.ui.auth.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class ProfileFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var cardEditProfile: MaterialCardView
    private lateinit var cardNotifications: MaterialCardView
    private lateinit var cardSecurity: MaterialCardView
    private lateinit var cardSupport: MaterialCardView
    private lateinit var cardAbout: MaterialCardView
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
        cardNotifications = view.findViewById(R.id.card_notifications)
        cardSecurity = view.findViewById(R.id.card_security)
        cardSupport = view.findViewById(R.id.card_support)
        cardAbout = view.findViewById(R.id.card_about)
        btnLogout = view.findViewById(R.id.btn_logout)
    }
    
    private fun setupUserInfo() {
        tvUserName.text = sessionManager.getUserName()
        tvUserEmail.text = sessionManager.getUserEmail()
    }
    
    private fun setupClickListeners() {
        cardEditProfile.setOnClickListener {
            showToast("Edit Profile clicked")
        }
        
        cardNotifications.setOnClickListener {
            showToast("Notifications settings clicked")
        }
        
        cardSecurity.setOnClickListener {
            showToast("Security settings clicked")
        }
        
        cardSupport.setOnClickListener {
            showToast("Support clicked")
        }
        
        cardAbout.setOnClickListener {
            showToast("About clicked")
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
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}