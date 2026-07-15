package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class EncodedPolyline(
    val points: String,
    val precision: Int,
    val length: Int,
)

@Serializable
data class StepInstruction(
    val relativeDirection: Direction,
    val distance: Double,
    val fromLevel: Double,
    val toLevel: Double,
    val polyline: EncodedPolyline,
    val streetName: String,
    val exit: String,
    val stayOn: Boolean,
    val area: Boolean,
    val osmWay: Int? = null,
    val toll: Boolean? = null,
    val accessRestriction: String? = null,
    val elevationUp: Int? = null,
    val elevationDown: Int? = null,
)
