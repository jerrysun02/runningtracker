package com.myprojects.modules.runningtracker.util

import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun formatTime(milliseconds: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds))
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))

    return String.format(Locale.getDefault(),"%02d:%02d:%02d", hours, minutes, seconds)
}

fun calculateDistance(locations: List<List<LatLng>>): Float {
    var totalDistance = 0f
    if (locations.isEmpty()) return totalDistance

    val flattenedLocations = locations.flatten()
    if (flattenedLocations.size < 2) return totalDistance

    for (i in 0 until flattenedLocations.size - 1) {
        val loc1 = flattenedLocations[i]
        val loc2 = flattenedLocations[i + 1]
        totalDistance += haversineDistance(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude)
    }
    return totalDistance
}

fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val R = 6371f // Radius of Earth in kilometers

    val latDistance = Math.toRadians(lat2 - lat1)
    val lonDistance = Math.toRadians(lon2 - lon1)

    val a = sin(latDistance / 2) * sin(latDistance / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(lonDistance / 2) * sin(lonDistance / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return (R * c * 1000f).toFloat() // Convert to meters
}
