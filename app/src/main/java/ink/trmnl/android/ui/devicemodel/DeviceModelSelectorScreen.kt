package ink.trmnl.android.ui.devicemodel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import ink.trmnl.android.data.AppConfig.TRMNL_API_SERVER_BASE_URL
import ink.trmnl.android.data.TrmnlDisplayRepository
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.model.SupportedDeviceModel
import ink.trmnl.android.ui.theme.TrmnlDisplayAppTheme
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/**
 * A screen that displays a list of available TRMNL device models for selection.
 *
 * This screen allows users to:
 * - View all available device models with their specifications
 * - Select a device model
 * - Return the selected model to the previous screen via PopResult
 */
@Parcelize
data object DeviceModelSelectorScreen : Screen {
    /**
     * Represents the UI state for the [DeviceModelSelectorScreen].
     *
     * @property models List of available device models
     * @property isLoading Whether the models are currently being loaded
     * @property errorMessage Error message if loading fails
     * @property eventSink Function to handle UI events
     */
    data class State(
        val models: List<SupportedDeviceModel>,
        val isLoading: Boolean,
        val errorMessage: String?,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    /**
     * Events that can be triggered from the DeviceModelSelectorScreen UI.
     */
    sealed class Event : CircuitUiEvent {
        /**
         * Event triggered when the user presses the back button.
         */
        data object BackPressed : Event()

        /**
         * Event triggered when a device model is selected.
         *
         * @property model The selected device model
         */
        data class ModelSelected(
            val model: SupportedDeviceModel,
        ) : Event()

        /**
         * Event triggered to retry loading models after a failure.
         */
        data object RetryLoad : Event()
    }

    /**
     * Result returned when a device model is selected.
     *
     * This result is passed back to the previous screen that navigated to this screen
     * using Circuit's PopResult mechanism.
     *
     * @property selectedModel The device model that was selected by the user
     */
    @Parcelize
    data class Result(
        val selectedModel: SupportedDeviceModel,
    ) : PopResult
}

/**
 * Presenter for the DeviceModelSelectorScreen.
 * Manages the screen's state and handles events from the UI.
 */
class DeviceModelSelectorPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val repository: TrmnlDisplayRepository,
    ) : Presenter<DeviceModelSelectorScreen.State> {
        /**
         * Creates and returns the state for the DeviceModelSelectorScreen.
         * Fetches device models from the repository and handles user interactions.
         *
         * @return The current UI state.
         */
        @Composable
        override fun present(): DeviceModelSelectorScreen.State {
            var models by remember { mutableStateOf<List<SupportedDeviceModel>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            // Load models on first composition
            LaunchedEffect(Unit) {
                loadModels(
                    onModelsLoaded = { loadedModels ->
                        models = loadedModels
                        isLoading = false
                        errorMessage = null
                    },
                    onError = { error ->
                        models = emptyList()
                        isLoading = false
                        errorMessage = error
                    },
                )
            }

            return DeviceModelSelectorScreen.State(
                models = models,
                isLoading = isLoading,
                errorMessage = errorMessage,
                eventSink = { event ->
                    when (event) {
                        is DeviceModelSelectorScreen.Event.BackPressed -> {
                            navigator.pop()
                        }
                        is DeviceModelSelectorScreen.Event.ModelSelected -> {
                            // Pop with result to return the selected model to the previous screen
                            navigator.pop(result = DeviceModelSelectorScreen.Result(event.model))
                        }
                        is DeviceModelSelectorScreen.Event.RetryLoad -> {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                loadModels(
                                    onModelsLoaded = { loadedModels ->
                                        models = loadedModels
                                        isLoading = false
                                        errorMessage = null
                                    },
                                    onError = { error ->
                                        models = emptyList()
                                        isLoading = false
                                        errorMessage = error
                                    },
                                )
                            }
                        }
                    }
                },
            )
        }

        private suspend fun loadModels(
            onModelsLoaded: (List<SupportedDeviceModel>) -> Unit,
            onError: (String) -> Unit,
        ) {
            val loadedModels = repository.getDeviceModels(TRMNL_API_SERVER_BASE_URL)
            if (loadedModels.isEmpty()) {
                onError("Failed to load device models. Please try again.")
            } else {
                onModelsLoaded(loadedModels)
            }
        }

        /**
         * Factory interface for creating DeviceModelSelectorPresenter instances.
         */
        @CircuitInject(DeviceModelSelectorScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): DeviceModelSelectorPresenter
        }
    }

/**
 * Main composable function for rendering the DeviceModelSelectorScreen.
 * Sets up the screen's structure including toolbar, model list, and loading/error states.
 */
@CircuitInject(DeviceModelSelectorScreen::class, AppScope::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceModelSelectorContent(
    state: DeviceModelSelectorScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Select Device Model") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(DeviceModelSelectorScreen.Event.BackPressed) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when {
                state.isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading device models...",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                state.errorMessage != null -> {
                    // Error state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { state.eventSink(DeviceModelSelectorScreen.Event.RetryLoad) },
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                state.models.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No device models available",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                else -> {
                    // Success state - show list of models
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.models) { model ->
                            DeviceModelCard(
                                model = model,
                                onClick = {
                                    state.eventSink(DeviceModelSelectorScreen.Event.ModelSelected(model))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable function that renders a single device model as a clickable card.
 *
 * @param model The device model to display
 * @param onClick Callback invoked when the card is clicked
 * @param modifier Optional modifier for the card
 */
@Composable
private fun DeviceModelCard(
    model: SupportedDeviceModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Model name and label
            Text(
                text = model.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            if (model.description.isNotEmpty() && model.description != model.label) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display specifications
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    SpecificationItem(label = "Display", value = "${model.width} × ${model.height}")
                    Spacer(modifier = Modifier.height(4.dp))
                    SpecificationItem(label = "Colors", value = "${model.colors} (${model.bitDepth}-bit)")
                }
                Column {
                    SpecificationItem(label = "Type", value = model.kind)
                    Spacer(modifier = Modifier.height(4.dp))
                    SpecificationItem(label = "Format", value = model.mimeType.substringAfter("/"))
                }
            }
        }
    }
}

/**
 * Helper composable to display a specification item with a label and value.
 */
@Composable
private fun SpecificationItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview(name = "Device Model Selector - Loading")
@Composable
private fun PreviewDeviceModelSelectorLoading() {
    TrmnlDisplayAppTheme {
        DeviceModelSelectorContent(
            state =
                DeviceModelSelectorScreen.State(
                    models = emptyList(),
                    isLoading = true,
                    errorMessage = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "Device Model Selector - Error")
@Composable
private fun PreviewDeviceModelSelectorError() {
    TrmnlDisplayAppTheme {
        DeviceModelSelectorContent(
            state =
                DeviceModelSelectorScreen.State(
                    models = emptyList(),
                    isLoading = false,
                    errorMessage = "Failed to load device models. Please try again.",
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "Device Model Selector - With Models")
@Composable
private fun PreviewDeviceModelSelectorWithModels() {
    val sampleModels =
        listOf(
            SupportedDeviceModel(
                name = "v2",
                label = "TRMNL X",
                description = "TRMNL X",
                width = 1872,
                height = 1404,
                colors = 16,
                bitDepth = 4,
                scaleFactor = 1.8,
                rotation = 0,
                mimeType = "image/png",
                kind = "trmnl",
            ),
            SupportedDeviceModel(
                name = "kindle",
                label = "Kindle Paperwhite",
                description = "Kindle e-reader display",
                width = 1448,
                height = 1072,
                colors = 16,
                bitDepth = 4,
                scaleFactor = 1.0,
                rotation = 0,
                mimeType = "image/png",
                kind = "kindle",
            ),
            SupportedDeviceModel(
                name = "byod",
                label = "Custom Display",
                description = "Bring Your Own Display",
                width = 1920,
                height = 1080,
                colors = 256,
                bitDepth = 8,
                scaleFactor = 1.0,
                rotation = 0,
                mimeType = "image/png",
                kind = "byod",
            ),
        )

    TrmnlDisplayAppTheme {
        DeviceModelSelectorContent(
            state =
                DeviceModelSelectorScreen.State(
                    models = sampleModels,
                    isLoading = false,
                    errorMessage = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "Device Model Card")
@Composable
private fun PreviewDeviceModelCard() {
    TrmnlDisplayAppTheme {
        DeviceModelCard(
            model =
                SupportedDeviceModel(
                    name = "v2",
                    label = "TRMNL X",
                    description = "TRMNL X e-ink display",
                    width = 1872,
                    height = 1404,
                    colors = 16,
                    bitDepth = 4,
                    scaleFactor = 1.8,
                    rotation = 0,
                    mimeType = "image/png",
                    kind = "trmnl",
                ),
            onClick = {},
        )
    }
}
