package org.etrange.towards.config

import io.ktor.server.config.ApplicationConfig

data class MotisConfig(
    val baseUrl: String,
    val requestTimeoutMillis: Long,
)

data class DatabaseConfig(
    val enabled: Boolean,
    val url: String,
    val user: String,
    val password: String,
)

data class RateLimitConfig(
    val requests: Int,
    val periodSeconds: Long,
)

data class AuthenticationConfig(
    val dummyUserId: String,
)

data class AppConfig(
    val motis: MotisConfig,
    val database: DatabaseConfig,
    val rateLimit: RateLimitConfig,
    val authentication: AuthenticationConfig,
) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig {
            val root = config.config("towards")
            val motis = root.config("motis")
            val database = root.config("database")
            val rateLimit = root.config("rateLimit")
            val authentication = root.config("authentication")

            return AppConfig(
                motis = MotisConfig(
                    baseUrl = motis.property("baseUrl").getString().trimEnd('/'),
                    requestTimeoutMillis = motis.property("requestTimeoutMillis").getString().toLong(),
                ),
                database = DatabaseConfig(
                    enabled = database.property("enabled").getString().toBooleanStrict(),
                    url = database.property("url").getString(),
                    user = database.property("user").getString(),
                    password = database.property("password").getString(),
                ),
                rateLimit = RateLimitConfig(
                    requests = rateLimit.property("requests").getString().toInt(),
                    periodSeconds = rateLimit.property("periodSeconds").getString().toLong(),
                ),
                authentication = AuthenticationConfig(
                    dummyUserId = authentication.property("dummyUserId").getString(),
                ),
            )
        }
    }
}
