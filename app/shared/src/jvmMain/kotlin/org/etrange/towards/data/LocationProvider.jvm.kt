package org.etrange.towards.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.etrange.towards.data.desktop.DesktopLocation
import org.etrange.towards.data.desktop.LocationAuthorization
import org.etrange.towards.data.desktop.awaitLocationPermission
import org.etrange.towards.data.desktop.desktopLocation
import org.etrange.towards.domain.model.Coordinate

class JvmLocationProvider internal constructor(
    private val location: DesktopLocation,
    private val fixTimeout: Duration = 15.seconds,
) : LocationProvider {
    override fun hasPermission(): Boolean =
        location.authorization() == LocationAuthorization.Granted

    override fun lastKnownCoordinate(): Coordinate? {
        if (!hasPermission()) return null
        return location.lastKnown()
    }

    override suspend fun currentCoordinate(): Coordinate? {
        if (!hasPermission()) return null
        return try {
            withTimeout(fixTimeout) { location.current() }
        } catch (_: TimeoutCancellationException) {
            null
        }
    }

    internal suspend fun requestPermission(): Boolean = location.requestPermission()
}

@Composable
actual fun rememberLocationProvider(): LocationProvider =
    remember { JvmLocationProvider(desktopLocation()) }

@Composable
actual fun rememberLocationPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val currentOnResult = rememberUpdatedState(onResult)
    val location = remember { desktopLocation() }
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            awaitLocationPermission(location) { granted ->
                currentOnResult.value(granted)
            }
        }
    }
}
