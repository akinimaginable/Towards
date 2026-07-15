package org.etrange.towards.motis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Duration(
    val duration: Double? = null,
    val distance: Double? = null,
)

@Serializable
data class ParetoSetEntry(
    val duration: Double,
    val transfers: Int,
)

@Serializable
data class OneToManyIntermodalResponse(
    @SerialName("street_durations")
    val streetDurations: List<Duration>? = null,
    @SerialName("transit_durations")
    val transitDurations: List<ParetoSet>? = null,
)
