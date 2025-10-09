package com.example.evchargingapp.utils

import kotlin.math.*

object LocationUtils {
    
    /**
     * Parse location string from API response
     * Format: "{address:WXJG8XP, Kaduwela, Sri Lanka,longitude:79.97756298573279,latitude:6.930657602273325}"
     */
    fun parseLocationString(locationString: String): Pair<Double, Double>? {
        return try {
            android.util.Log.d("LocationUtils", "Parsing location string: $locationString")
            
            // Remove braces and split by comma
            val cleanString = locationString.removePrefix("{").removeSuffix("}")
            val parts = cleanString.split(",")
            
            var latitude: Double? = null
            var longitude: Double? = null
            
            for (part in parts) {
                val trimmed = part.trim()
                when {
                    trimmed.startsWith("latitude:") -> {
                        latitude = trimmed.substringAfter("latitude:").toDoubleOrNull()
                    }
                    trimmed.startsWith("longitude:") -> {
                        longitude = trimmed.substringAfter("longitude:").toDoubleOrNull()
                    }
                }
            }
            
            if (latitude != null && longitude != null) {
                android.util.Log.d("LocationUtils", "Parsed coordinates: lat=$latitude, lng=$longitude")
                Pair(latitude, longitude)
            } else {
                android.util.Log.w("LocationUtils", "Could not parse coordinates from: $locationString")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationUtils", "Error parsing location string: $locationString", e)
            null
        }
    }
    
    /**
     * Parse address from location string
     * Format: "{address:WXJG8XP, Kaduwela, Sri Lanka,longitude:79.97756298573279,latitude:6.930657602273325}"
     */
    fun parseAddressFromLocationString(locationString: String): String {
        return try {
            val cleanString = locationString.removePrefix("{").removeSuffix("}")
            val addressPart = cleanString.substringBefore(",longitude:")
                .substringBefore(",latitude:")
            
            if (addressPart.startsWith("address:")) {
                addressPart.substringAfter("address:")
            } else {
                locationString // fallback to original string
            }
        } catch (e: Exception) {
            locationString // fallback to original string
        }
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     * Returns distance in kilometers
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadiusKm * c
    }
    
    /**
     * Filter stations within specified radius and add distance information
     */
    fun filterStationsWithinRadius(
        stations: List<com.example.evchargingapp.data.api.ChargingStationDto>,
        userLatitude: Double,
        userLongitude: Double,
        radiusKm: Double = 10.0
    ): List<com.example.evchargingapp.data.api.ChargingStationDto> {
        return stations.mapNotNull { station ->
            // Parse coordinates from location string
            val coordinates = parseLocationString(station.location)
            
            if (coordinates != null) {
                val (stationLat, stationLon) = coordinates
                val distance = calculateDistance(userLatitude, userLongitude, stationLat, stationLon)
                
                if (distance <= radiusKm) {
                    // Return station with distance and parsed coordinates
                    station.copy(
                        latitude = stationLat,
                        longitude = stationLon,
                        distance = distance
                    )
                } else null
            } else null
        }.sortedBy { it.distance } // Sort by distance, nearest first
    }
}