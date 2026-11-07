package org.etrange.towards.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.runBlocking
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.StopTimes

class NearbyStopTimesCacheTest {
    private val sample = StopTimes(
        place = Place(id = null, name = "Stop", coordinate = Coordinate(50.85, 4.35)),
        events = emptyList(),
        previousPageCursor = null,
        nextPageCursor = null,
    )

    @Test
    fun theSameCellAndMinuteLoadsOnce() = runBlocking {
        val cache = NearbyStopTimesCache()
        val cell = Coordinate(50.8503, 4.3517).nearbyCell()
        var loads = 0

        val first = cache.getOrLoad(cell, minute = 10) { loads += 1; sample }
        val second = cache.getOrLoad(cell, minute = 10) { loads += 1; sample }

        assertEquals(1, loads)
        assertSame(first, second)
    }

    @Test
    fun aNewMinuteLoadsAgain() = runBlocking {
        val cache = NearbyStopTimesCache()
        val cell = Coordinate(50.8503, 4.3517).nearbyCell()
        var loads = 0
        val load: suspend () -> StopTimes = { loads += 1; sample }

        cache.getOrLoad(cell, minute = 10, load)
        cache.getOrLoad(cell, minute = 11, load)

        assertEquals(2, loads)
    }

    @Test
    fun nearbyPointsShareACellAndItsCenterStaysInsideIt() {
        val here = Coordinate(50.8503, 4.3517)
        val tenMetersNorth = Coordinate(50.85039, 4.3517)
        val cell = here.nearbyCell()

        assertEquals(cell, tenMetersNorth.nearbyCell())
        assertEquals(cell, cell.center().nearbyCell())
    }
}
