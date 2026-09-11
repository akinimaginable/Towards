package org.etrange.towards.domain.model.map

import org.etrange.towards.domain.model.Coordinate

data class MapBounds(
    val minimum: Coordinate,
    val maximum: Coordinate,
)
