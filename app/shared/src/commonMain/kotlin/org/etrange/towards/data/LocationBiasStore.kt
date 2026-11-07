package org.etrange.towards.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import org.etrange.towards.domain.model.Coordinate

/**
 * Persists the last successful map location bias so cold starts can open near the user
 * without waiting for a fresh GPS fix.
 */
class LocationBiasStore(
    private val settings: Settings = Settings(),
) {
    private val lock = Any()

    fun load(): Coordinate? = synchronized(lock) {
        val latitude = settings.getDoubleOrNull(KEY_LATITUDE) ?: return null
        val longitude = settings.getDoubleOrNull(KEY_LONGITUDE) ?: return null
        runCatching { Coordinate(latitude = latitude, longitude = longitude) }.getOrNull()
    }

    fun save(coordinate: Coordinate) {
        synchronized(lock) {
            settings[KEY_LATITUDE] = coordinate.latitude
            settings[KEY_LONGITUDE] = coordinate.longitude
        }
    }

    companion object {
        const val KEY_LATITUDE = "location_bias_latitude"
        const val KEY_LONGITUDE = "location_bias_longitude"
    }
}
