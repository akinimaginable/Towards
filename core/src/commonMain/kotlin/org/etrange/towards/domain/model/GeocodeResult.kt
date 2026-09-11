package org.etrange.towards.domain.model

data class GeocodeResult(
    val id: String,
    val kind: LocationKind,
    val name: String,
    val coordinate: Coordinate,
    val country: String? = null,
    val postalCode: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val modes: Set<TransportMode> = emptySet(),
)

