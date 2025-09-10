package com.xyz.strapp.presentation.homescreen // Adjust package as needed

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.compose.ui.text.intl.Locale
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

data class CurrentLocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String,
)

sealed interface LocationUiState {
    object Idle : LocationUiState
    object Fetching : LocationUiState
    data class Success(val location: CurrentLocationData) : LocationUiState
    data class Error(val message: String) : LocationUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    @ApplicationContext private val applicationContext: Context // For permission check
) : ViewModel() {

    private val _locationState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)
    val locationState: StateFlow<LocationUiState> = _locationState.asStateFlow()

    private var cancellationTokenSource: CancellationTokenSource? = null

    companion object {
        private const val TAG = "HomeViewModel"
    }

    fun fetchCurrentUserLocation() {
        // Permission check is crucial. UI should request, ViewModel can double-check.
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            _locationState.value = LocationUiState.Error("Location permissions not granted.")
            Log.w(TAG, "Attempted to fetch location without permissions.")
            return
        }

        _locationState.value = LocationUiState.Fetching
        cancellationTokenSource?.cancel() // Cancel previous request if any
        cancellationTokenSource = CancellationTokenSource()

        viewModelScope.launch {
            try {
                val location: Location? = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource?.token
                ).await() // kotlinx-coroutines-play-services

                if (location != null) {
                    val address = getAddressFromLocation(context = applicationContext, location) ?: ""
                    val currentLocationData = CurrentLocationData(location.latitude, location.longitude, address)
                    _locationState.value = LocationUiState.Success(currentLocationData)
                    Log.i(TAG, "Location fetched: $currentLocationData")
                } else {
                    _locationState.value = LocationUiState.Error("Failed to get current location (location was null).")
                    Log.e(TAG, "Fetched location was null.")
                }
            } catch (e: SecurityException) {
                _locationState.value = LocationUiState.Error("Location permission issue: ${e.message}")
                Log.e(TAG, "SecurityException fetching location", e)
            } catch (e: Exception) {
                _locationState.value = LocationUiState.Error("Error fetching location: ${e.message}")
                Log.e(TAG, "Exception fetching location", e)
            } finally {
                cancellationTokenSource = null
            }
        }
    }

    suspend fun getAddressFromLocation(context: Context, location: Location): String? {
        // Geocoder can sometimes be flaky or slow, so run it on a background thread.
        return withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context, java.util.Locale.getDefault())
            var addressText: String? = null

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13 (API 33) and above has a new way with a listener
                    // For simplicity, this example will focus on the older synchronous method
                    // which is still widely used and works, but you should be aware of the async one.
                    // geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses -> ... }
                    // However, to keep it simple and compatible with older suspend function style:
                    val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address: Address = addresses[0]
                        // You can build a more detailed address string if needed
                        // address.getAddressLine(0) often gives a full address.
                        // address.locality would be city
                        // address.adminArea would be state
                        // address.countryName
                        addressText = address.getAddressLine(0) // Example: "1600 Amphitheatre Parkway, Mountain View, CA"
                        Log.i("Geocoder", "Address found: $addressText")
                    } else {
                        Log.w("Geocoder", "No address found for the location.")
                    }
                } else {
                    // For versions below Android 13 (API 33)
                    @Suppress("DEPRECATION") // getFromLocation is deprecated but necessary for older versions
                    val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address: Address = addresses[0]
                        addressText = address.getAddressLine(0)
                        Log.i("Geocoder", "Address found (legacy): $addressText")
                    } else {
                        Log.w("Geocoder", "No address found for the location (legacy).")
                    }
                }
            } catch (e: IOException) {
                // Network or other I/O problems
                Log.e("Geocoder", "Geocoder failed due to IOException", e)
                addressText = "Geocoder service not available"
            } catch (e: IllegalArgumentException) {
                // Invalid latitude or longitude
                Log.e("Geocoder", "Invalid latitude/longitude used for Geocoder", e)
                addressText = "Invalid location coordinates"
            } catch (e: Exception) {
                // Any other unexpected error
                Log.e("Geocoder", "An unexpected error occurred in Geocoder", e)
                addressText = "Could not retrieve address"
            }
            addressText
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancellationTokenSource?.cancel()
        Log.d(TAG, "HomeViewModel cleared, location request cancelled if active.")
    }
}
