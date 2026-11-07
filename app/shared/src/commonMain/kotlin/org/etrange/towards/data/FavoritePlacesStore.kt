package org.etrange.towards.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.etrange.towards.domain.model.Coordinate
import kotlin.time.Clock

enum class FavoriteRole {
    HOME,
    WORK,
    OTHER,
}

data class FavoriteLocation(
    val name: String,
    val coordinate: Coordinate,
    val stopId: String? = null,
)

data class FavoritePlace(
    val id: String,
    val label: String,
    val role: FavoriteRole,
    val name: String,
    val coordinate: Coordinate,
    val stopId: String? = null,
)

sealed interface FavoriteSaveResult {
    data object Success : FavoriteSaveResult
    data class Failure(val message: String) : FavoriteSaveResult
}

/**
 * Persists Home, Work, and other named places so they can be offered as trip shortcuts.
 */
class FavoritePlacesStore(
    private val settings: Settings = Settings(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _places = MutableStateFlow(read())
    val places: StateFlow<List<FavoritePlace>> = _places.asStateFlow()

    private val lock = Any()
    private var sequence = 0

    fun setHome(location: FavoriteLocation) {
        publish { current ->
            listOf(slot(HOME_ID, HOME_LABEL, FavoriteRole.HOME, location)) +
                current.filter { it.role != FavoriteRole.HOME }
        }
    }

    fun setWork(location: FavoriteLocation) {
        publish { current ->
            listOf(slot(WORK_ID, WORK_LABEL, FavoriteRole.WORK, location)) +
                current.filter { it.role != FavoriteRole.WORK }
        }
    }

    fun add(label: String, location: FavoriteLocation): FavoriteSaveResult {
        var result: FavoriteSaveResult = FavoriteSaveResult.Success
        publish { current ->
            labelError(label, current)?.let { message ->
                result = FavoriteSaveResult.Failure(message)
                return@publish current
            }
            val trimmed = label.trim()
            result = FavoriteSaveResult.Success
            current + FavoritePlace(
                id = newId(),
                label = trimmed,
                role = FavoriteRole.OTHER,
                name = location.name.ifBlank { trimmed },
                coordinate = location.coordinate,
                stopId = location.stopId?.takeIf { it.isNotBlank() },
            )
        }
        return result
    }

    fun replace(id: String, location: FavoriteLocation): FavoriteSaveResult {
        var result: FavoriteSaveResult = FavoriteSaveResult.Success
        publish { current ->
            val existing = current.find { it.id == id }
            if (existing == null) {
                result = FavoriteSaveResult.Failure(MISSING_PLACE_MESSAGE)
                return@publish current
            }
            result = FavoriteSaveResult.Success
            current.map { place ->
                if (place.id != existing.id) {
                    place
                } else {
                    place.copy(
                        name = location.name.ifBlank { place.label },
                        coordinate = location.coordinate,
                        stopId = location.stopId?.takeIf { it.isNotBlank() },
                    )
                }
            }
        }
        return result
    }

    fun remove(id: String) {
        publish { current -> current.filter { it.id != id } }
    }

    fun labelError(label: String): String? = labelError(label, _places.value)

    private fun labelError(label: String, places: List<FavoritePlace>): String? {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return BLANK_LABEL_MESSAGE
        if (trimmed.equals(HOME_LABEL, ignoreCase = true) || trimmed.equals(WORK_LABEL, ignoreCase = true)) {
            return RESERVED_LABEL_MESSAGE
        }
        if (places.any { it.role == FavoriteRole.OTHER && it.label.equals(trimmed, ignoreCase = true) }) {
            return DUPLICATE_LABEL_MESSAGE
        }
        if (places.count { it.role == FavoriteRole.OTHER } >= MAX_OTHERS) {
            return TOO_MANY_MESSAGE
        }
        return null
    }

    private fun slot(
        id: String,
        label: String,
        role: FavoriteRole,
        location: FavoriteLocation,
    ) = FavoritePlace(
        id = id,
        label = label,
        role = role,
        name = location.name.ifBlank { label },
        coordinate = location.coordinate,
        stopId = location.stopId?.takeIf { it.isNotBlank() },
    )

    private fun publish(transform: (List<FavoritePlace>) -> List<FavoritePlace>) {
        synchronized(lock) {
            val ordered = _places.updateAndGet { order(transform(it)) }
            settings[KEY] = json.encodeToString(
                ListSerializer(StoredFavorite.serializer()),
                ordered.map { it.toStored() },
            )
        }
    }

    private fun newId(): String {
        sequence += 1
        return "place-${Clock.System.now().toEpochMilliseconds()}-$sequence"
    }

    private fun read(): List<FavoritePlace> {
        val raw = settings.getStringOrNull(KEY) ?: return emptyList()
        val stored = runCatching {
            json.decodeFromString(ListSerializer(StoredFavorite.serializer()), raw)
        }.getOrNull() ?: return emptyList()
        var home: FavoritePlace? = null
        var work: FavoritePlace? = null
        val others = mutableListOf<FavoritePlace>()
        for (entry in stored) {
            val place = entry.toPlaceOrNull() ?: continue
            when (place.role) {
                FavoriteRole.HOME -> if (home == null) {
                    home = place.copy(id = HOME_ID, label = HOME_LABEL)
                }
                FavoriteRole.WORK -> if (work == null) {
                    work = place.copy(id = WORK_ID, label = WORK_LABEL)
                }
                FavoriteRole.OTHER -> {
                    val reserved = place.label.equals(HOME_LABEL, ignoreCase = true) ||
                        place.label.equals(WORK_LABEL, ignoreCase = true)
                    val duplicate = others.any { it.label.equals(place.label, ignoreCase = true) }
                    if (!reserved && !duplicate && others.size < MAX_OTHERS && place.id.isNotBlank()) {
                        others += place
                    }
                }
            }
        }
        return order(listOfNotNull(home, work) + others)
    }

    companion object {
        const val KEY = "favorite_places"
        const val MAX_OTHERS = 10
        const val HOME_ID = "home"
        const val WORK_ID = "work"
        const val HOME_LABEL = "Home"
        const val WORK_LABEL = "Work"
        const val NEW_TARGET = "new"
        const val BLANK_LABEL_MESSAGE = "Enter a name"
        const val RESERVED_LABEL_MESSAGE = "Home and Work are reserved"
        const val DUPLICATE_LABEL_MESSAGE = "You already have a place with that name"
        const val TOO_MANY_MESSAGE = "You can save up to $MAX_OTHERS places"
        const val MISSING_PLACE_MESSAGE = "That place is no longer saved"
    }
}

private fun order(places: List<FavoritePlace>): List<FavoritePlace> {
    val home = places.firstOrNull { it.role == FavoriteRole.HOME }
    val work = places.firstOrNull { it.role == FavoriteRole.WORK }
    val others = places.filter { it.role == FavoriteRole.OTHER }
    return listOfNotNull(home, work) + others
}

@Serializable
private data class StoredFavorite(
    val id: String,
    val label: String,
    val role: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val stopId: String? = null,
)

private fun FavoritePlace.toStored() = StoredFavorite(
    id = id,
    label = label,
    role = role.name,
    name = name,
    latitude = coordinate.latitude,
    longitude = coordinate.longitude,
    stopId = stopId,
)

private fun StoredFavorite.toPlaceOrNull(): FavoritePlace? {
    val role = runCatching { FavoriteRole.valueOf(role) }.getOrNull() ?: return null
    if (name.isBlank() || label.isBlank()) return null
    val coordinate = runCatching {
        Coordinate(latitude = latitude, longitude = longitude)
    }.getOrNull() ?: return null
    return FavoritePlace(
        id = id,
        label = label,
        role = role,
        name = name,
        coordinate = coordinate,
        stopId = stopId?.takeIf { it.isNotBlank() },
    )
}
