package com.example.evchargingapp.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.ChargingStationDto
import com.google.android.material.button.MaterialButton
import kotlin.math.roundToInt

class ChargingStationAdapter(
    private val onViewDetailsClick: (ChargingStationDto) -> Unit,
    private val onBookNowClick: (ChargingStationDto) -> Unit
) : ListAdapter<ChargingStationDto, ChargingStationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_charging_station, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStationName: TextView = itemView.findViewById(R.id.tv_station_name)
        private val tvStationLocation: TextView = itemView.findViewById(R.id.tv_station_location)
        private val tvDistance: TextView = itemView.findViewById(R.id.tv_distance)
        private val tvStationType: TextView = itemView.findViewById(R.id.tv_station_type)
        private val tvSlotsInfo: TextView = itemView.findViewById(R.id.tv_slots_info)
        private val tvAvailabilityStatus: TextView = itemView.findViewById(R.id.tv_availability_status)
        private val indicatorAvailability: View = itemView.findViewById(R.id.indicator_availability)
        private val btnViewDetails: MaterialButton = itemView.findViewById(R.id.btn_view_details)
        private val btnBookNow: MaterialButton = itemView.findViewById(R.id.btn_book_now)

        fun bind(station: ChargingStationDto) {
            tvStationName.text = station.name
            tvStationLocation.text = station.location
            
            // Format distance
            station.distance?.let { distance ->
                tvDistance.text = "${(distance * 10).roundToInt() / 10.0} km"
            } ?: run {
                tvDistance.text = ""
            }
            
            // Set station type
            tvStationType.text = station.type
            
            // Set slot information
            tvSlotsInfo.text = "${station.availableSlots}/${station.totalSlots} available"
            
            // Set availability status and indicator
            val isAvailable = station.availableSlots > 0 && station.isActive
            
            if (isAvailable) {
                tvAvailabilityStatus.text = "Available"
                tvAvailabilityStatus.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.status_available)
                )
                indicatorAvailability.backgroundTintList = 
                    ContextCompat.getColorStateList(itemView.context, R.color.status_available)
                btnBookNow.isEnabled = true
            } else if (!station.isActive) {
                tvAvailabilityStatus.text = "Maintenance"
                tvAvailabilityStatus.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.status_maintenance)
                )
                indicatorAvailability.backgroundTintList = 
                    ContextCompat.getColorStateList(itemView.context, R.color.status_maintenance)
                btnBookNow.isEnabled = false
            } else {
                tvAvailabilityStatus.text = "Occupied"
                tvAvailabilityStatus.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.status_occupied)
                )
                indicatorAvailability.backgroundTintList = 
                    ContextCompat.getColorStateList(itemView.context, R.color.status_occupied)
                btnBookNow.isEnabled = false
            }
            
            // Set click listeners
            btnViewDetails.setOnClickListener {
                onViewDetailsClick(station)
            }
            
            btnBookNow.setOnClickListener {
                onBookNowClick(station)
            }
            
            // Set item click listener
            itemView.setOnClickListener {
                onViewDetailsClick(station)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ChargingStationDto>() {
        override fun areItemsTheSame(oldItem: ChargingStationDto, newItem: ChargingStationDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChargingStationDto, newItem: ChargingStationDto): Boolean {
            return oldItem == newItem
        }
    }
}