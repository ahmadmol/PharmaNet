package com.pharmalink.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume

data class FacilityLocation(
    val latitude: Double,
    val longitude: Double,
    val areaName: String,
)

interface FacilityLocationService {
    suspend fun getCurrentFacilityLocation(): Result<FacilityLocation>

    /**
     * Like [getCurrentFacilityLocation] but intended for explicit "refresh" actions.
     * Should avoid returning stale cached values.
     */
    suspend fun getFreshFacilityLocation(): Result<FacilityLocation>
}

class AndroidFacilityLocationService @Inject constructor(
    @ApplicationContext private val context: Context,
) : FacilityLocationService {


    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override suspend fun getCurrentFacilityLocation(): Result<FacilityLocation> = runCatching {
        require(hasLocationPermission()) { "LOCATION_PERMISSION_DENIED" }
        require(isLocationEnabled()) { "LOCATION_DISABLED" }

        val location = getBestAvailableLocation()
            ?: error("LOCATION_UNAVAILABLE")

        FacilityLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            areaName = runCatching {
                reverseGeocode(location.latitude, location.longitude)
            }.getOrElse {
                String.format(Locale.US, "%.6f, %.6f", location.latitude, location.longitude)
            },
        )
    }

    override suspend fun getFreshFacilityLocation(): Result<FacilityLocation> = runCatching {
        require(hasLocationPermission()) { "LOCATION_PERMISSION_DENIED" }
        require(isLocationEnabled()) { "LOCATION_DISABLED" }

        // Avoid returning cached lastLocation to make "refresh/current" explicit actions effective.
        val location = getFreshLocation()
            ?: error("LOCATION_UNAVAILABLE")

        FacilityLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            areaName = runCatching {
                reverseGeocode(location.latitude, location.longitude)
            }.getOrElse {
                String.format(Locale.US, "%.6f, %.6f", location.latitude, location.longitude)
            },
        )
    }


    private fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        return hasFine || hasCoarse
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching { locationManager.isLocationEnabled }.getOrElse {
            runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ||
                runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        }
    }

    private suspend fun getBestAvailableLocation(): Location? {
        val lastKnownLocation = runCatching { fusedLocationClient.lastLocation.awaitResult() }.getOrNull()
        if (lastKnownLocation != null) return lastKnownLocation

        return getFreshLocation()
    }

    private suspend fun getFreshLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val tokenSource = CancellationTokenSource()
            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

            fusedLocationClient.getCurrentLocation(
                priority,
                tokenSource.token,
            ).addOnSuccessListener { location ->
                if (continuation.isActive) continuation.resume(location)
            }.addOnFailureListener {
                if (continuation.isActive) continuation.resume(null)
            }
            continuation.invokeOnCancellation { tokenSource.cancel() }
        }
    }


    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String =
        withContext(Dispatchers.IO) {
            val endpoint = buildString {
                append("https://nominatim.openstreetmap.org/reverse?")
                append("format=jsonv2")
                append("&lat=")
                append(URLEncoder.encode(latitude.toString(), Charsets.UTF_8.name()))
                append("&lon=")
                append(URLEncoder.encode(longitude.toString(), Charsets.UTF_8.name()))
                append("&accept-language=ar")
            }

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Sydaliti/1.0 (facility-location)")
            }

            try {
                val payload = connection.inputStream.bufferedReader().use { it.readText() }
                val root = Json.parseToJsonElement(payload).jsonObject
                val address = root["address"]?.jsonObject
                listOf(
                    "suburb",
                    "neighbourhood",
                    "quarter",
                    "city_district",
                    "residential",
                    "town",
                    "village",
                    "city",
                    "state",
                ).firstNotNullOfOrNull { key ->
                    address?.get(key)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                } ?: root["display_name"]?.jsonPrimitive?.contentOrNull?.substringBefore(",")?.trim()
                    ?: String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
            } finally {
                connection.disconnect()
            }
        }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T? =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener {
            if (continuation.isActive) continuation.resume(null)
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.resume(null)
        }
    }
