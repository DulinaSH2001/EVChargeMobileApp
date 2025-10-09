package com.example.evchargingapp.ui.booking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.BookingDto
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(
    private val onEditClick: (BookingDto) -> Unit,
    private val onCancelClick: (BookingDto) -> Unit,
    private val onItemClick: (BookingDto) -> Unit
) : ListAdapter<BookingDto, BookingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_booking)
        private val tvStationName: TextView = itemView.findViewById(R.id.tv_station_name)
        private val tvStationLocation: TextView = itemView.findViewById(R.id.tv_station_location)
        private val tvBookingDate: TextView = itemView.findViewById(R.id.tv_booking_date)
        private val tvBookingTime: TextView = itemView.findViewById(R.id.tv_booking_time)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val tvSlotNumber: TextView = itemView.findViewById(R.id.tv_slot_number)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvBookingId: TextView = itemView.findViewById(R.id.tv_booking_id)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btn_edit)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btn_cancel)

        fun bind(booking: BookingDto) {
            // Set basic information
            tvStationName.text = booking.chargingStationName ?: "Unknown Station"
            tvStationLocation.text = booking.getParsedAddress()
            tvSlotNumber.text = "Slot ${booking.slotNumber}"
            tvDuration.text = "${booking.duration} min"
            tvBookingId.text = "ID: ${booking.id.take(8)}..."

            // Parse and format date/time
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(booking.reservationDateTime)
                
                if (date != null) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    
                    tvBookingDate.text = dateFormat.format(date)
                    tvBookingTime.text = timeFormat.format(date)
                } else {
                    tvBookingDate.text = "Invalid date"
                    tvBookingTime.text = "Invalid time"
                }
            } catch (e: Exception) {
                tvBookingDate.text = booking.reservationDateTime.substringBefore('T')
                tvBookingTime.text = booking.reservationDateTime.substringAfter('T').substringBefore('Z')
            }

            // Set status with appropriate styling
            val statusText = getStatusText(booking.status)
            tvStatus.text = statusText
            
            when (statusText.uppercase()) {
                "PENDING" -> {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_pending))
                    tvStatus.setBackgroundResource(R.drawable.background_status_pending)
                }
                "APPROVED" -> {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_approved))
                    tvStatus.setBackgroundResource(R.drawable.background_status_approved)
                }
                "CANCELLED" -> {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_cancelled))
                    tvStatus.setBackgroundResource(R.drawable.background_status_cancelled)
                }
                "COMPLETED" -> {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_completed))
                    tvStatus.setBackgroundResource(R.drawable.background_status_completed)
                }
                else -> {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                    tvStatus.setBackgroundResource(R.drawable.background_status_default)
                }
            }

            // Configure buttons based on status
            val normalizedStatus = getStatusText(booking.status).uppercase()
            val canEdit = normalizedStatus == "PENDING" || normalizedStatus == "APPROVED"
            val canCancel = normalizedStatus == "PENDING" || normalizedStatus == "APPROVED"

            btnEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
            btnCancel.visibility = if (canCancel) View.VISIBLE else View.GONE

            btnEdit.isEnabled = canEdit
            btnCancel.isEnabled = canCancel

            // Set click listeners
            cardView.setOnClickListener { onItemClick(booking) }
            btnEdit.setOnClickListener { onEditClick(booking) }
            btnCancel.setOnClickListener { onCancelClick(booking) }
        }
        
        private fun getStatusText(status: String): String {
            return when (status.trim().uppercase()) {
                // Handle string status values from API
                "PENDING" -> "Pending"
                "APPROVED" -> "Approved"
                "CANCELLED" -> "Cancelled"
                "COMPLETED" -> "Completed"
                // Handle numeric status values (backward compatibility)
                "0" -> "Pending"
                "1" -> "Approved"
                "2" -> "Cancelled"
                "3" -> "Completed"
                else -> status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<BookingDto>() {
        override fun areItemsTheSame(oldItem: BookingDto, newItem: BookingDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BookingDto, newItem: BookingDto): Boolean {
            return oldItem == newItem
        }
    }
}