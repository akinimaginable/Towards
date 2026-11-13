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
    init {
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    private val entries = ConcurrentHashMap<Key, StopTimes>()
    private val locks = ConcurrentHashMap<Key, LoadLock>()

    suspend fun getOrLoad(cell: NearbyCell, minute: Long, load: suspend () -> StopTimes): StopTimes {
        val key = Key(cell, minute)
        entries[key]?.let { return it }
        val lock = locks.compute(key) { _, existing ->
            (existing ?: LoadLock()).also { it.users += 1 }
        }!!
        try {
            return lock.mutex.withLock {
                entries[key] ?: load().also { loaded ->
                    entries[key] = loaded
                    evict(keeping = key)
                }
            }
        } finally {
            // Registration and release are atomic per key, including waiting callers.
            locks.compute(key) { _, current ->
                check(current === lock)
                lock.users -= 1
                lock.takeIf { it.users > 0 }
            }
        }
    }

    internal fun retainedEntryCount(): Int = entries.size

    internal fun retainedLockCount(): Int = locks.size

    private fun evict(keeping: Key) {
        entries.keys.filter { it.minute != keeping.minute }.forEach { expired ->
            entries.remove(expired)
        }
        while (entries.size > maxEntries) {
            val extra = entries.keys.firstOrNull { it != keeping } ?: break
            entries.remove(extra)
        }
    }

    private class LoadLock(val mutex: Mutex = Mutex(), var users: Int = 0)

    private data class Key(val cell: NearbyCell, val minute: Long)
}

private const val METERS_PER_DEGREE = 111_320.0

private fun longitudeStep(latitude: Double, meters: Int): Double {
    val cosLat = abs(cos(latitude * PI / 180.0)).coerceAtLeast(0.2)
    return meters / (METERS_PER_DEGREE * cosLat)
}
