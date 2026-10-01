package org.etrange.towards.config

import io.ktor.server.config.ApplicationConfig

data class MotisConfig(
    val baseUrl: String,
    val requestTimeoutMillis: Long,
)

data class DatabasePoolConfig(
    val maximumPoolSize: Int,
    val minimumIdle: Int,
    val connectionTimeoutMillis: Long,
    val idleTimeoutMillis: Long,
    val maxLifetimeMillis: Long,
)

sealed interface DatabaseConfig {
    data object Disabled : DatabaseConfig

    data class Enabled(
        val url: String,
        val user: String,
        val password: String,
        val pool: DatabasePoolConfig,
    ) : DatabaseConfig

    companion object {
        fun from(config: ApplicationConfig): DatabaseConfig {
            val enabled = config.propertyOrNull("enabled")?.getString()?.toBooleanStrict() ?: false
            if (!enabled) {
                return Disabled
            }

            val url = config.property("url").getString().trim()
            val user = config.property("user").getString().trim()
            require(url.isNotEmpty()) { "database url is required when the database is enabled" }
            require(user.isNotEmpty()) { "database user is required when the database is enabled" }

            val pool = config.config("pool")
            return Enabled(
                url = url,
                user = user,
                password = config.propertyOrNull("password")?.getString().orEmpty(),
                pool = DatabasePoolConfig(
                    maximumPoolSize = pool.property("maximumPoolSize").getString().toInt(),
                    minimumIdle = pool.property("minimumIdle").getString().toInt(),
                    connectionTimeoutMillis = pool.property("connectionTimeoutMillis").getString().toLong(),
                    idleTimeoutMillis = pool.property("idleTimeoutMillis").getString().toLong(),
                    maxLifetimeMillis = pool.property("maxLifetimeMillis").getString().toLong(),
                ),
            )
        }
    }
}

data class RateLimitConfig(
    val requests: Int,
    val periodSeconds: Long,
)

data class AuditConfig(
    val retentionDays: Int,
)

data class AppConfig(
    val motis: MotisConfig,
    val database: DatabaseConfig,
    val rateLimit: RateLimitConfig,
    val audit: AuditConfig,
) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig {
            val root = config.config("towards")
            val motis = root.config("motis")
            val rateLimit = root.config("rateLimit")
            val audit = root.config("audit")
            val database = if (root.keys().any { it == "database" || it.startsWith("database.") }) {
                DatabaseConfig.from(root.config("database"))
            } else {
                DatabaseConfig.Disabled
            }

            return AppConfig(
                motis = MotisConfig(
                    baseUrl = motis.property("baseUrl").getString().trimEnd('/'),
                    requestTimeoutMillis = motis.property("requestTimeoutMillis").getString().toLong(),
                ),
                database = database,
                rateLimit = RateLimitConfig(
                    requests = rateLimit.property("requests").getString().toInt(),
                    periodSeconds = rateLimit.property("periodSeconds").getString().toLong(),
                ),
                audit = AuditConfig(
                    retentionDays = audit.property("retentionDays").getString().toInt()
                        .also { require(it > 0) { "audit retentionDays must be positive" } },
                ),
            )
        }
    }
}
