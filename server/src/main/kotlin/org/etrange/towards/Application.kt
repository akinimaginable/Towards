package org.etrange.towards

import io.ktor.client.HttpClient
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.etrange.towards.application.AuditService
import org.etrange.towards.application.MobilityService
import org.etrange.towards.config.AppConfig
import org.etrange.towards.di.applicationModule
import org.etrange.towards.plugins.configureHttpPlugins
import org.etrange.towards.routes.configureRoutes
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val config = AppConfig.from(environment.config)

    install(Koin) {
        slf4jLogger()
        modules(applicationModule(config))
    }

    val prometheusRegistry = getKoin().get<io.micrometer.prometheusmetrics.PrometheusMeterRegistry>()
    val mobilityService = getKoin().get<MobilityService>()
    val auditService = getKoin().get<AuditService>()

    configureHttpPlugins(config, prometheusRegistry, auditService)
    configureRoutes(mobilityService, prometheusRegistry)

    monitor.subscribe(ApplicationStopped) {
        getKoin().get<HttpClient>().close()
    }
}