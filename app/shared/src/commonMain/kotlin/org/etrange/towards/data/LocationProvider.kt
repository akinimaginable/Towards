package org.etrange.towards.data

import org.etrange.towards.domain.model.Coordinate

interface LocationProvider {
    fun hasPermission(): Boolean

    /**
     * Returns the device's current coordinate, or null when unavailable / denied.
     * Callers should ensure permission is granted on platforms that require it.
     */
    suspend fun currentCoordinate(): Coordinate?
}
