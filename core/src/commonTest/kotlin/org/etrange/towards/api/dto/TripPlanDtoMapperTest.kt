package org.etrange.towards.api.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import org.etrange.towards.domain.model.TransportMode

class TripPlanDtoMapperTest {
    @Test
    fun mapsTripPlanToDomain() {
        val dto = TripPlanDto(
            from = PlaceDto(
                id = "stop:a",
                name = "A",
                coordinate = CoordinateDto(50.85, 4.35),
            ),
            to = PlaceDto(
                name = "B",
                coordinate = CoordinateDto(50.84, 4.36),
            ),
            direct = emptyList(),
            itineraries = listOf(
                ItineraryDto(
                    id = "itin-1",
                    durationSeconds = 600,
                    startTime = "2026-09-07T10:00:00+02:00",
                    endTime = "2026-09-07T10:10:00+02:00",
                    transfers = 0,
                    legs = listOf(
                        JourneyLegDto(
                            mode = "TRAM",
                            from = PlaceDto(
                                id = "stop:a",
                                name = "A",
                                coordinate = CoordinateDto(50.85, 4.35),
                            ),
                            to = PlaceDto(
                                name = "B",
                                coordinate = CoordinateDto(50.84, 4.36),
                            ),
                            startTime = "2026-09-07T10:00:00+02:00",
                            endTime = "2026-09-07T10:10:00+02:00",
                            scheduledStartTime = "2026-09-07T10:00:00+02:00",
                            scheduledEndTime = "2026-09-07T10:10:00+02:00",
                            durationSeconds = 600,
                            realTime = true,
                            displayName = "3",
                            geometry = EncodedPathDto("abc", 5, 2),
                        ),
                    ),
                ),
            ),
        )

        val plan = dto.toDomain()
        assertEquals("A", plan.from.name)
        assertEquals("itin-1", plan.itineraries.single().id)
        assertEquals(TransportMode.TRAM, plan.itineraries.single().legs.single().mode)
        assertEquals("abc", plan.itineraries.single().legs.single().geometry?.points)
    }
}
