package org.etrange.towards.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import org.etrange.towards.domain.model.Coordinate
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider : LocationProvider {
    private val manager = CLLocationManager()

    // CLLocationManager.delegate is weak — must keep a strong Kotlin reference.
    private var activeDelegate: NSObject? = null

    override fun hasPermission(): Boolean = manager.authorizationStatus.isAuthorized()

    override suspend fun currentCoordinate(): Coordinate? =
        suspendCancellableCoroutine { continuation ->
            fun finish(coordinate: Coordinate?) {
                activeDelegate = null
                manager.delegate = null
                manager.stopUpdatingLocation()
                if (continuation.isActive) {
                    continuation.resume(coordinate)
                }
            }

            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(
                    manager: CLLocationManager,
                    didUpdateLocations: List<*>,
                ) {
                    val location = didUpdateLocations.lastOrNull() as? CLLocation
                    val coordinate = location?.coordinate?.useContents {
                        Coordinate(latitude, longitude)
                    }
                    finish(coordinate)
                }

                override fun locationManager(
                    manager: CLLocationManager,
                    didFailWithError: NSError,
                ) {
                    finish(null)
                }

                override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                    handleAuthorizationChange(manager.authorizationStatus)
                }

                override fun locationManager(
                    manager: CLLocationManager,
                    didChangeAuthorizationStatus: CLAuthorizationStatus,
                ) {
                    handleAuthorizationChange(didChangeAuthorizationStatus)
                }

                private fun handleAuthorizationChange(status: CLAuthorizationStatus) {
                    when {
                        status.isAuthorized() -> manager.requestLocation()
                        status == kCLAuthorizationStatusDenied ||
                            status == kCLAuthorizationStatusRestricted -> finish(null)
                        status == kCLAuthorizationStatusNotDetermined -> Unit
                        else -> finish(null)
                    }
                }
            }

            activeDelegate = delegate
            continuation.invokeOnCancellation {
                activeDelegate = null
                manager.delegate = null
                manager.stopUpdatingLocation()
            }

            dispatch_async(dispatch_get_main_queue()) {
                if (!continuation.isActive) return@dispatch_async
                manager.delegate = delegate
                when (val status = manager.authorizationStatus) {
                    kCLAuthorizationStatusNotDetermined ->
                        manager.requestWhenInUseAuthorization()
                    else -> {
                        if (status.isAuthorized()) {
                            manager.requestLocation()
                        } else {
                            finish(null)
                        }
                    }
                }
            }
        }
}

private fun CLAuthorizationStatus.isAuthorized(): Boolean =
    this == kCLAuthorizationStatusAuthorizedAlways ||
        this == kCLAuthorizationStatusAuthorizedWhenInUse

@Composable
actual fun rememberLocationProvider(): LocationProvider =
    remember { IosLocationProvider() }

@Composable
actual fun rememberLocationPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit {
    return {
        val status = CLLocationManager().authorizationStatus
        onResult(status.isAuthorized() || status == kCLAuthorizationStatusNotDetermined)
    }
}
