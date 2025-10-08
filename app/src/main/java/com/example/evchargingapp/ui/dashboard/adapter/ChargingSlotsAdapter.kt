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
import com.example.evchargingapp.data.api.SlotScheduleDto
import java.text.SimpleDateFormat
import java.util.*

class ChargingSlotsAdapter : ListAdapter<SlotScheduleDto, ChargingSlotsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_charging_slot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSlotNumber: TextView = itemView.findViewById(R.id.tv_slot_number)
        private val tvSlotTime: TextView = itemView.findViewById(R.id.tv_slot_time)
        private val tvSlotStatus: TextView = itemView.findViewById(R.id.tv_slot_status)
        private val indicatorSlotStatus: View = itemView.findViewById(R.id.indicator_slot_status)

        fun bind(slot: SlotScheduleDto) {
            tvSlotNumber.text = "Slot ${slot.slotNumber}"
            
            // Format time range
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                
                val startTime = inputFormat.parse(slot.startTime)
                val endTime = inputFormat.parse(slot.endTime)
                
                if (startTime != null && endTime != null) {
                    tvSlotTime.text = "${outputFormat.format(startTime)} - ${outputFormat.format(endTime)}"
                } else {
                    tvSlotTime.text = "Schedule not available"
                }
            } catch (e: Exception) {
                // If parsing fails, show raw time or default message
                tvSlotTime.text = "${slot.startTime} - ${slot.endTime}"
            }
            
            // Set status
            if (slot.isAvailable) {
                tvSlotStatus.text = "Available"
                tvSlotStatus.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.status_available)
                )
                indicatorSlotStatus.backgroundTintList = 
                    ContextCompat.getColorStateList(itemView.context, R.color.status_available)
            } else {
                tvSlotStatus.text = "Occupied"
                tvSlotStatus.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.status_occupied)
                )
                indicatorSlotStatus.backgroundTintList = 
                    ContextCompat.getColorStateList(itemView.context, R.color.status_occupied)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SlotScheduleDto>() {
        override fun areItemsTheSame(oldItem: SlotScheduleDto, newItem: SlotScheduleDto): Boolean {
            return oldItem.slotNumber == newItem.slotNumber
        }

        override fun areContentsTheSame(oldItem: SlotScheduleDto, newItem: SlotScheduleDto): Boolean {
            return oldItem == newItem
        }
    }
}