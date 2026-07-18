package org.etrange.towards.application

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
}

class AuditService(
    private val repository: AuditRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun record(record: AuditRecord) {
        runCatching { repository.append(record) }
            .onFailure { cause ->
                logger.error(
                    "audit_write_failed correlationId={} action={}",
                    record.correlationId,
                    record.action,
                    cause,
                )
            }
    }
}

class NoOpAuditRepository : AuditRepository {
    override suspend fun append(record: AuditRecord) = Unit
}
