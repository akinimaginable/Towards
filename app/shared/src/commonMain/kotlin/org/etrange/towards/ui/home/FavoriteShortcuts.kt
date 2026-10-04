package org.etrange.towards.ui.home

import org.etrange.towards.data.FavoritePlace

fun List<FavoritePlace>.toShortcutItems(): List<DestinationShortcutItem> = map { place ->
    DestinationShortcutItem(
        label = place.label,
        detail = place.name.takeUnless { it.equals(place.label, ignoreCase = true) }.orEmpty(),
        coordinate = place.coordinate,
        stopId = place.stopId,
    )
}
