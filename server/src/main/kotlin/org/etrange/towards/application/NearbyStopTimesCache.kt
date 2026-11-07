package org.etrange.towards.application

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.StopTimes

/** About 100 m. Callers in the same cell share one upstream stop-times fetch per minute. */
internal const val NEARBY_CELL_METERS = 100

data class NearbyCell(val latitudeBucket: Int, val longitudeBucket: Int)

internal fun Coordinate.nearbyCell(meters: Int = NEARBY_CELL_METERS): NearbyCell {
    val latStep = meters / METERS_PER_DEGREE
    val latitudeBucket = floor(latitude / latStep).toInt()
    val lonStep = longitudeStep(latitude = (latitudeBucket + 0.5) * latStep, meters = meters)
    return NearbyCell(
        latitudeBucket = latitudeBucket,
        longitudeBucket = floor(longitude / lonStep).toInt(),
    )
}

fun NearbyCell.center(meters: Int = NEARBY_CELL_METERS): Coordinate {
    val latStep = meters / METERS_PER_DEGREE
    val latitude = (latitudeBucket + 0.5) * latStep
    val lonStep = longitudeStep(latitude, meters)
    return Coordinate(
        latitude = latitude.coerceIn(-90.0, 90.0),
        longitude = ((longitudeBucket + 0.5) * lonStep).coerceIn(-180.0, 180.0),
    )
}

class NearbyStopTimesCache(
    private val maxEntries: Int = 128,
) {
    private val entries = ConcurrentHashMap<Key, StopTimes>()
    private val locks = ConcurrentHashMap<Key, Mutex>()

    suspend fun getOrLoad(cell: NearbyCell, minute: Long, load: suspend () -> StopTimes): StopTimes {
        val key = Key(cell, minute)
        entries[key]?.let { return it }
        val mutex = locks.computeIfAbsent(key) { Mutex() }
        return mutex.withLock {
            entries[key]?.let { return@withLock it }
            val loaded = load()
            entries[key] = loaded
            evict(keeping = key)
            loaded
        }
    }

    private fun evict(keeping: Key) {
        entries.keys.filter { it.minute != keeping.minute }.forEach { expired ->
            entries.remove(expired)
            locks.remove(expired)
        }
        while (entries.size > maxEntries) {
            val extra = entries.keys.firstOrNull { it != keeping } ?: break
            entries.remove(extra)
            locks.remove(extra)
        }
    }

    private data class Key(val cell: NearbyCell, val minute: Long)
}

private const val METERS_PER_DEGREE = 111_320.0

private fun longitudeStep(latitude: Double, meters: Int): Double {
    val cosLat = abs(cos(latitude * PI / 180.0)).coerceAtLeast(0.2)
    return meters / (METERS_PER_DEGREE * cosLat)
}
