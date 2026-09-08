package org.etrange.towards.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DateTimesTest {
    @Test
    fun truncatesFractionalSecondsForApiQueries() {
        val instant = Instant.parse("2026-09-08T12:23:45.847123Z")
        assertEquals("2026-09-08T12:23:45Z", instant.toApiDateTime())
    }
}
