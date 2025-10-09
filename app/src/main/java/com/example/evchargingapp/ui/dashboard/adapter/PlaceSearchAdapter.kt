package com.example.evchargingapp.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingapp.R
import com.google.android.libraries.places.api.model.AutocompletePrediction

class PlaceSearchAdapter(
    private val onPlaceClick: (AutocompletePrediction) -> Unit
) : RecyclerView.Adapter<PlaceSearchAdapter.ViewHolder>() {

    private var predictions = listOf<AutocompletePrediction>()

    fun updatePredictions(newPredictions: List<AutocompletePrediction>) {
        predictions = newPredictions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(predictions[position])
    }

    override fun getItemCount() = predictions.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconLocation: ImageView = itemView.findViewById(R.id.icon_location)
        private val textPrimary: TextView = itemView.findViewById(R.id.text_primary)
        private val textSecondary: TextView = itemView.findViewById(R.id.text_secondary)

        fun bind(prediction: AutocompletePrediction) {
            textPrimary.text = prediction.getPrimaryText(null)
            textSecondary.text = prediction.getSecondaryText(null)
            
            itemView.setOnClickListener {
                onPlaceClick(prediction)
            }
        }
    }
}