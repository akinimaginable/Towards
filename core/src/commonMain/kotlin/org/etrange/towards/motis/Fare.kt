package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class RiderCategory(
    val riderCategoryName: String,
    val isDefaultFareCategory: Boolean,
    val eligibilityUrl: String? = null,
)

@Serializable
data class FareMedia(
    val fareMediaType: FareMediaType,
    val fareMediaName: String? = null,
)

@Serializable
data class FareProduct(
    val name: String,
    val amount: Double,
    val currency: String,
    val riderCategory: RiderCategory? = null,
    val media: FareMedia? = null,
)

@Serializable
data class FareTransfer(
    val effectiveFareLegProducts: List<List<List<FareProduct>>>,
    val rule: FareTransferRule? = null,
    val transferProducts: List<FareProduct>? = null,
)
