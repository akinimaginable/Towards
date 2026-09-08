package org.etrange.towards.ui.home

import org.etrange.towards.domain.model.Coordinate

/** Minimum camera-target move before nearby focus is refreshed. */
internal const val MAP_FOCUS_MIN_DISTANCE_METERS = 50

/**
 * Returns true when [next] should replace [current] as the nearby-query focus.
 * Tiny pans (under [minDistanceMeters]) are ignored to avoid reload spam.
 */
internal fun shouldUpdateMapFocus(
    current: Coordinate?,
    next: Coordinate,
    minDistanceMeters: Int = MAP_FOCUS_MIN_DISTANCE_METERS,
): Boolean {
    if (current == null) return true
    return haversineMeters(current, next) >= minDistanceMeters
}
