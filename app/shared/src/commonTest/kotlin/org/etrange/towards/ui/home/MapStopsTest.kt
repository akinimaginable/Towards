package org.etrange.towards.ui.home

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.map.MapBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapStopsTest {
    private val brussels = MapBounds(
        minimum = Coordinate(latitude = 50.84, longitude = 4.34),
        maximum = Coordinate(latitude = 50.86, longitude = 4.38),
    )

    @Test
    fun expandsBoundsOnEverySide() {
        val expanded = brussels.expandedBy(0.5)

        assertEquals(50.83, expanded.minimum.latitude, 1e-9)
        assertEquals(4.32, expanded.minimum.longitude, 1e-9)
        assertEquals(50.87, expanded.maximum.latitude, 1e-9)
        assertEquals(4.40, expanded.maximum.longitude, 1e-9)
    }

    @Test
    fun clampsExpandedBoundsToTheWorld() {
        val world = MapBounds(
            minimum = Coordinate(latitude = -80.0, longitude = -170.0),
            maximum = Coordinate(latitude = 80.0, longitude = 170.0),
        ).expandedBy(1.0)

        assertEquals(-90.0, world.minimum.latitude)
        assertEquals(-180.0, world.minimum.longitude)
        assertEquals(90.0, world.maximum.latitude)
        assertEquals(180.0, world.maximum.longitude)
    }

    @Test
    fun containsOnlyBoundsFullyInside() {
        val loaded = brussels.expandedBy(0.5)
        val panned = MapBounds(
            minimum = Coordinate(latitude = 50.845, longitude = 4.35),
            maximum = Coordinate(latitude = 50.865, longitude = 4.39),
        )
        val pannedFar = MapBounds(
            minimum = Coordinate(latitude = 50.86, longitude = 4.39),
            maximum = Coordinate(latitude = 50.88, longitude = 4.43),
        )

        assertTrue(brussels in loaded)
        assertTrue(panned in loaded)
        assertFalse(pannedFar in loaded)
    }
}
