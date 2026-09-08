package org.etrange.towards.ui.home

import org.etrange.towards.domain.model.GeocodeResult

fun GeocodeResult.subtitle(): String {
    val address = listOfNotNull(
        listOfNotNull(street, houseNumber).joinToString(" ").ifBlank { null },
        postalCode,
        country,
    ).joinToString(", ")
    return address.ifBlank { kind.name.lowercase().replaceFirstChar { it.titlecase() } }
}
