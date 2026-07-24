package org.etrange.towards.infrastructure.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
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

class DatabaseResources(
    val dataSource: HikariDataSource,
    val database: Database,
) : AutoCloseable {
    override fun close() {
        dataSource.close()
    }
}

object DatabaseFactorySupport {
    fun initialize(config: DatabaseConfig): DatabaseResources {
        val dataSource = createDataSource(config)
        runMigrations(dataSource)
        return DatabaseResources(
            dataSource = dataSource,
            database = Database.connect(dataSource),
        )
    }

    private fun createDataSource(config: DatabaseConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            driverClassName = "org.postgresql.Driver"
            poolName = "towards-postgres"
            maximumPoolSize = config.pool.maximumPoolSize
            minimumIdle = config.pool.minimumIdle
            connectionTimeout = config.pool.connectionTimeoutMillis
            idleTimeout = config.pool.idleTimeoutMillis
            maxLifetime = config.pool.maxLifetimeMillis
            isAutoCommit = false
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        return HikariDataSource(hikariConfig)
    }

    private fun runMigrations(dataSource: HikariDataSource) {
        dataSource.connection.use { connection ->
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
