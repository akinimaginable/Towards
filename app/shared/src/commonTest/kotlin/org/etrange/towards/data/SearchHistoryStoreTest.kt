package org.etrange.towards.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.LocationKind

class SearchHistoryStoreTest {

    @Test
    fun loadsEmptyWhenNothingPersisted() {
        val store = SearchHistoryStore(settings = MapSettings())

        assertEquals(emptyList(), store.entries.value)
    }

    @Test
    fun persistsAndReloadsPlaces() {
        val settings = MapSettings()
        val place = place(id = "stop:central", name = "Bruxelles-Central")

        SearchHistoryStore(settings = settings).record(place)

        assertEquals(listOf(place), SearchHistoryStore(settings = settings).entries.value)
    }

    @Test
    fun movesRepeatOfSameIdToFront() {
        val store = SearchHistoryStore(settings = MapSettings())
        val central = place(id = "stop:central", name = "Bruxelles-Central")
        val updated = central.copy(street = "Rue Infante Isabelle")
        val grandPlace = place(id = "place:gp", name = "Grand Place", latitude = 50.8467)

        store.record(central)
        store.record(grandPlace)
        store.record(updated)

        assertEquals(listOf(updated, grandPlace), store.entries.value)
    }

    @Test
    fun dedupesBlankIdsByNameAndCoordinate() {
        val store = SearchHistoryStore(settings = MapSettings())
        val first = place(id = "", name = "Grand Place")
        val other = place(id = "stop:central", name = "Bruxelles-Central", latitude = 50.84)
        val again = first.copy(street = "Grand Place")

        store.record(first)
        store.record(other)
        store.record(again)

        assertEquals(listOf(again, other), store.entries.value)
    }

    @Test
    fun keepsDistinctIdsThatShareANameAndCoordinate() {
        val store = SearchHistoryStore(settings = MapSettings())
        val stop = place(id = "stop:gp", name = "Grand Place")
        val address = place(id = "addr:gp", name = "Grand Place", kind = LocationKind.ADDRESS)

        store.record(stop)
        store.record(address)

        assertEquals(listOf(address, stop), store.entries.value)
    }

    @Test
    fun capsHistoryAtTenNewestFirst() {
        val store = SearchHistoryStore(settings = MapSettings())

        repeat(SearchHistoryStore.MAX_ENTRIES + 1) { index ->
            store.record(place(id = "place:$index", latitude = 50.0 + index * 0.01))
        }

        val entries = store.entries.value
        assertEquals(SearchHistoryStore.MAX_ENTRIES, entries.size)
        assertEquals("place:${SearchHistoryStore.MAX_ENTRIES}", entries.first().id)
        assertEquals("place:1", entries.last().id)
    }

    @Test
    fun loadsEmptyWhenJsonIsCorrupt() {
        val store = SearchHistoryStore(
            settings = MapSettings(SearchHistoryStore.KEY to "{not-json"),
        )

        assertEquals(emptyList(), store.entries.value)
    }

    @Test
    fun dropsEntriesThatCannotBeRestored() {
        val settings = MapSettings(
            SearchHistoryStore.KEY to """
                [
                  {"id":"ok","kind":"PLACE","name":"Grand Place","latitude":50.85,"longitude":4.35},
                  {"id":"bad","kind":"NOWHERE","name":"Missing","latitude":50.85,"longitude":4.35},
                  {"id":"far","kind":"PLACE","name":"Off World","latitude":999.0,"longitude":4.35}
                ]
            """.trimIndent(),
        )

        val entries = SearchHistoryStore(settings = settings).entries.value

        assertEquals(listOf("ok"), entries.map { it.id })
        assertEquals("Grand Place", entries.single().name)
    }

    @Test
    fun clearRemovesPersistedHistory() {
        val settings = MapSettings()
        val store = SearchHistoryStore(settings = settings)
        store.record(place(id = "stop:central", name = "Bruxelles-Central"))

        store.clear()

        assertEquals(emptyList(), store.entries.value)
        assertNull(settings.getStringOrNull(SearchHistoryStore.KEY))
        assertEquals(emptyList(), SearchHistoryStore(settings = settings).entries.value)
    }

    private fun place(
        id: String,
        name: String = id,
        latitude: Double = 50.85,
        longitude: Double = 4.35,
        kind: LocationKind = LocationKind.PLACE,
    ) = GeocodeResult(
        id = id,
        kind = kind,
        name = name,
        coordinate = Coordinate(latitude = latitude, longitude = longitude),
        country = "Belgium",
    )
}
