package org.etrange.towards.ui.trip

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.etrange.towards.data.toApiDateTime
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.domain.model.TripPlan

class TripPresentationTest {
    private val origin = Place(null, "Start", Coordinate(50.85, 4.35))
    private val stop = Place("stop:1", "Bourse", Coordinate(50.848, 4.349))
    private val dest = Place("place:gp", "Grand Place", Coordinate(50.8467, 4.3525))

    @Test
    fun formatsDurationAndClock() {
        assertEquals("12 min", formatDuration(12 * 60))
        assertEquals("1 h", formatDuration(60 * 60))
        assertEquals("1 h 12 min", formatDuration(72 * 60))
        assertEquals("10:04", formatClock("2026-09-07T10:04:00+02:00"))
    }

    @Test
    fun combinesTransitThenWalkAndDedupesIds() {
        val transit = itinerary("opt-1", walk = false)
        val walk = itinerary("opt-walk", walk = true)
        val duplicate = itinerary("opt-1", walk = false)
        val plan = TripPlan(
            from = origin,
            to = dest,
            direct = listOf(walk),
            itineraries = listOf(transit, duplicate),
            previousPageCursor = null,
            nextPageCursor = null,
        )

        val options = combineTripOptions(plan)
        assertEquals(listOf("opt-1", "opt-walk"), options.map { it.id })
    }

    @Test
    fun walkOnlySummaryAndStreetDetection() {
        val walk = itinerary("walk", walk = true)
        assertTrue(walk.isWalkOnly())
        assertEquals("Walk", walk.summaryLabel())
        assertEquals("Walk 12 min to Grand Place", walk.legs.single().detailTitle())
    }

    @Test
    fun transitSummaryIncludesWalkAndTransfers() {
        val transit = itinerary("tram", walk = false)
        assertEquals("Walk 4 min · Direct", transit.summaryLabel())
        assertEquals("3 Churchill", transit.legs.last().detailTitle())
        assertEquals("Live · on time", transit.legs.last().realtimeStatusLabel())
        assertEquals(
            "Live · 5 min late",
            transit.legs.last().copy(
                startTime = "2026-09-07T10:13:00+02:00",
            ).realtimeStatusLabel(),
        )
        assertEquals(
            "Cancelled",
            transit.legs.last().copy(cancelled = true).realtimeStatusLabel(),
        )
    }

    @Test
    fun pickingOriginKeepsDestination() {
        val origin = TripEndpoint("My location", Coordinate(50.85, 4.35))
        val destination = TripEndpoint("Grand Place", Coordinate(50.8467, 4.3525))
        val picked = TripEndpoint("Bourse", Coordinate(50.848, 4.349), stopId = "stop:bourse")

        val (from, to) = tripEndpointsAfterPicking(
            editingOrigin = true,
            picked = picked,
            counterpart = destination,
        )
        assertEquals("Bourse", from.name)
        assertEquals("Grand Place", to.name)

        val (fromDestination, toDestination) = tripEndpointsAfterPicking(
            editingOrigin = false,
            picked = picked,
            counterpart = origin,
        )
        assertEquals("My location", fromDestination.name)
        assertEquals("Bourse", toDestination.name)
    }

    @Test
    fun formatsLeaveNowTodayAndAnotherDay() {
        val zone = TimeZone.of("Europe/Brussels")
        val now = Instant.parse("2026-11-09T08:00:00Z")
        val today = Instant.parse("2026-11-09T17:30:00Z")
        val tuesday = Instant.parse("2026-11-10T17:30:00Z")
        val nextYear = Instant.parse("2027-11-10T17:30:00Z")

        assertEquals("Leave now", TripTime.LeaveNow.headerLabel(now, zone))
        assertEquals("Depart at 18:30", TripTime.DepartAt(today).headerLabel(now, zone))
        assertEquals("Arrive by Tue 10 Nov, 18:30", TripTime.ArriveBy(tuesday).headerLabel(now, zone))
        assertEquals("Tue 10 Nov", formatTripDate(tuesday, zone, now))
        assertEquals("Wed 10 Nov 2027", formatTripDate(nextYear, zone, now))
        assertEquals("Arrive by Wed 10 Nov 2027, 18:30", TripTime.ArriveBy(nextYear).headerLabel(now, zone))
    }

    @Test
    fun calendarDayKeepsTheLocalDate() {
        val zone = TimeZone.of("Europe/Brussels")
        val utcDay = LocalDateTime(LocalDate(2026, 11, 10), LocalTime(0, 0))
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds()

        val instant = instantFromCalendarDayAndLocalTime(utcDay, hour = 18, minute = 30, timeZone = zone)
        val local = instant.toLocalDateTime(zone)

        assertEquals(LocalDate(2026, 11, 10), local.date)
        assertEquals(LocalTime(18, 30), local.time)
    }

    @Test
    fun requestTimeUsesNowOrTheChosenInstant() {
        val now = Instant.parse("2026-11-10T08:00:00Z")
        val chosen = Instant.parse("2026-11-10T17:30:00Z")

        assertEquals(now.toApiDateTime() to false, TripTime.LeaveNow.toRequestTime(now))
        assertEquals(chosen.toApiDateTime() to false, TripTime.DepartAt(chosen).toRequestTime(now))
        assertEquals(chosen.toApiDateTime() to true, TripTime.ArriveBy(chosen).toRequestTime(now))
    }

    @Test
    fun pickedPlaceKeepsTimeAfterASwap() {
        val origin = TripEndpoint("My location", Coordinate(50.85, 4.35))
        val destination = TripEndpoint("Grand Place", Coordinate(50.8467, 4.3525))
        val departAt = TripTime.DepartAt(Instant.parse("2026-11-10T17:30:00Z"))
        val arriveBy = TripTime.ArriveBy(Instant.parse("2026-11-10T18:00:00Z"))
        val picked = TripEndpoint("Bourse", Coordinate(50.848, 4.349), stopId = "stop:bourse")

        val leaveNow = origin.toResultsRoute(destination)
        assertNull(leaveNow.timeEpochSeconds)
        assertEquals(false, leaveNow.arriveBy)
        assertEquals(TripTime.LeaveNow, leaveNow.tripTime())
        assertEquals(departAt, origin.toResultsRoute(destination, departAt).tripTime())
        val beforeEpoch = TripTime.DepartAt(Instant.fromEpochSeconds(-1))
        assertEquals(beforeEpoch, origin.toResultsRoute(destination, beforeEpoch).tripTime())

        val swapped = resultsRouteAfterPicking(
            editingOrigin = true,
            picked = picked,
            counterpart = origin,
            time = arriveBy,
        )
        assertEquals("Bourse", swapped.origin().name)
        assertEquals("My location", swapped.destination().name)
        assertEquals(arriveBy, swapped.tripTime())
        assertEquals(
            arriveBy,
            destination.toPickerRoute(
                editingOrigin = true,
                counterpart = origin,
                time = arriveBy,
            ).tripTime(),
        )
    }

    @Test
    fun nextFiveMinutesRoundsUpAndKeepsAnExactMark() {
        val zone = TimeZone.of("Europe/Brussels")
        assertEquals(
            Instant.parse("2026-11-10T17:35:00Z"),
            nextFiveMinuteMark(Instant.parse("2026-11-10T17:32:10Z"), zone),
        )
        assertEquals(
            Instant.parse("2026-11-10T17:35:00Z"),
            nextFiveMinuteMark(Instant.parse("2026-11-10T17:35:00Z"), zone),
        )
        assertEquals(
            Instant.parse("2026-11-10T23:00:00Z"),
            nextFiveMinuteMark(Instant.parse("2026-11-10T22:58:01Z"), zone),
        )
        assertEquals(
            Instant.parse("2026-10-25T01:00:00Z"),
            nextFiveMinuteMark(Instant.parse("2026-10-25T00:58:00Z"), zone),
        )
        assertEquals(
            Instant.parse("2026-03-29T01:00:00Z"),
            nextFiveMinuteMark(Instant.parse("2026-03-29T00:58:00Z"), zone),
        )
    }

    private fun itinerary(id: String, walk: Boolean): Itinerary {
        val legs = if (walk) {
            listOf(
                JourneyLeg(
                    mode = TransportMode.WALK,
                    from = origin,
                    to = dest,
                    startTime = "2026-09-07T10:06:00+02:00",
                    endTime = "2026-09-07T10:18:00+02:00",
                    scheduledStartTime = "2026-09-07T10:06:00+02:00",
                    scheduledEndTime = "2026-09-07T10:18:00+02:00",
                    durationSeconds = 12 * 60,
                    realTime = false,
                ),
            )
        } else {
            listOf(
                JourneyLeg(
                    mode = TransportMode.WALK,
                    from = origin,
                    to = stop,
                    startTime = "2026-09-07T10:04:00+02:00",
                    endTime = "2026-09-07T10:08:00+02:00",
                    scheduledStartTime = "2026-09-07T10:04:00+02:00",
                    scheduledEndTime = "2026-09-07T10:08:00+02:00",
                    durationSeconds = 240,
                    realTime = false,
                ),
                JourneyLeg(
                    mode = TransportMode.TRAM,
                    from = stop,
                    to = dest,
                    startTime = "2026-09-07T10:08:00+02:00",
                    endTime = "2026-09-07T10:16:00+02:00",
                    scheduledStartTime = "2026-09-07T10:08:00+02:00",
                    scheduledEndTime = "2026-09-07T10:16:00+02:00",
                    durationSeconds = 480,
                    realTime = true,
                    displayName = "3",
                    headsign = "Churchill",
                    routeColor = "FFDD00",
                    routeTextColor = "000000",
                ),
            )
        }
        return Itinerary(
            id = id,
            durationSeconds = legs.sumOf { it.durationSeconds },
            startTime = legs.first().startTime,
            endTime = legs.last().endTime,
            transfers = if (walk) 0 else 0,
            legs = legs,
        )
    }
}
