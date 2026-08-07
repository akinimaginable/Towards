package org.etrange.towards.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.etrange.towards.domain.model.Coordinate
import kotlin.coroutines.resume

class AndroidLocationProvider(
    private val context: Context,
) : LocationProvider {

    override fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    override fun lastKnownCoordinate(): Coordinate? {
        if (!hasPermission()) return null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        return lastKnownCoordinate(locationManager)
    }

    @SuppressLint("MissingPermission")
    override suspend fun currentCoordinate(): Coordinate? {
        if (!hasPermission()) return null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        // Prefer fused/network over GPS for a faster first fix.
        val provider = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER) ->
                LocationManager.FUSED_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            else -> null
        } ?: return lastKnownCoordinate(locationManager)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            suspendCancellableCoroutine { continuation ->
                locationManager.getCurrentLocation(
                    provider,
                    null,
                    context.mainExecutor,
                ) { location ->
                    if (continuation.isActive) {
                        continuation.resume(
                            location?.let { Coordinate(it.latitude, it.longitude) }
                                ?: lastKnownCoordinate(locationManager),
                        )
                    }
                }
            }
        } else {
            lastKnownCoordinate(locationManager)
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownCoordinate(locationManager: LocationManager): Coordinate? {
        val candidates = buildList {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let(::add)
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let(::add)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)?.let(::add)
            }
        }
        val best = candidates.maxByOrNull { it.time } ?: return null
        return Coordinate(best.latitude, best.longitude)
    }
}

@Composable
actual fun rememberLocationProvider(): LocationProvider {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidLocationProvider(context) }
}

@Composable
actual fun rememberLocationPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        onResult(grants.values.any { granted -> granted })
    }
    return {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }
}
