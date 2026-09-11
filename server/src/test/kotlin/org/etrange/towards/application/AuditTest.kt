package org.etrange.towards.application

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeRequest

class AuditTest {
    @Test
    fun geocodeSummaryExcludesSearchTextAndCoordinates() {
        val request = GeocodeRequest(
            text = "Private home address",
            bias = Coordinate(latitude = 50.123456, longitude = 4.654321),
            numberOfResults = 10,
        )

        val summary = request.toAuditSummary()

        assertFalse(summary.contains(request.text))
        assertFalse(summary.contains("50.123456"))
        assertFalse(summary.contains("4.654321"))
        assertTrue(summary.contains("queryLength=${request.text.length}"))
        assertTrue(summary.contains("hasBias=true"))
    }

    @Test
    fun queuedWritesDoNotBlockAndCloseFlushesThem() {
        val appendStarted = CompletableDeferred<Unit>()
        val releaseAppend = CompletableDeferred<Unit>()
        val written = Collections.synchronizedList(mutableListOf<AuditRecord>())
        val repository = AuditRepository { record ->
            appendStarted.complete(Unit)
            releaseAppend.await()
            written += record
        }
        val service = AuditService(
            repository = repository,
            retentionDays = 30,
            meterRegistry = SimpleMeterRegistry(),
        )
        val record = AuditRecord(
            actor = null,
            correlationId = "audit-test",
            action = AuditAction.GEOCODE,
            outcome = AuditOutcome.SUCCESS,
            requestSummary = "queryLength=7",
            resultSummary = "results=1",
            errorDetails = null,
            durationMillis = 1,
        )

        service.record(record)
        runBlocking {
            withTimeout(1_000) { appendStarted.await() }
            assertTrue(written.isEmpty())
            releaseAppend.complete(Unit)
        }
        service.close()

        assertEquals(listOf(record), written)
    }
}
