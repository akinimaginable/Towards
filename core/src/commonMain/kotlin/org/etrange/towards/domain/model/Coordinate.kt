package org.etrange.towards.domain.model

data class Coordinate(
    val latitude: Double,
    val longitude: Double,
    val level: Double? = null,
) {
    init {
        require(latitude in -90.0..90.0) { "latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "longitude must be between -180 and 180" }
    }
}
