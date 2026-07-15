package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class ReachablePlace(
    val place: Place? = null,
    val duration: Int? = null,
    val k: Int? = null,
)

@Serializable
data class Reachable(
    val one: Place? = null,
    val all: List<ReachablePlace>? = null,
)
