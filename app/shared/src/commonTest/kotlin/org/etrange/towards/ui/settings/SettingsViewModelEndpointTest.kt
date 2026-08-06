package org.etrange.towards.ui.settings

import com.russhwolf.settings.MapSettings
import org.etrange.towards.data.ApiEndpointStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelEndpointTest {

    @Test
    fun saveUpdatesDraftAndClearsErrorOnValidEndpoint() {
        val viewModel = SettingsViewModel(
            endpointStore = ApiEndpointStore(
                settings = MapSettings(),
                defaultUrl = "http://127.0.0.1:8081",
            ),
        )

        viewModel.onApiEndpointDraftChange(" https://api.example.com/ ")
        val saved = viewModel.onApiEndpointSave()

        assertTrue(saved)
        assertEquals("https://api.example.com", viewModel.apiEndpointDraft.value)
        assertEquals("https://api.example.com", viewModel.apiEndpoint.value)
        assertNull(viewModel.apiEndpointError.value)
    }

    @Test
    fun saveKeepsDraftAndSetsErrorOnInvalidEndpoint() {
        val viewModel = SettingsViewModel(
            endpointStore = ApiEndpointStore(
                settings = MapSettings(),
                defaultUrl = "http://127.0.0.1:8081",
            ),
        )

        viewModel.onApiEndpointDraftChange("not-a-url")
        val saved = viewModel.onApiEndpointSave()

        assertFalse(saved)
        assertEquals("not-a-url", viewModel.apiEndpointDraft.value)
        assertEquals("http://127.0.0.1:8081", viewModel.apiEndpoint.value)
        assertEquals(ApiEndpointStore.INVALID_ENDPOINT_MESSAGE, viewModel.apiEndpointError.value)
    }

    @Test
    fun draftChangeClearsPreviousError() {
        val viewModel = SettingsViewModel(
            endpointStore = ApiEndpointStore(
                settings = MapSettings(),
                defaultUrl = "http://127.0.0.1:8081",
            ),
        )

        viewModel.onApiEndpointDraftChange("bad")
        viewModel.onApiEndpointSave()
        viewModel.onApiEndpointDraftChange("https://api.example.com")

        assertNull(viewModel.apiEndpointError.value)
    }
}
