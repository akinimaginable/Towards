package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class OneToManyParams(
    val one: String,
    val many: List<String>,
    val mode: Mode,
    val max: Double,
    val maxMatchingDistance: Double,
    val arriveBy: Boolean,
    val elevationCosts: ElevationCosts? = null,
    val withDistance: Boolean? = null,
)

@Serializable
data class OneToManyIntermodalParams(
    val one: String,
    val many: List<String>,
    val time: String? = null,
    val maxTravelTime: Int? = null,
    val maxMatchingDistance: Double? = null,
    val arriveBy: Boolean? = null,
    val maxTransfers: Int? = null,
    val minTransferTime: Int? = null,
    val additionalTransferTime: Int? = null,
    val transferTimeFactor: Double? = null,
    val useRoutedTransfers: Boolean? = null,
    val pedestrianProfile: PedestrianProfile? = null,
    val pedestrianSpeed: PedestrianSpeed? = null,
    val cyclingSpeed: CyclingSpeed? = null,
    val elevationCosts: ElevationCosts? = null,
    val transitModes: List<Mode>? = null,
    val preTransitModes: List<Mode>? = null,
    val postTransitModes: List<Mode>? = null,
    val directMode: Mode? = null,
    val maxPreTransitTime: Int? = null,
    val maxPostTransitTime: Int? = null,
    val maxDirectTime: Int? = null,
    val withDistance: Boolean? = null,
    val requireBikeTransport: Boolean? = null,
    val requireCarTransport: Boolean? = null,
)
