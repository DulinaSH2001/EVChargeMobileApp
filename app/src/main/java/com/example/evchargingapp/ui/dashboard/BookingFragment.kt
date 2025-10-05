package com.example.evchargingapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.evchargingapp.R
import com.example.evchargingapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.*

class BookingFragment : Fragment() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var etStationName: EditText
    private lateinit var etLocation: EditText
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvSelectedTime: TextView
    private lateinit var spinnerDuration: Spinner
    private lateinit var btnSelectDate: MaterialCardView
    private lateinit var btnSelectTime: MaterialCardView
    private lateinit var btnCreateBooking: MaterialButton
    
    private var selectedDate: Long? = null
    private var selectedTime: Pair<Int, Int>? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_booking, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupClickListeners()
    }
    
    private fun initViews(view: View) {
        sessionManager = SessionManager(requireContext())
        
        etStationName = view.findViewById(R.id.et_station_name)
        etLocation = view.findViewById(R.id.et_location)
        tvSelectedDate = view.findViewById(R.id.tv_selected_date)
        tvSelectedTime = view.findViewById(R.id.tv_selected_time)
        spinnerDuration = view.findViewById(R.id.spinner_duration)
        btnSelectDate = view.findViewById(R.id.btn_select_date)
        btnSelectTime = view.findViewById(R.id.btn_select_time)
        btnCreateBooking = view.findViewById(R.id.btn_create_booking)
    }
    
    private fun setupClickListeners() {
        btnSelectDate.setOnClickListener {
            showDatePicker()
        }
        
        btnSelectTime.setOnClickListener {
            showTimePicker()
        }
        
        btnCreateBooking.setOnClickListener {
            createBooking()
        }
    }
    
    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Booking Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        
        datePicker.addOnPositiveButtonClickListener { date ->
            selectedDate = date
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            tvSelectedDate.text = formatter.format(Date(date))
        }
        
        datePicker.show(parentFragmentManager, "date_picker")
    }
    
    private fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Select Booking Time")
            .build()
        
        timePicker.addOnPositiveButtonClickListener {
            selectedTime = Pair(timePicker.hour, timePicker.minute)
            val timeString = String.format(
                Locale.getDefault(),
                "%02d:%02d %s",
                if (timePicker.hour == 0 || timePicker.hour == 12) 12 else timePicker.hour % 12,
                timePicker.minute,
                if (timePicker.hour < 12) "AM" else "PM"
            )
            tvSelectedTime.text = timeString
        }
        
        timePicker.show(parentFragmentManager, "time_picker")
    }
    
    private fun createBooking() {
        val stationName = etStationName.text.toString().trim()
        val location = etLocation.text.toString().trim()
        
        // Validation
        if (stationName.isEmpty()) {
            etStationName.error = "Station name is required"
            return
        }
        
        if (location.isEmpty()) {
            etLocation.error = "Location is required"
            return
        }
        
        if (selectedDate == null) {
            showToast("Please select a date")
            return
        }
        
        if (selectedTime == null) {
            showToast("Please select a time")
            return
        }
        
        // TODO: Create booking logic
        showToast("Booking created successfully!")
        
        // Navigate back to My Bookings
        (activity as? MainActivity)?.navigateToBookings()
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}