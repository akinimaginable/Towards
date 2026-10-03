package org.etrange.towards.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.LocationKind

/**
 * Remembers places the user picked from search so they can be offered again
 * when the search field is empty.
 */
class SearchHistoryStore(
    private val settings: Settings = Settings(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _entries = MutableStateFlow(read())
    val entries: StateFlow<List<GeocodeResult>> = _entries.asStateFlow()

    fun record(result: GeocodeResult) {
        val next = buildList {
            add(result)
            for (existing in _entries.value) {
                if (!samePlace(existing, result)) add(existing)
            }
        }.take(MAX_ENTRIES)
        settings[KEY] = json.encodeToString(
            ListSerializer(StoredSearchEntry.serializer()),
            next.map { it.toStored() },
        )
        _entries.value = next
    }

    fun clear() {
        settings.remove(KEY)
        _entries.value = emptyList()
    }

    private fun read(): List<GeocodeResult> {
        val raw = settings.getStringOrNull(KEY) ?: return emptyList()
        val stored = runCatching {
            json.decodeFromString<List<StoredSearchEntry>>(raw)
        }.getOrNull() ?: return emptyList()
        return stored.mapNotNull { it.toGeocodeResultOrNull() }.take(MAX_ENTRIES)
    }

    companion object {
        const val KEY = "search_history"
        const val MAX_ENTRIES = 10
    }
}

private fun samePlace(existing: GeocodeResult, incoming: GeocodeResult): Boolean {
    if (existing.id.isNotBlank() && existing.id == incoming.id) return true
    if (existing.id.isBlank() || incoming.id.isBlank()) {
        return existing.name == incoming.name &&
            existing.coordinate.latitude == incoming.coordinate.latitude &&
            existing.coordinate.longitude == incoming.coordinate.longitude
    }
    return false
}

@Serializable
private data class StoredSearchEntry(
    val id: String,
    val kind: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val postalCode: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
)

private fun GeocodeResult.toStored() = StoredSearchEntry(
    id = id,
    kind = kind.name,
    name = name,
    latitude = coordinate.latitude,
    longitude = coordinate.longitude,
    country = country,
    postalCode = postalCode,
    street = street,
    houseNumber = houseNumber,
)

private fun StoredSearchEntry.toGeocodeResultOrNull(): GeocodeResult? {
    val locationKind = runCatching { LocationKind.valueOf(kind) }.getOrNull() ?: return null
    val coordinate = runCatching {
        Coordinate(latitude = latitude, longitude = longitude)
    }.getOrNull() ?: return null
    return GeocodeResult(
        id = id,
        kind = locationKind,
        name = name,
        coordinate = coordinate,
        country = country,
        postalCode = postalCode,
        street = street,
        houseNumber = houseNumber,
    )
}
