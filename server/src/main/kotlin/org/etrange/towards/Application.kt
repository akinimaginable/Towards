package org.etrange.towards

import io.ktor.client.HttpClient
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.etrange.towards.application.AuditService
import org.etrange.towards.application.MobilityService
import org.etrange.towards.config.AppConfig
import org.etrange.towards.config.DatabaseConfig
import org.etrange.towards.di.applicationModule
import org.etrange.towards.infrastructure.database.DatabaseResources
import org.etrange.towards.plugins.configureHttpPlugins
import org.etrange.towards.routes.configureRoutes
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val config = AppConfig.from(environment.config)
    when (val database = config.database) {
        is DatabaseConfig.Enabled -> log.info("Database enabled url={}", database.url)
        DatabaseConfig.Disabled -> log.info("Database disabled")
    }

    install(Koin) {
        slf4jLogger()
        modules(applicationModule(config))
    }

    val prometheusRegistry = getKoin().get<io.micrometer.prometheusmetrics.PrometheusMeterRegistry>()

    configureHttpPlugins(
        config = config,
        prometheusRegistry = prometheusRegistry,
        auditService = { getKoin().get<AuditService>() },
    )
    val mobilityService = getKoin().get<MobilityService>()
    configureRoutes(mobilityService, prometheusRegistry)

    monitor.subscribe(ApplicationStopped) {
        getKoin().get<HttpClient>().close()
        getKoin().get<AuditService>().close()
        getKoin().getOrNull<DatabaseResources>()?.close()
    }
}