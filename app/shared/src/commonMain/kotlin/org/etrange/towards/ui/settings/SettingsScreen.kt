package org.etrange.towards.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.etrange.towards.data.FavoritePlace
import org.etrange.towards.data.FavoritePlacesStore
import org.etrange.towards.data.FavoriteRole
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.rememberAppVersion
import org.etrange.towards.ui.icons.arrow_backIcon
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onPickHome: () -> Unit,
    onPickWork: () -> Unit,
    onAddPlace: (String) -> Unit,
    onReplacePlace: (FavoritePlace) -> Unit,
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val apiEndpointDraft by viewModel.apiEndpointDraft.collectAsStateWithLifecycle()
    val apiEndpointError by viewModel.apiEndpointError.collectAsStateWithLifecycle()
    val places by viewModel.places.collectAsStateWithLifecycle()

    SettingsScreen(
        themeMode = themeMode,
        onThemeModeChange = viewModel::onThemeModeChange,
        places = places,
        onPickHome = onPickHome,
        onPickWork = onPickWork,
        onClearPlace = viewModel::removePlace,
        onAddPlace = onAddPlace,
        onReplacePlace = onReplacePlace,
        labelError = viewModel::labelError,
        apiEndpointDraft = apiEndpointDraft,
        apiEndpointError = apiEndpointError,
        onApiEndpointDraftChange = viewModel::onApiEndpointDraftChange,
        onApiEndpointSave = viewModel::onApiEndpointSave,
        appVersion = rememberAppVersion(),
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    places: List<FavoritePlace>,
    onPickHome: () -> Unit,
    onPickWork: () -> Unit,
    onClearPlace: (String) -> Unit,
    onAddPlace: (String) -> Unit,
    onReplacePlace: (FavoritePlace) -> Unit,
    labelError: (String) -> String?,
    apiEndpointDraft: String,
    apiEndpointError: String?,
    onApiEndpointDraftChange: (String) -> Unit,
    onApiEndpointSave: () -> Boolean,
    appVersion: String,
    onBack: () -> Unit,
) {
    val home = places.find { it.role == FavoriteRole.HOME }
    val work = places.find { it.role == FavoriteRole.WORK }
    val others = places.filter { it.role == FavoriteRole.OTHER }
    var addDialogOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = arrow_backIcon,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Choose how Towards looks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size,
                        ),
                        label = { Text(mode.name) },
                    )
                }
            }

            Text(
                text = "Places",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "Shortcuts on the home screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FavoritePlaceRow(
                label = FavoritePlacesStore.HOME_LABEL,
                placeName = home?.name,
                onClick = onPickHome,
                onClear = home?.let { place -> { onClearPlace(place.id) } },
                clearLabel = "Clear",
            )
            FavoritePlaceRow(
                label = FavoritePlacesStore.WORK_LABEL,
                placeName = work?.name,
                onClick = onPickWork,
                onClear = work?.let { place -> { onClearPlace(place.id) } },
                clearLabel = "Clear",
            )
            for (place in others) {
                FavoritePlaceRow(
                    label = place.label,
                    placeName = place.name,
                    onClick = { onReplacePlace(place) },
                    onClear = { onClearPlace(place.id) },
                    clearLabel = "Remove",
                )
            }
            Button(
                onClick = { addDialogOpen = true },
                enabled = others.size < FavoritePlacesStore.MAX_OTHERS,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add place")
            }
            if (others.size >= FavoritePlacesStore.MAX_OTHERS) {
                Text(
                    text = FavoritePlacesStore.TOO_MANY_MESSAGE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "API endpoint",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "Base URL for the Towards API.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = apiEndpointDraft,
                onValueChange = onApiEndpointDraftChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("URL") },
                placeholder = { Text("https://api.towards.etrange.org") },
                isError = apiEndpointError != null,
                supportingText = apiEndpointError?.let { message ->
                    { Text(message) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onApiEndpointSave() },
                ),
            )
            Button(
                onClick = { onApiEndpointSave() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save endpoint")
            }

            Text(
                text = "Legal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "Towards $appVersion",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }

    if (addDialogOpen) {
        AddPlaceLabelDialog(
            title = "Add place",
            onDismiss = { addDialogOpen = false },
            onConfirm = { label ->
                addDialogOpen = false
                onAddPlace(label)
            },
            labelError = labelError,
        )
    }
}

@Composable
private fun FavoritePlaceRow(
    label: String,
    placeName: String?,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    clearLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = placeName ?: "Not set",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClear != null) {
            TextButton(onClick = onClear) {
                Text(clearLabel)
            }
        }
    }
}

@Preview(name = "Settings · Light")
@Composable
private fun SettingsScreenLightPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        SettingsScreen(
            themeMode = ThemeMode.System,
            onThemeModeChange = {},
            places = listOf(
                FavoritePlace(
                    id = FavoritePlacesStore.HOME_ID,
                    label = FavoritePlacesStore.HOME_LABEL,
                    role = FavoriteRole.HOME,
                    name = "Grand Place",
                    coordinate = Coordinate(50.8467, 4.3525),
                ),
            ),
            onPickHome = {},
            onPickWork = {},
            onClearPlace = {},
            onAddPlace = {},
            onReplacePlace = {},
            labelError = { null },
            apiEndpointDraft = "http://127.0.0.1:8081",
            apiEndpointError = null,
            onApiEndpointDraftChange = {},
            onApiEndpointSave = { true },
            appVersion = "26.10.0",
            onBack = {},
        )
    }
}

@Preview(name = "Settings · Dark")
@Composable
private fun SettingsScreenDarkPreview() {
    TowardsPreview(themeMode = ThemeMode.Dark) {
        SettingsScreen(
            themeMode = ThemeMode.Dark,
            onThemeModeChange = {},
            places = emptyList(),
            onPickHome = {},
            onPickWork = {},
            onClearPlace = {},
            onAddPlace = {},
            onReplacePlace = {},
            labelError = { null },
            apiEndpointDraft = "not-a-url",
            apiEndpointError = "Enter a valid http(s) URL with a host, for example https://api.towards.etrange.org",
            onApiEndpointDraftChange = {},
            onApiEndpointSave = { true },
            appVersion = "26.10.0",
            onBack = {},
        )
    }
}
