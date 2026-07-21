package org.etrange.towards.infrastructure.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.etrange.towards.application.AuditRecord
import org.etrange.towards.application.AuditRepository
import org.etrange.towards.config.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.DriverManager
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.uuid.Uuid

object AuditLogTable : Table("audit_log") {
    val id = long("id").autoIncrement()
    val userId = uuid("user_id").nullable()
    val correlationId = varchar("correlation_id", 128)
    val actionType = varchar("action_type", 64)
    val outcome = varchar("outcome", 32)
    val requestSummary = text("request_summary").nullable()
    val resultSummary = text("result_summary").nullable()
    val errorDetails = text("error_details").nullable()
    val durationMillis = long("duration_ms")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id, name = "pk_audit_log")
}

object DatabaseFactorySupport {
    fun initialize(config: DatabaseConfig): Database {
        runMigrations(config)
        return Database.connect(
            url = config.url,
            driver = "org.postgresql.Driver",
            user = config.user,
            password = config.password,
        )
    }

    private fun runMigrations(config: DatabaseConfig) {
        DriverManager.getConnection(config.url, config.user, config.password).use { connection ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(connection))
            Liquibase(
                "db/changelog/db.changelog-master.xml",
                ClassLoaderResourceAccessor(),
                database,
            ).use { liquibase ->
                liquibase.update()
            }
        }
    }
}

class ExposedAuditRepository(
    private val database: Database,
) : AuditRepository {
    override suspend fun append(record: AuditRecord) {
        withContext(Dispatchers.IO) {
            transaction(database) {
                AuditLogTable.insert {
                    it[userId] = record.actor?.userId?.value?.let(Uuid::parse)
                    it[correlationId] = record.correlationId
                    it[actionType] = record.action.name
                    it[outcome] = record.outcome.name
                    it[requestSummary] = record.requestSummary
                    it[resultSummary] = record.resultSummary
                    it[errorDetails] = record.errorDetails
                    it[durationMillis] = record.durationMillis
                    it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
                }
            }
        }
    }
}
