package org.etrange.towards.motis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Area(
    val name: String,
    val adminLevel: Double,
    val matched: Boolean,
    val unique: Boolean? = null,
    @SerialName("default")
    val isDefault: Boolean? = null,
)

@Serializable
data class Match(
    val type: LocationType,
    val name: String,
    val id: String,
    val lat: Double,
    val lon: Double,
    val tokens: List<Token>,
    val areas: List<Area>,
    val score: Double,
    val category: String? = null,
    val level: Double? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val country: String? = null,
    val zip: String? = null,
    val tz: String? = null,
    val modes: List<Mode>? = null,
    val importance: Double? = null,
)
