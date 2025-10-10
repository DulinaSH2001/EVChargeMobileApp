package com.example.evchargingapp.ui.operator.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingapp.R
import com.example.evchargingapp.data.local.ScannedBookingEntity
import java.text.SimpleDateFormat
import java.util.*

class ScannedBookingAdapter(
    private val onCompleteClick: (ScannedBookingEntity) -> Unit
) : ListAdapter<ScannedBookingEntity, ScannedBookingAdapter.ScannedBookingViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedBookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scanned_booking, parent, false)
        return ScannedBookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScannedBookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ScannedBookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEvOwnerName: TextView = itemView.findViewById(R.id.tv_ev_owner_name)
        private val tvBookingId: TextView = itemView.findViewById(R.id.tv_booking_id)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvStationName: TextView = itemView.findViewById(R.id.tv_station_name)
        private val tvStationLocation: TextView = itemView.findViewById(R.id.tv_station_location)
        private val tvBookingDate: TextView = itemView.findViewById(R.id.tv_booking_date)
        private val tvTimeSlot: TextView = itemView.findViewById(R.id.tv_time_slot)
        private val tvChargingSlot: TextView = itemView.findViewById(R.id.tv_charging_slot)
        private val tvVehicleNumber: TextView = itemView.findViewById(R.id.tv_vehicle_number)
        private val tvContactNumber: TextView = itemView.findViewById(R.id.tv_contact_number)
        private val tvScannedAt: TextView = itemView.findViewById(R.id.tv_scanned_at)
        private val tvUpdatedAt: TextView = itemView.findViewById(R.id.tv_updated_at)
        private val layoutAdditionalInfo: LinearLayout = itemView.findViewById(R.id.layout_additional_info)
        private val btnComplete: Button = itemView.findViewById(R.id.btn_complete)

        fun bind(booking: ScannedBookingEntity) {
            tvEvOwnerName.text = booking.evOwnerName
            tvBookingId.text = "ID: ${booking.bookingId}"
            tvStationName.text = booking.stationName
            tvStationLocation.text = booking.stationLocation
            tvBookingDate.text = booking.bookingDate
            tvTimeSlot.text = "${booking.startTime} - ${booking.endTime}"
            tvChargingSlot.text = "Slot ${booking.chargingSlotId}"

            // Handle additional info
            val hasAdditionalInfo = !booking.vehicleNumber.isNullOrEmpty() || !booking.contactNumber.isNullOrEmpty()
            layoutAdditionalInfo.visibility = if (hasAdditionalInfo) View.VISIBLE else View.GONE
            
            if (!booking.vehicleNumber.isNullOrEmpty()) {
                tvVehicleNumber.text = booking.vehicleNumber
            }
            if (!booking.contactNumber.isNullOrEmpty()) {
                tvContactNumber.text = booking.contactNumber
            }

            // Set status styling
            when (booking.status) {
                "InProgress" -> {
                    tvStatus.text = "In Progress"
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_inprogress_text))
                    tvStatus.setBackgroundResource(R.drawable.status_inprogress_background)
                    btnComplete.visibility = View.VISIBLE
                    btnComplete.text = "Mark as Completed"
                }
                "Completed" -> {
                    tvStatus.text = "Completed"
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_completed_text))
                    tvStatus.setBackgroundResource(R.drawable.status_completed_background)
                    btnComplete.visibility = View.GONE
                }
                else -> {
                    tvStatus.text = booking.status
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                    tvStatus.setBackgroundResource(R.drawable.rounded_background_light)
                    btnComplete.visibility = View.GONE
                }
            }

            // Format timestamps
            tvScannedAt.text = "Scanned: ${formatTimestamp(booking.scannedAt)}"
            tvUpdatedAt.text = "Updated: ${formatTimestamp(booking.updatedAt)}"

            // Set click listeners
            btnComplete.setOnClickListener {
                onCompleteClick(booking)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000} min ago"
                diff < 86400_000 -> "${diff / 3600_000} hr ago"
                diff < 604800_000 -> "${diff / 86400_000} days ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ScannedBookingEntity>() {
            override fun areItemsTheSame(oldItem: ScannedBookingEntity, newItem: ScannedBookingEntity): Boolean {
                return oldItem.bookingId == newItem.bookingId
            }

            override fun areContentsTheSame(oldItem: ScannedBookingEntity, newItem: ScannedBookingEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}