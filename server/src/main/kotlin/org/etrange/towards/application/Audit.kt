package org.etrange.towards.application

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.etrange.towards.domain.model.ActorContext
import org.slf4j.LoggerFactory

enum class AuditAction {
    TRIP_SEARCH,
    TRIP_LOOKUP,
    ITINERARY_REFRESH,
    STOP_TIMES,
    GEOCODE,
    REVERSE_GEOCODE,
    MAP_INITIAL,
    MAP_STOPS,
    MAP_TRIPS,
    MAP_LEVELS,
    SYSTEM_ERROR,
}

enum class AuditOutcome {
    SUCCESS,
    FAILURE,
}

data class AuditRecord(
    val actor: ActorContext?,
    val correlationId: String,
    val action: AuditAction,
    val outcome: AuditOutcome,
    val requestSummary: String?,
    val resultSummary: String?,
    val errorDetails: String?,
    val durationMillis: Long,
)

fun interface AuditRepository {
    suspend fun append(record: AuditRecord)

    suspend fun deleteBefore(cutoff: OffsetDateTime): Int = 0
}

class AuditService(
    private val repository: AuditRepository,
    private val retentionDays: Int,
    meterRegistry: MeterRegistry,
    queueCapacity: Int = DEFAULT_QUEUE_CAPACITY,
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        require(retentionDays > 0) { "retentionDays must be positive" }
        require(queueCapacity > 0) { "queueCapacity must be positive" }
    }

    private val queueDepth = AtomicInteger()
    private val droppedCounter = Counter.builder("audit_queue_dropped")
        .description("Audit records dropped because the bounded queue was full")
        .register(meterRegistry)
    private val writeFailureCounter = Counter.builder("audit_write_failures")
        .description("Audit records that failed to persist")
        .register(meterRegistry)
    private val retentionFailureCounter = Counter.builder("audit_retention_failures")
        .description("Audit retention cleanup attempts that failed")
        .register(meterRegistry)
    private val writeTimer = Timer.builder("audit_write")
        .description("Audit repository write latency")
        .register(meterRegistry)
    @Suppress("unused")
    private val queueDepthGauge = Gauge.builder("audit_queue_depth", queueDepth) { depth ->
        depth.get().toDouble()
    }
        .description("Audit records currently waiting to be written")
        .register(meterRegistry)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<AuditRecord>(capacity = queueCapacity)
    private val worker = scope.launch {
        pruneExpired()
        var nextPruneNanos = System.nanoTime() + PRUNE_INTERVAL_NANOS
        for (record in queue) {
            queueDepth.decrementAndGet()
            append(record)
            if (System.nanoTime() >= nextPruneNanos) {
                pruneExpired()
                nextPruneNanos = System.nanoTime() + PRUNE_INTERVAL_NANOS
            }
        }
    }

    fun record(record: AuditRecord) {
        queueDepth.incrementAndGet()
        if (queue.trySend(record).isFailure) {
            queueDepth.decrementAndGet()
            droppedCounter.increment()
            logger.warn(
                "audit_queue_full correlationId={} action={}",
                record.correlationId,
                record.action,
            )
        }
    }

    private suspend fun append(record: AuditRecord) {
        val startedAt = System.nanoTime()
        try {
            repository.append(record)
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Exception) {
            writeFailureCounter.increment()
            logger.error(
                "audit_write_failed correlationId={} action={}",
                record.correlationId,
                record.action,
                cause,
            )
        } finally {
            writeTimer.record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS)
        }
    }

    private suspend fun pruneExpired() {
        val cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(retentionDays.toLong())
        try {
            val deleted = repository.deleteBefore(cutoff)
            if (deleted > 0) {
                logger.info("audit_retention_deleted rows={}", deleted)
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Exception) {
            retentionFailureCounter.increment()
            logger.error("audit_retention_failed", cause)
        }
    }

    override fun close() {
        queue.close()
        runBlocking {
            withTimeoutOrNull(CLOSE_TIMEOUT_MILLIS) {
                worker.join()
            }
        }
        scope.cancel()
    }

    companion object {
        private const val DEFAULT_QUEUE_CAPACITY = 1_024
        private const val PRUNE_INTERVAL_NANOS = 24L * 60 * 60 * 1_000_000_000
        private const val CLOSE_TIMEOUT_MILLIS = 2_000L
    }
}

class NoOpAuditRepository : AuditRepository {
    override suspend fun append(record: AuditRecord) = Unit

    override suspend fun deleteBefore(cutoff: OffsetDateTime): Int = 0
}
