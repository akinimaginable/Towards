package org.etrange.towards.domain.model.map

import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.TransportMode

data class MapTrip(
    val tripIds: List<String>,
    val mode: TransportMode,
    val from: Place,
    val to: Place,
    val departure: String,
    val arrival: String,
    val polyline: String,
    val routeColor: String? = null,
)
