package org.etrange.towards.ui.home

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.map.MapBounds

/** Stops are only fetched and drawn from this zoom; below it a viewport holds too many. */
internal const val MapStopsMinZoom = 14.0

/** Grows the bounds by [fraction] of their size on every side. */
internal fun MapBounds.expandedBy(fraction: Double): MapBounds {
    val latitudePadding = (maximum.latitude - minimum.latitude) * fraction
    val longitudePadding = (maximum.longitude - minimum.longitude) * fraction
    return MapBounds(
        minimum = Coordinate(
            latitude = (minimum.latitude - latitudePadding).coerceAtLeast(-90.0),
            longitude = (minimum.longitude - longitudePadding).coerceAtLeast(-180.0),
        ),
        maximum = Coordinate(
            latitude = (maximum.latitude + latitudePadding).coerceAtMost(90.0),
            longitude = (maximum.longitude + longitudePadding).coerceAtMost(180.0),
        ),
    )
}

internal operator fun MapBounds.contains(other: MapBounds): Boolean =
    other.minimum.latitude >= minimum.latitude &&
        other.minimum.longitude >= minimum.longitude &&
        other.maximum.latitude <= maximum.latitude &&
        other.maximum.longitude <= maximum.longitude
