package com.example.evchargingapp.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingapp.R
import com.example.evchargingapp.data.api.ChargingStationDto

class ChargingStationSearchAdapter(
    private val onStationClick: (ChargingStationDto) -> Unit
) : RecyclerView.Adapter<ChargingStationSearchAdapter.ViewHolder>() {

    private var stations = listOf<ChargingStationDto>()

    fun updateStations(newStations: List<ChargingStationDto>) {
        stations = newStations
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_station_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(stations[position])
    }

    override fun getItemCount() = stations.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconStation: ImageView = itemView.findViewById(R.id.icon_station)
        private val tvStationName: TextView = itemView.findViewById(R.id.tv_station_name)
        private val tvStationLocation: TextView = itemView.findViewById(R.id.tv_station_location)
        private val tvAvailability: TextView = itemView.findViewById(R.id.tv_availability)
        private val indicatorStatus: View = itemView.findViewById(R.id.indicator_status)

        fun bind(station: ChargingStationDto) {
            tvStationName.text = station.name
            tvStationLocation.text = station.getParsedAddress()
            
            // Set availability based on available slots
            val availableSlots = station.availableSlots ?: 0
            val isAvailable = availableSlots > 0
            
            tvAvailability.text = if (isAvailable) {
                "Available ($availableSlots slots)"
            } else {
                "Busy"
            }
            
            // Set status indicator color
            val colorRes = if (isAvailable) R.color.status_available else R.color.status_busy
            indicatorStatus.setBackgroundColor(
                ContextCompat.getColor(itemView.context, colorRes)
            )
            
            itemView.setOnClickListener {
                onStationClick(station)
            }
        }
    }
}