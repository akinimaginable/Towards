package org.etrange.towards.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeocodeUniqueIdsTest {
    @Test
    fun synthesizesUniqueIdsForBlankAddressMatches() {
        val results = listOf(
            GeocodeResult(
                id = "",
                kind = LocationKind.ADDRESS,
                name = "Rue de la Loi 16",
                coordinate = Coordinate(50.85, 4.35),
            ),
            GeocodeResult(
                id = "",
                kind = LocationKind.ADDRESS,
                name = "Rue de la Loi 16",
                coordinate = Coordinate(50.85, 4.35),
            ),
            GeocodeResult(
                id = "stop:1",
                kind = LocationKind.STOP,
                name = "Bourse",
                coordinate = Coordinate(50.84, 4.34),
            ),
        ).ensureUniqueIds()

        assertEquals(3, results.map { it.id }.toSet().size)
        assertTrue(results[0].id.isNotBlank())
        assertTrue(results[1].id.isNotBlank())
        assertEquals("stop:1", results[2].id)
    }
}
