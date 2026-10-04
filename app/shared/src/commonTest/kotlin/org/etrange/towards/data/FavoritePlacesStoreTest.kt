package org.etrange.towards.data

import com.russhwolf.settings.MapSettings
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.ui.home.toShortcutItems
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FavoritePlacesStoreTest {

    @Test
    fun loadsEmptyWhenNothingPersisted() {
        val store = FavoritePlacesStore(settings = MapSettings())

        assertEquals(emptyList(), store.places.value)
    }

    @Test
    fun setHomeAndWorkReplaceTheSameSlotAndKeepOrder() {
        val settings = MapSettings()
        val store = FavoritePlacesStore(settings = settings)
        val firstHome = location("Rue Neuve", 50.85, 4.35)
        val secondHome = location("Grand Place", 50.8467, 4.3525, stopId = "stop-1")
        val work = location("Central Station", 50.845, 4.357)

        store.setWork(work)
        store.setHome(firstHome)
        store.setHome(secondHome)

        val reloaded = FavoritePlacesStore(settings = settings)
        assertEquals(listOf(FavoriteRole.HOME, FavoriteRole.WORK), reloaded.places.value.map { it.role })
        val home = reloaded.places.value.first()
        assertEquals(FavoritePlacesStore.HOME_ID, home.id)
        assertEquals(FavoritePlacesStore.HOME_LABEL, home.label)
        assertEquals("Grand Place", home.name)
        assertEquals(secondHome.coordinate, home.coordinate)
        assertEquals("stop-1", home.stopId)
        assertEquals(work.coordinate, reloaded.places.value[1].coordinate)
    }

    @Test
    fun addAndRemoveNamedPlace() {
        val store = FavoritePlacesStore(settings = MapSettings())
        val gym = location("Gym", 50.84, 4.36)
        val campus = location("Campus", 50.81, 4.39)

        assertIs<FavoriteSaveResult.Success>(store.add("School", gym))
        val id = store.places.value.single().id
        assertIs<FavoriteSaveResult.Success>(store.replace(id, campus))

        val saved = store.places.value.single()
        assertEquals("School", saved.label)
        assertEquals("Campus", saved.name)
        assertEquals(campus.coordinate, saved.coordinate)

        store.remove(id)
        assertEquals(emptyList(), store.places.value)
    }

    @Test
    fun rejectsBlankReservedAndDuplicateLabels() {
        val store = FavoritePlacesStore(settings = MapSettings())
        val place = location("Somewhere", 50.8, 4.3)

        assertEquals(
            FavoritePlacesStore.BLANK_LABEL_MESSAGE,
            (store.add("  ", place) as FavoriteSaveResult.Failure).message,
        )
        assertEquals(
            FavoritePlacesStore.RESERVED_LABEL_MESSAGE,
            (store.add("home", place) as FavoriteSaveResult.Failure).message,
        )
        assertIs<FavoriteSaveResult.Success>(store.add("Gym", place))
        assertEquals(
            FavoritePlacesStore.DUPLICATE_LABEL_MESSAGE,
            (store.add("gym", place) as FavoriteSaveResult.Failure).message,
        )
        assertEquals(1, store.places.value.size)
    }

    @Test
    fun rejectsMoreThanTenNamedPlaces() {
        val store = FavoritePlacesStore(settings = MapSettings())
        val place = location("Somewhere", 50.8, 4.3)

        repeat(FavoritePlacesStore.MAX_OTHERS) { index ->
            assertIs<FavoriteSaveResult.Success>(store.add("Place $index", place))
        }

        val extra = store.add("Extra", place)
        assertEquals(
            FavoritePlacesStore.TOO_MANY_MESSAGE,
            (extra as FavoriteSaveResult.Failure).message,
        )
        assertEquals(FavoritePlacesStore.MAX_OTHERS, store.places.value.size)
    }

    @Test
    fun replaceMissingPlaceFails() {
        val store = FavoritePlacesStore(settings = MapSettings())

        val result = store.replace("missing", location("Somewhere", 50.8, 4.3))

        assertEquals(
            FavoritePlacesStore.MISSING_PLACE_MESSAGE,
            (result as FavoriteSaveResult.Failure).message,
        )
        assertEquals(emptyList(), store.places.value)
    }

    @Test
    fun ignoresCorruptPersistedValues() {
        val store = FavoritePlacesStore(
            settings = MapSettings(FavoritePlacesStore.KEY to "{not json"),
        )

        assertEquals(emptyList(), store.places.value)
    }

    @Test
    fun skipsInvalidEntriesAndKeepsTheFirstHome() {
        val raw = """
            [
              {"id":"home","label":"Home","role":"HOME","name":"Bad","latitude":999.0,"longitude":4.0},
              {"id":"other","label":"Nope","role":"NOPE","name":"Nope","latitude":50.0,"longitude":4.0},
              {"id":"home","label":"Maison","role":"HOME","name":"Grand Place","latitude":50.85,"longitude":4.35},
              {"id":"home","label":"Home","role":"HOME","name":"Second","latitude":50.1,"longitude":4.1},
              {"id":"","label":"Blank","role":"OTHER","name":"Blank","latitude":50.2,"longitude":4.2}
            ]
        """.trimIndent()
        val store = FavoritePlacesStore(settings = MapSettings(FavoritePlacesStore.KEY to raw))

        val places = store.places.value
        assertEquals(1, places.size)
        assertEquals("Grand Place", places.single().name)
        assertEquals(FavoritePlacesStore.HOME_LABEL, places.single().label)
        assertNull(places.single().stopId)
    }

    @Test
    fun shortcutMappingKeepsOrderAndCoordinates() {
        val store = FavoritePlacesStore(settings = MapSettings())
        val home = location("Grand Place", 50.8467, 4.3525, stopId = "stop-1")
        val work = location("Work", 50.845, 4.357)
        val school = location("Campus", 50.81, 4.39, stopId = "stop-2")

        store.setWork(work)
        assertIs<FavoriteSaveResult.Success>(store.add("School", school))
        store.setHome(home)

        val shortcuts = store.places.value.toShortcutItems()

        assertEquals(listOf("Home", "Work", "School"), shortcuts.map { it.label })
        assertEquals("Grand Place", shortcuts[0].detail)
        assertEquals(home.coordinate, shortcuts[0].coordinate)
        assertEquals("stop-1", shortcuts[0].stopId)
        assertEquals("", shortcuts[1].detail)
        assertEquals(work.coordinate, shortcuts[1].coordinate)
        assertEquals("Campus", shortcuts[2].detail)
        assertEquals("stop-2", shortcuts[2].stopId)
        assertTrue(shortcuts.all { it.coordinate != null })
    }

    private fun location(
        name: String,
        latitude: Double,
        longitude: Double,
        stopId: String? = null,
    ) = FavoriteLocation(
        name = name,
        coordinate = Coordinate(latitude = latitude, longitude = longitude),
        stopId = stopId,
    )
}
