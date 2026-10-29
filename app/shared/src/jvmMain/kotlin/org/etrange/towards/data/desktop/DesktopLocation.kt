package org.etrange.towards.data.desktop

import kotlin.coroutines.cancellation.CancellationException
import org.etrange.towards.domain.model.Coordinate

internal enum class LocationAuthorization {
    NotDetermined,
    Denied,
    Granted,
    Unavailable,
}

internal interface DesktopLocation {
    fun authorization(): LocationAuthorization

    fun lastKnown(): Coordinate?

    suspend fun requestPermission(): Boolean

    suspend fun current(): Coordinate?

    fun cancel()
}

internal object UnavailableLocation : DesktopLocation {
    override fun authorization(): LocationAuthorization = LocationAuthorization.Unavailable

    override fun lastKnown(): Coordinate? = null

    override suspend fun requestPermission(): Boolean = false

    override suspend fun current(): Coordinate? = null

    override fun cancel() = Unit
}

internal fun coordinateOrNull(latitude: Double, longitude: Double): Coordinate? =
    try {
        Coordinate(latitude, longitude)
    } catch (_: IllegalArgumentException) {
        null
    }

internal suspend fun awaitLocationPermission(
    location: DesktopLocation,
    onResult: (Boolean) -> Unit,
) {
    val granted = try {
        location.requestPermission()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        false
    }
    onResult(granted)
}
