package org.etrange.towards

import io.ktor.client.HttpClient
import org.etrange.towards.data.ApiConfig
import org.etrange.towards.data.FavoritePlacesStore
import org.etrange.towards.data.HttpGeocoder
import org.etrange.towards.data.HttpNearbyDepartures
import org.etrange.towards.data.HttpTimetableProvider
import org.etrange.towards.data.HttpTripPlanner
import org.etrange.towards.data.LocationBiasStore
import org.etrange.towards.data.LocationProvider
import org.etrange.towards.data.SearchHistoryStore
import org.etrange.towards.data.SettingsStore
import org.etrange.towards.data.TowardsApi
import org.etrange.towards.data.createHttpClient

/**
 * One data graph for the process. ViewModels outlive composition, so these
 * instances must not be created with `remember`.
 */
class AppGraph(
    val locationProvider: LocationProvider,
    val settingsStore: SettingsStore = SettingsStore(),
    val favoritePlacesStore: FavoritePlacesStore = FavoritePlacesStore(),
    val locationBiasStore: LocationBiasStore = LocationBiasStore(),
    val searchHistoryStore: SearchHistoryStore = SearchHistoryStore(),
    private val httpClient: HttpClient = createHttpClient(),
) : AutoCloseable {
    private val api = TowardsApi(httpClient, ApiConfig(settingsStore))
    val geocoder = HttpGeocoder(api)
    val timetableProvider = HttpTimetableProvider(api)
    val tripPlanner = HttpTripPlanner(api)
    val nearbyDepartures = HttpNearbyDepartures(api)

    override fun close() {
        httpClient.close()
    }
}
