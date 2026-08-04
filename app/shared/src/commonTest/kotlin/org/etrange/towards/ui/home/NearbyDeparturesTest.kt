package org.etrange.towards.ui.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.StopTime
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.TransportMode

class NearbyDeparturesTest {

    private val origin = Coordinate(50.8500, 4.3500)
    private val nearPlace = Place(
        id = "stop:near",
        name = "Near Stop",
        coordinate = Coordinate(50.8505, 4.3505),
    )
    private val farPlace = Place(
        id = "stop:far",
        name = "Far Stop",
        coordinate = Coordinate(50.8550, 4.3600),
        parentId = "parent:far",
    )
    private val baseTime = Instant.parse("2026-08-06T12:00:00Z")

    @Test
    fun keepsEarliestDeparturePerLineAndDirection() {
        val stopTimes = StopTimes(
            place = nearPlace,
            events = listOf(
                stopTime(
                    place = nearPlace,
                    routeId = "route:3",
                    displayName = "3",
                    headsign = "Churchill",
                    time = "2026-08-06T12:10:00Z",
                    tripId = "trip:late",
                ),
                stopTime(
                    place = nearPlace,
                    routeId = "route:3",
                    displayName = "3",
                    headsign = "Churchill",
                    time = "2026-08-06T12:03:00Z",
                    tripId = "trip:early",
                ),
                stopTime(
                    place = nearPlace,
                    routeId = "route:3",
                    displayName = "3",
                    headsign = "Erasme",
                    time = "2026-08-06T12:05:00Z",
                    tripId = "trip:other-dir",
                ),
            ),
            previousPageCursor = null,
            nextPageCursor = null,
        )

        val result = groupNearbyDepartures(stopTimes, origin)

        assertEquals(1, result.size)
        assertEquals(2, result.single().departures.size)
        assertEquals("Churchill", result.single().departures[0].headsign)
        assertEquals(Instant.parse("2026-08-06T12:03:00Z"), result.single().departures[0].time)
        assertEquals("Erasme", result.single().departures[1].headsign)
    }

    @Test
    fun dropsCancelledEvents() {
        val stopTimes = StopTimes(
            place = nearPlace,
            events = listOf(
                stopTime(
                    place = nearPlace,
                    routeId = "route:4",
                    displayName = "4",
                    headsign = "Stalle",
                    time = "2026-08-06T12:04:00Z",
                    cancelled = true,
                ),
                stopTime(
                    place = nearPlace,
                    routeId = "route:4",
                    displayName = "4",
                    headsign = "Stalle",
                    time = "2026-08-06T12:08:00Z",
                ),
            ),
            previousPageCursor = null,
            nextPageCursor = null,
        )

        val result = groupNearbyDepartures(stopTimes, origin)

        assertEquals(1, result.single().departures.size)
        assertEquals(Instant.parse("2026-08-06T12:08:00Z"), result.single().departures.single().time)
    }

    @Test
    fun ordersStopsByDistance() {
        val stopTimes = StopTimes(
            place = nearPlace,
            events = listOf(
                stopTime(
                    place = farPlace,
                    routeId = "route:1",
                    displayName = "1",
                    headsign = "A",
                    time = "2026-08-06T12:05:00Z",
                ),
                stopTime(
                    place = nearPlace,
                    routeId = "route:2",
                    displayName = "2",
                    headsign = "B",
                    time = "2026-08-06T12:05:00Z",
                ),
            ),
            previousPageCursor = null,
            nextPageCursor = null,
        )

        val result = groupNearbyDepartures(stopTimes, origin)

        assertEquals(listOf("Near Stop", "Far Stop"), result.map { it.name })
        assertTrue(result[0].distanceMeters < result[1].distanceMeters)
    }

    @Test
    fun respectsCaps() {
        val events = (1..6).flatMap { stopIndex ->
            val place = Place(
                id = "stop:$stopIndex",
                name = "Stop $stopIndex",
                coordinate = Coordinate(
                    latitude = origin.latitude + stopIndex * 0.001,
                    longitude = origin.longitude,
                ),
            )
            (1..6).map { lineIndex ->
                stopTime(
                    place = place,
                    routeId = "route:$lineIndex",
                    displayName = "$lineIndex",
                    headsign = "Dir $lineIndex",
                    time = "2026-08-06T12:${(lineIndex + 10).toString().padStart(2, '0')}:00Z",
                )
            }
        }

        val result = groupNearbyDepartures(
            StopTimes(nearPlace, events, null, null),
            origin,
            maxStops = 5,
            maxDeparturesPerStop = 4,
        )

        assertEquals(5, result.size)
        assertTrue(result.all { it.departures.size <= 4 })
    }

    @Test
    fun relativeLabelBoundaries() {
        assertEquals("now", relativeLabel(baseTime + 30.seconds, baseTime))
        assertEquals("3 min", relativeLabel(baseTime + 3.minutes, baseTime))
        assertEquals("1 h", relativeLabel(baseTime + 60.minutes, baseTime))
        assertEquals("1 h 12", relativeLabel(baseTime + 72.minutes, baseTime))
    }

    private fun stopTime(
        place: Place,
        routeId: String,
        displayName: String,
        headsign: String,
        time: String,
        tripId: String = "trip:$routeId:$time",
        cancelled: Boolean = false,
    ) = StopTime(
        place = place,
        mode = TransportMode.BUS,
        time = time,
        scheduledTime = time,
        realTime = true,
        headsign = headsign,
        tripId = tripId,
        routeId = routeId,
        displayName = displayName,
        routeColor = "FF0000",
        routeTextColor = "FFFFFF",
        cancelled = cancelled,
    )
}
