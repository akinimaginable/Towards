package org.etrange.towards.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.etrange.towards.domain.model.Coordinate

class LocationBiasStoreTest {

    @Test
    fun loadsNullWhenNothingPersisted() {
        val store = LocationBiasStore(settings = MapSettings())

        assertNull(store.load())
    }

    @Test
    fun persistsAndLoadsCoordinate() {
        val settings = MapSettings()
        val store = LocationBiasStore(settings = settings)
        val coordinate = Coordinate(latitude = 50.85, longitude = 4.35)

        store.save(coordinate)

        assertEquals(coordinate, store.load())
        assertEquals(50.85, settings.getDouble(LocationBiasStore.KEY_LATITUDE, Double.NaN))
        assertEquals(4.35, settings.getDouble(LocationBiasStore.KEY_LONGITUDE, Double.NaN))
    }

    @Test
    fun ignoresCorruptPersistedValues() {
        val store = LocationBiasStore(
            settings = MapSettings(
                LocationBiasStore.KEY_LATITUDE to 999.0,
                LocationBiasStore.KEY_LONGITUDE to 4.35,
            ),
        )

        assertNull(store.load())
    }
}
