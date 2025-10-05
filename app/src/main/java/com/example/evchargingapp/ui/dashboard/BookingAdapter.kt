package com.example.evchargingapp.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingapp.R
import com.example.evchargingapp.data.local.Booking
import com.example.evchargingapp.data.local.BookingStatus
import com.google.android.material.button.MaterialButton

class BookingAdapter(
    private var bookings: MutableList<Booking>,
    private val onItemClick: (Booking) -> Unit,
    private val onCancelClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bind(booking)
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        bookings.clear()
        bookings.addAll(newBookings)
        notifyDataSetChanged()
    }

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStationName: TextView = itemView.findViewById(R.id.tv_station_name)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btn_cancel)
        private val btnViewDetails: MaterialButton = itemView.findViewById(R.id.btn_view_details)
        private val actionButtons: View = itemView.findViewById(R.id.action_buttons)

        fun bind(booking: Booking) {
            tvStationName.text = booking.stationName
            tvDate.text = "${booking.date} at ${booking.time}"
            tvDuration.text = booking.duration
            tvLocation.text = booking.location

            // Set status with appropriate styling
            tvStatus.text = booking.status.name.lowercase().replaceFirstChar { it.uppercase() }
            when (booking.status) {
                BookingStatus.PENDING -> {
                    tvStatus.setBackgroundResource(R.drawable.rounded_background_warning)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.primary_blue))
                }
                BookingStatus.APPROVED -> {
                    tvStatus.setBackgroundResource(R.drawable.rounded_background_success)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.success_green))
                }
                BookingStatus.PAST -> {
                    tvStatus.setBackgroundResource(R.drawable.rounded_background_neutral)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.text_secondary))
                }
                BookingStatus.CANCELLED -> {
                    tvStatus.setBackgroundResource(R.drawable.rounded_background_error)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.error_red))
                }
            }

            // Handle button visibility and actions
            when (booking.status) {
                BookingStatus.PENDING -> {
                    actionButtons.visibility = View.VISIBLE
                    btnCancel.visibility = View.VISIBLE
                    btnCancel.text = "Cancel"
                    btnViewDetails.text = "Details"
                }
                BookingStatus.APPROVED -> {
                    actionButtons.visibility = View.VISIBLE
                    btnCancel.visibility = View.VISIBLE
                    btnCancel.text = "Cancel"
                    btnViewDetails.text = "View"
                }
                BookingStatus.PAST -> {
                    actionButtons.visibility = View.VISIBLE
                    btnCancel.visibility = View.GONE
                    btnViewDetails.text = "View"
                }
                BookingStatus.CANCELLED -> {
                    actionButtons.visibility = View.VISIBLE
                    btnCancel.visibility = View.GONE
                    btnViewDetails.text = "View"
                }
            }

            // Set click listeners
            btnViewDetails.setOnClickListener {
                onItemClick(booking)
            }

            btnCancel.setOnClickListener {
                onCancelClick(booking)
            }

            itemView.setOnClickListener {
                onItemClick(booking)
            }
        }
    }
}