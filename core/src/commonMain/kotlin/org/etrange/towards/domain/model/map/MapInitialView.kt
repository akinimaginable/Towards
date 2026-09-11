package org.etrange.towards.domain.model.map

import org.etrange.towards.domain.model.Coordinate

data class MapInitialView(
    val center: Coordinate,
    val zoom: Double,
    val motisVersion: String,
    val streetRoutingAvailable: Boolean,
)
