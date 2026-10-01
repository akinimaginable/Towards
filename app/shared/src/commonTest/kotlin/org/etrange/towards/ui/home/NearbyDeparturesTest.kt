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
                    routeId = "route:$stopIndex-$lineIndex",
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
        )

        assertEquals(5, result.size)
        assertEquals(listOf("Stop 1", "Stop 2", "Stop 3", "Stop 4", "Stop 5"), result.map { it.name })
    }

    @Test
    fun keepsFollowingDeparturesOfTheSameLine() {
        val times = listOf("12:03", "12:09", "12:15", "12:21")
        val stopTimes = StopTimes(
            place = nearPlace,
            events = times.map { hhmm ->
                stopTime(
                    place = nearPlace,
                    routeId = "route:1",
                    displayName = "1",
                    headsign = "Stockel",
                    time = "2026-08-06T$hhmm:00Z",
                )
            },
            previousPageCursor = null,
            nextPageCursor = null,
        )

        val departure = groupNearbyDepartures(stopTimes, origin).single().departures.single()

        assertEquals(Instant.parse("2026-08-06T12:03:00Z"), departure.time)
        assertEquals(
            listOf(Instant.parse("2026-08-06T12:09:00Z"), Instant.parse("2026-08-06T12:15:00Z")),
            departure.laterTimes,
        )
    }

    @Test
    fun hidesLinesWithNoDepartureForHours() {
        val idleStop = Place(id = "stop:idle", name = "Idle Stop", coordinate = Coordinate(50.8510, 4.3510))
        val stopTimes = StopTimes(
            place = nearPlace,
            events = listOf(
                stopTime(nearPlace, "route:1", "1", "A", "2026-08-06T12:05:00Z"),
                // Night line: first departure is 5 hours away.
                stopTime(nearPlace, "route:N1", "N1", "B", "2026-08-06T17:00:00Z"),
                // A stop that only has such a line is not shown at all.
                stopTime(idleStop, "route:N2", "N2", "C", "2026-08-06T18:00:00Z"),
            ),
            previousPageCursor = null,
            nextPageCursor = null,
        )

        val result = groupNearbyDepartures(stopTimes, origin, now = baseTime)

        assertEquals(listOf("Near Stop"), result.map { it.name })
        assertEquals(listOf("1"), result.single().departures.map { it.lineName })
    }

    @Test
    fun keepsLineWhoseNextDepartureIsWithinTheWindow() {
        val stopTimes = StopTimes(
            place = nearPlace,
            events = listOf(
                stopTime(nearPlace, "route:1", "1", "A", "2026-08-06T13:45:00Z"),
            ),
            previousPageCursor = null,
            nextPageCursor = null,
        )

        val result = groupNearbyDepartures(stopTimes, origin, now = baseTime)

        assertEquals(1, result.single().departures.size)
    }

    @Test
    fun mergesSameNamedStopsNearEachOther() {
        val metro = Place(id = "stop:metro", name = "De Brouckère", coordinate = Coordinate(50.8503, 4.3503))
        val street = Place(id = "stop:street", name = "De Brouckère", coordinate = Coordinate(50.8506, 4.3506))
        val stopTimes = StopTimes(
            place = metro,
            events = listOf(
                stopTime(metro, "route:1", "1", "Stockel", "2026-08-06T12:05:00Z"),
                stopTime(street, "route:29", "29", "Hof ten Berg", "2026-08-06T12:02:00Z"),
            ),
            previousPageCursor = null,
            nextPageCursor = null,
        )

        val result = groupNearbyDepartures(stopTimes, origin)

        assertEquals(1, result.size)
        assertEquals("stop:metro", result.single().id)
        assertEquals(listOf("29", "1"), result.single().departures.map { it.lineName })
    }

    @Test
    fun keepsSameNamedStopsFarApart() {
        val here = Place(id = "stop:a", name = "Gare", coordinate = Coordinate(50.8503, 4.3503))
        val elsewhere = Place(id = "stop:b", name = "Gare", coordinate = Coordinate(50.8600, 4.3503))
        val stopTimes = StopTimes(
            place = here,
            events = listOf(
                stopTime(here, "route:1", "1", "A", "2026-08-06T12:05:00Z"),
                stopTime(elsewhere, "route:2", "2", "B", "2026-08-06T12:05:00Z"),
            ),
            previousPageCursor = null,
            nextPageCursor = null,
        )

        assertEquals(2, groupNearbyDepartures(stopTimes, origin).size)
    }

    @Test
    fun ordersModeGroupsMetroFirst() {
        val stop = NearbyStop(
            id = "stop",
            name = "Stop",
            coordinate = origin,
            distanceMeters = 0,
            departures = listOf(
                departureOf("29", TransportMode.BUS),
                departureOf("4", TransportMode.TRAM),
                departureOf("1", TransportMode.SUBWAY),
            ),
        )

        assertEquals(
            listOf(ModeGroup.METRO, ModeGroup.TRAM, ModeGroup.BUS),
            stop.departuresByMode().map { it.first },
        )
    }

    @Test
    fun titleCasesAllCapsFeedText() {
        assertEquals("Gare de l'Ouest", "GARE DE L'OUEST".toDisplayCase())
        assertEquals("Hof ten Berg", "HOF TEN BERG".toDisplayCase())
        assertEquals("Herrmann-Debroux", "HERRMANN-DEBROUX".toDisplayCase())
        assertEquals("Hop. Militaire", "HOP. MILITAIRE".toDisplayCase())
        assertEquals("De Brouckère", "DE BROUCKÈRE".toDisplayCase())
        assertEquals("Gare du Midi", "Gare du Midi".toDisplayCase())
        assertEquals("12", "12".toDisplayCase())
    }

    @Test
    fun treatsBlankParentIdAsMissing() {
        val blankParent = nearPlace.copy(id = "stop:a", parentId = "")
        val other = farPlace.copy(id = "stop:b", parentId = "")
        val stopTimes = StopTimes(
            place = nearPlace,
            events = listOf(
                stopTime(
                    place = blankParent,
                    routeId = "route:1",
                    displayName = "1",
                    headsign = "A",
                    time = "2026-08-06T12:05:00Z",
                ),
                stopTime(
                    place = other,
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

        assertEquals(2, result.size)
        assertEquals(listOf("Near Stop", "Far Stop"), result.map { it.name })
    }

    @Test
    fun groupsEventsWithOffsetTimestamps() {
        val stopTimes = StopTimes(
            place = nearPlace,
            events = listOf(
                stopTime(
                    place = nearPlace,
                    routeId = "route:3",
                    displayName = "3",
                    headsign = "Churchill",
                    time = "2026-08-06T14:03:00+02:00",
                ),
            ),
            previousPageCursor = null,
            nextPageCursor = null,
        )

        val result = groupNearbyDepartures(stopTimes, origin)

        assertEquals(1, result.size)
        assertEquals(1, result.single().departures.size)
        assertEquals(Instant.parse("2026-08-06T12:03:00Z"), result.single().departures.single().time)
    }

    @Test
    fun relativeLabelBoundaries() {
        assertEquals("now", relativeLabel(baseTime + 30.seconds, baseTime))
        assertEquals("3 min", relativeLabel(baseTime + 3.minutes, baseTime))
        assertEquals("1 h", relativeLabel(baseTime + 60.minutes, baseTime))
        assertEquals("1 h 12", relativeLabel(baseTime + 72.minutes, baseTime))
    }

    private fun departureOf(line: String, mode: TransportMode) = NearbyDeparture(
        id = line,
        lineName = line,
        headsign = null,
        mode = mode,
        routeColor = null,
        routeTextColor = null,
        time = baseTime,
        realTime = false,
    )

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
