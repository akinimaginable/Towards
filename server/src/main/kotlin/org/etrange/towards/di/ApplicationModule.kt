package org.etrange.towards.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.serialization.json.Json
import org.etrange.towards.application.AuditRepository
import org.etrange.towards.application.AuditService
import org.etrange.towards.application.MobilityService
import org.etrange.towards.application.NoOpAuditRepository
import org.etrange.towards.application.PassThroughTripPlanCache
import org.etrange.towards.application.TripPlanCache
import org.etrange.towards.config.AppConfig
import org.etrange.towards.domain.port.Geocoder
import org.etrange.towards.domain.port.TransitDataProvider
import org.etrange.towards.domain.port.TripInformationProvider
import org.etrange.towards.domain.port.TripPlanner
import org.etrange.towards.infrastructure.database.DatabaseFactorySupport
import org.etrange.towards.infrastructure.database.ExposedAuditRepository
import org.etrange.towards.infrastructure.motis.MotisClient
import org.koin.dsl.module

fun applicationModule(config: AppConfig) = module {
    single { config }
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
    }
    single { PrometheusMeterRegistry(PrometheusConfig.DEFAULT) }
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(get())
            }
            install(HttpTimeout) {
                requestTimeoutMillis = config.motis.requestTimeoutMillis
                connectTimeoutMillis = config.motis.requestTimeoutMillis
                socketTimeoutMillis = config.motis.requestTimeoutMillis
            }
        }
    }
    single {
        MotisClient(
            client = get(),
            config = config.motis,
            json = get(),
        )
    }
    single<TripPlanner> { get<MotisClient>() }
    single<TripInformationProvider> { get<MotisClient>() }
    single<Geocoder> { get<MotisClient>() }
    single<TransitDataProvider> { get<MotisClient>() }

    single<AuditRepository> {
        if (config.database.enabled) {
            ExposedAuditRepository(DatabaseFactorySupport.initialize(config.database))
        } else {
            NoOpAuditRepository()
        }
    }
    single { AuditService(get()) }
    single<TripPlanCache> { PassThroughTripPlanCache() }
    single {
        MobilityService(
            tripPlanner = get(),
            tripInformationProvider = get(),
            geocoder = get(),
            transitDataProvider = get(),
            auditService = get(),
            tripPlanCache = get(),
        )
    }
}
