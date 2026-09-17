package org.etrange.towards.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.etrange.towards.rememberAppVersion
import org.etrange.towards.ui.icons.arrow_backIcon
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val apiEndpointDraft by viewModel.apiEndpointDraft.collectAsStateWithLifecycle()
    val apiEndpointError by viewModel.apiEndpointError.collectAsStateWithLifecycle()

    SettingsScreen(
        themeMode = themeMode,
        onThemeModeChange = viewModel::onThemeModeChange,
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
    apiEndpointDraft: String,
    apiEndpointError: String?,
    onApiEndpointDraftChange: (String) -> Unit,
    onApiEndpointSave: () -> Boolean,
    appVersion: String,
    onBack: () -> Unit,
) {
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
            modifier = Modifier.padding(innerPadding).fillMaxSize()
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
}

@Preview(name = "Settings · Light")
@Composable
private fun SettingsScreenLightPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        SettingsScreen(
            themeMode = ThemeMode.System,
            onThemeModeChange = {},
            apiEndpointDraft = "http://127.0.0.1:8081",
            apiEndpointError = null,
            onApiEndpointDraftChange = {},
            onApiEndpointSave = { true },
            appVersion = "2026.9.0",
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
            apiEndpointDraft = "not-a-url",
            apiEndpointError = "Enter a valid http(s) URL with a host, for example https://api.towards.etrange.org",
            onApiEndpointDraftChange = {},
            onApiEndpointSave = { true },
            appVersion = "2026.9.0",
            onBack = {},
        )
    }
}
