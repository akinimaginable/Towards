package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class TripInfo(
    val tripId: String,
    val routeShortName: String? = null,
    val displayName: String? = null,
)

@Serializable
data class TripSegment(
    val trips: List<TripInfo>,
    val mode: Mode,
    val distance: Double,
    val from: Place,
    val to: Place,
    val departure: String,
    val arrival: String,
    val scheduledDeparture: String,
    val scheduledArrival: String,
    val realTime: Boolean,
    val polyline: String,
    val routeColor: String? = null,
)
