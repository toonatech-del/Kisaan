package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.example.util.GeoPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class GpsQualityLevel(val hindiLabel: String, val thresholdDesc: String) {
    EXCELLENT("उत्कृष्ट संकेत (≤ 4 मी.)", "बहुत सटीक"),
    GOOD("अच्छा संकेत (4 - 8 मी.)", "संतोषजनक"),
    MEDIUM("मध्यम संकेत (8 - 15 मी.)", "सावधानी अपेक्षित"),
    WEAK("कमजोर संकेत (> 15 मी.)", "नाप में त्रुटि संभव"),
    UNAVAILABLE("GPS अनुपलब्ध", "सिग्नल नहीं मिला")
}

data class LiveGpsStatus(
    val isGpsEnabled: Boolean = false,
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val currentPoint: GeoPoint? = null,
    val accuracyMeters: Float = 0f,
    val qualityLevel: GpsQualityLevel = GpsQualityLevel.UNAVAILABLE,
    val satelliteCount: Int = 0,
    val warningMessage: String? = null
)

class LocationTracker(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun isGpsProviderEnabled(): Boolean {
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    }

    /**
     * Determines quality level based on reported horizontal accuracy.
     */
    fun evaluateQuality(accuracyMeters: Float): GpsQualityLevel {
        return when {
            accuracyMeters <= 0f -> GpsQualityLevel.UNAVAILABLE
            accuracyMeters <= 4.0f -> GpsQualityLevel.EXCELLENT
            accuracyMeters <= 8.0f -> GpsQualityLevel.GOOD
            accuracyMeters <= 15.0f -> GpsQualityLevel.MEDIUM
            else -> GpsQualityLevel.WEAK
        }
    }

    /**
     * Streams real-time location updates using FusedLocationProvider with system LocationManager fallback.
     */
    @SuppressLint("MissingPermission")
    fun getLocationFlow(intervalMs: Long = 1500L): Flow<Location> = callbackFlow {
        var isFusedWorking = false

        // 1. Try Google Play Services Fused Location
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(0.5f)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                isFusedWorking = true
                result.lastLocation?.let { trySend(it) }
            }
        }

        try {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            // Fused may fail if Play Services not available, fallback to LocationManager
        }

        // 2. Setup System LocationManager listener as reliable fallback
        val systemListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!isFusedWorking) {
                    trySend(location)
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                intervalMs,
                0.5f,
                systemListener,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            // Will throw if permission missing
        }

        awaitClose {
            try {
                fusedClient.removeLocationUpdates(locationCallback)
            } catch (e: Exception) {}
            try {
                locationManager?.removeUpdates(systemListener)
            } catch (e: Exception) {}
        }
    }
}
