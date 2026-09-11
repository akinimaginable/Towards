package org.etrange.towards.domain.model

data class Place(
    val id: String?,
    val name: String,
    val coordinate: Coordinate,
    val parentId: String? = null,
    val timezone: String? = null,
    val platform: String? = null,
    val modes: Set<TransportMode> = emptySet(),
)
