package com.example.evchargingapp.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingapp.R
import com.example.evchargingapp.ui.dashboard.EnhancedDashboardFragment

class UnifiedSearchAdapter(
    private val onLocationClick: (LocationSearchResult) -> Unit
) : RecyclerView.Adapter<UnifiedSearchAdapter.ViewHolder>() {

    private var searchResults = listOf<LocationSearchResult>()

    fun updateResults(newResults: List<LocationSearchResult>) {
        searchResults = newResults
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(searchResults[position])
    }

    override fun getItemCount() = searchResults.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconLocation: ImageView = itemView.findViewById(R.id.icon_location)
        private val textPrimary: TextView = itemView.findViewById(R.id.text_primary)
        private val textSecondary: TextView = itemView.findViewById(R.id.text_secondary)

        fun bind(result: LocationSearchResult) {
            textPrimary.text = result.primaryText
            textSecondary.text = result.secondaryText
            
            itemView.setOnClickListener {
                onLocationClick(result)
            }
        }
    }
}

sealed class LocationSearchResult {
    abstract val primaryText: String
    abstract val secondaryText: String
    abstract val latitude: Double
    abstract val longitude: Double
    
    data class OnlineResult(
        override val primaryText: String,
        override val secondaryText: String,
        override val latitude: Double,
        override val longitude: Double,
        val placeId: String
    ) : LocationSearchResult()
    
    data class OfflineResult(
        override val primaryText: String,
        override val secondaryText: String,
        override val latitude: Double,
        override val longitude: Double
    ) : LocationSearchResult()
}