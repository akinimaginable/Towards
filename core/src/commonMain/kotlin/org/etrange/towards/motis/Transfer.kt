package org.etrange.towards.motis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transfer(
    val to: Place,
    @SerialName("default")
    val defaultDuration: Double? = null,
    val foot: Double? = null,
    val footRouted: Double? = null,
    val wheelchair: Double? = null,
    val wheelchairRouted: Double? = null,
    val wheelchairUsesElevator: Boolean? = null,
    val car: Double? = null,
)

@Serializable
data class TransfersResponse(
    val place: Place,
    val root: Place,
    val equivalences: List<Place>,
    val hasFootTransfers: Boolean,
    val hasWheelchairTransfers: Boolean,
    val hasCarTransfers: Boolean,
    val transfers: List<Transfer>,
)
