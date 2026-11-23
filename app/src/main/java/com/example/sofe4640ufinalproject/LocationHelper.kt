package com.example.sofe4640ufinalproject

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.util.Locale

object LocationHelper {

    private val cityCoordinates = mapOf(
        "toronto" to Pair(43.6532, -79.3832),
        "oshawa" to Pair(43.8975, -78.8658),
        "whitby" to Pair(43.8975, -78.9428),
        "ajax" to Pair(43.8509, -79.0204),
        "pickering" to Pair(43.8383, -79.0868),
        "markham" to Pair(43.8561, -79.3370),
        "mississauga" to Pair(43.5890, -79.6441),
        "brampton" to Pair(43.7315, -79.7624),
        "vaughan" to Pair(43.8563, -79.5085),
        "scarborough" to Pair(43.7731, -79.2577)
    )

    fun getUserLocation(context: Context): Location {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if (locationManager != null) {
                    val providers = locationManager.getProviders(true)
                    var bestLocation: Location? = null
                    for (provider in providers) {
                        val loc = locationManager.getLastKnownLocation(provider) ?: continue
                        if (bestLocation == null || loc.time > bestLocation.time) {
                            bestLocation = loc
                        }
                    }
                    if (bestLocation != null) return bestLocation
                }
            }
        } catch (_: Exception) {
            // ignore
        }

        // Fallback user location (Oshawa center) if GPS is unavailable
        return Location("fallback").apply {
            latitude = 43.8975
            longitude = -78.8658
        }
    }

    fun calculateDistance(context: Context, listingLocationStr: String): String {
        if (listingLocationStr.isBlank()) return "Nearby"
        val userLoc = getUserLocation(context)

        val listingPoint = parseLocation(context, listingLocationStr)
        val results = FloatArray(1)
        Location.distanceBetween(
            userLoc.latitude, userLoc.longitude,
            listingPoint.first, listingPoint.second,
            results
        )
        val distanceMeters = results[0]

        if (distanceMeters < 50) return "0.0 km away"

        return if (distanceMeters < 1000) {
            "${distanceMeters.toInt()} m away"
        } else {
            String.format(Locale.getDefault(), "%.1f km away", distanceMeters / 1000f)
        }
    }

    private fun parseLocation(context: Context, loc: String): Pair<Double, Double> {
        val cleaned = loc.lowercase(Locale.getDefault()).trim()

        // 1. Check city dictionary
        for ((city, coords) in cityCoordinates) {
            if (cleaned.contains(city)) {
                return coords
            }
        }

        // 2. Check lat,lon format
        val parts = loc.split(",")
        if (parts.size == 2) {
            val lat = parts[0].trim().toDoubleOrNull()
            val lon = parts[1].trim().toDoubleOrNull()
            if (lat != null && lon != null) {
                return Pair(lat, lon)
            }
        }

        // 3. Try Geocoder
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(loc, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                return Pair(address.latitude, address.longitude)
            }
        } catch (_: Exception) {
            // ignore
        }

        // 4. Default fallback offset based on string hash if no match found
        return Pair(43.65 + (Math.abs(loc.hashCode()) % 5) * 0.1, -79.38 - (Math.abs(loc.hashCode()) % 5) * 0.1)
    }
}
