package ink.trmnl.android.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import coil3.compose.AsyncImage
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import ink.trmnl.android.R
import ink.trmnl.android.data.AppConfig.DEFAULT_REFRESH_INTERVAL_SEC
import ink.trmnl.android.data.AppConfig.TRMNL_API_SERVER_BASE_URL
import ink.trmnl.android.data.DeviceSetupInfo
import ink.trmnl.android.data.RepositoryConfigProvider
import ink.trmnl.android.data.TrmnlDeviceConfigDataStore
import ink.trmnl.android.data.TrmnlDisplayRepository
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.ui.aboutapp.AppInfoScreen
import ink.trmnl.android.ui.display.TrmnlMirrorDisplayScreen
import ink.trmnl.android.ui.settings.AppSettingsScreen.ValidationResult
import ink.trmnl.android.ui.settings.AppSettingsScreen.ValidationResult.Failure
import ink.trmnl.android.ui.settings.AppSettingsScreen.ValidationResult.InvalidServerUrl
import ink.trmnl.android.ui.settings.AppSettingsScreen.ValidationResult.Success
import ink.trmnl.android.ui.theme.TrmnlDisplayAppTheme
import ink.trmnl.android.util.CoilRequestUtils
import ink.trmnl.android.util.ERROR_TYPE_DEVICE_SETUP_REQUIRED
import ink.trmnl.android.util.NextImageRefreshDisplayInfo
import ink.trmnl.android.util.isHttpError
import ink.trmnl.android.util.isValidMacAddress
import ink.trmnl.android.util.isValidUrl
import ink.trmnl.android.util.nextRunTime
import ink.trmnl.android.util.normalizeMacAddress
import ink.trmnl.android.util.toColor
import ink.trmnl.android.util.toDisplayString
import ink.trmnl.android.util.toIcon
import ink.trmnl.android.work.TrmnlImageUpdateManager
import ink.trmnl.android.work.TrmnlWorkScheduler
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen for configuring the TRMNL mirror app settings.
 *
 * This screen allows users to set up and validate their TRMNL access token,
 * which is required to connect to the TRMNL API service. It displays validation
 * results, image previews when successful, and provides options to save the
 * configuration which in turn schedules refresh job using [TrmnlWorkScheduler].
 *
 * The screen can be configured to either return to the mirror display after
 * saving or to pop back to the previous screen.
 */
@Parcelize
data class AppSettingsScreen(
    val returnToMirrorAfterSave: Boolean = false,
) : Screen {
    data class State(
        val deviceType: TrmnlDeviceType,
        val serverBaseUrl: String,
        val accessToken: String,
        val deviceMacId: String,
        val usesFakeApiData: Boolean,
        val isLoading: Boolean = false,
        val validationResult: ValidationResult? = null,
        val isDeviceSetupLoading: Boolean = false,
        val deviceSetupMessage: String? = null,
        val nextRefreshJobInfo: NextImageRefreshDisplayInfo? = null,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class ValidationResult {
        data class Success(
            val imageUrl: String,
            val refreshRateSecs: Long,
        ) : ValidationResult()

        data class InvalidServerUrl(
            val message: String,
        ) : ValidationResult()

        data class InvalidDeviceMacId(
            val message: String,
        ) : ValidationResult()

        data class Failure(
            val message: String,
        ) : ValidationResult()

        data class DeviceSetupRequired(
            val message: String,
        ) : ValidationResult()
    }

    /**
     * Events that can be triggered from the AppSettingsScreen UI.
     */
    sealed class Event : CircuitUiEvent {
        /**
         * Event triggered when the access token is changed.
         */
        data class AccessTokenChanged(
            val token: String,
        ) : Event()

        /**
         * Event triggered to validate the current access token.
         */
        data object ValidateToken : Event()

        /**
         * Event triggered to save the settings and continue to the next screen.
         */
        data object SaveAndContinue : Event()

        /**
         * Event triggered when the back button is pressed.
         */
        data object BackPressed : Event()

        /**
         * Event triggered to cancel the scheduled work.
         */
        data object CancelScheduledWork : Event()

        data class DeviceTypeChanged(
            val type: TrmnlDeviceType,
        ) : Event()

        data class ServerUrlChanged(
            val url: String,
        ) : Event()

        /**
         * Event triggered when the device ID (MAC address) is changed.
         */
        data class DeviceMacIdChanged(
            val deviceMacId: String,
        ) : Event()

        /**
         * Event triggered when the info icon is clicked.
         */
        data object AppInfoPressed : Event()

        data class SetupDevice(
            val deviceMacId: String,
        ) : Event()
    }
}

/**
 * Presenter for the [AppSettingsScreen].
 * Manages the screen's state and handles events from the UI.
 */
class AppSettingsPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        @Assisted private val screen: AppSettingsScreen,
        private val displayRepository: TrmnlDisplayRepository,
        private val deviceConfigStore: TrmnlDeviceConfigDataStore,
        private val trmnlWorkScheduler: TrmnlWorkScheduler,
        private val trmnlImageUpdateManager: TrmnlImageUpdateManager,
        private val repositoryConfigProvider: RepositoryConfigProvider,
    ) : Presenter<AppSettingsScreen.State> {
        @Composable
        override fun present(): AppSettingsScreen.State {
            var deviceType by remember { mutableStateOf(TrmnlDeviceType.TRMNL) }
            var serverBaseUrl by remember { mutableStateOf("") }
            var accessToken by remember { mutableStateOf("") }
            var deviceMacId by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var validationResult by remember { mutableStateOf<ValidationResult?>(null) }
            var isDeviceSetupLoading by remember { mutableStateOf(false) }
            var deviceSetupMessage by remember { mutableStateOf<String?>(null) }
            val usesFakeApiData = repositoryConfigProvider.shouldUseFakeData
            val scope = rememberCoroutineScope()
            val focusManager = LocalFocusManager.current

            val nextRefreshInfo by produceState<NextImageRefreshDisplayInfo?>(null) {
                trmnlWorkScheduler.getScheduledWorkInfo().collect { workInfo ->
                    value = workInfo?.nextRunTime()
                }
            }

            // Load saved token if available
            LaunchedEffect(Unit) {
                deviceConfigStore.deviceConfigFlow.filterNotNull().collect {
                    deviceType = it.type
                    accessToken = it.apiAccessToken

                    if (it.type == TrmnlDeviceType.BYOS) {
                        // On initial load, prefill only if the device type is BYOS
                        serverBaseUrl = it.apiBaseUrl
                        it.deviceMacId?.let { savedDeviceId ->
                            deviceMacId = savedDeviceId
                        }
                    }
                }
            }

            return AppSettingsScreen.State(
                deviceType = deviceType,
                serverBaseUrl = serverBaseUrl,
                accessToken = accessToken,
                deviceMacId = deviceMacId,
                usesFakeApiData = usesFakeApiData,
                isLoading = isLoading,
                validationResult = validationResult,
                isDeviceSetupLoading = isDeviceSetupLoading,
                deviceSetupMessage = deviceSetupMessage,
                nextRefreshJobInfo = nextRefreshInfo,
                eventSink = { event ->
                    when (event) {
                        is AppSettingsScreen.Event.AccessTokenChanged -> {
                            accessToken = event.token
                            // Clear previous validation when token changes
                            validationResult = null
                            deviceSetupMessage = null
                        }

                        AppSettingsScreen.Event.ValidateToken -> {
                            scope.launch {
                                focusManager.clearFocus()
                                isLoading = true
                                validationResult = null
                                deviceSetupMessage = null

                                // First validate server URL if device type is BYOS
                                if (deviceType == TrmnlDeviceType.BYOS) {
                                    if (!isValidUrl(serverBaseUrl)) {
                                        isLoading = false
                                        validationResult = InvalidServerUrl("Please enter a valid HTTPS URL (e.g. https://my-terminus.com)")
                                        return@launch
                                    }

                                    // If device ID is provided, validate MAC address format (only for BYOS)
                                    if (deviceMacId.isNotBlank() && !isValidMacAddress(deviceMacId)) {
                                        isLoading = false
                                        validationResult =
                                            ValidationResult.InvalidDeviceMacId(
                                                "Please enter a valid MAC address format:\n" +
                                                    "• XX:XX:XX:XX:XX:XX\n" +
                                                    "• XX-XX-XX-XX-XX-XX\n" +
                                                    "• XXXXXXXXXXXX\n" +
                                                    "where X is a hexadecimal digit (0-9, A-F)",
                                            )
                                        return@launch
                                    }
                                }

                                // Device configuration for API calls
                                val deviceConfig =
                                    TrmnlDeviceConfig(
                                        type = deviceType,
                                        apiBaseUrl = serverBaseUrl.forDevice(deviceType),
                                        apiAccessToken = accessToken,
                                        deviceMacId = deviceMacId.ifBlank { null },
                                    )
                                // For TRMNL mirror device type, use getCurrentDisplayData
                                // For all other device types, use getNextDisplayData
                                // See https://discord.com/channels/1281055965508141100/1331360842809348106/1382865608236077086
                                val response =
                                    when (deviceType) {
                                        TrmnlDeviceType.TRMNL -> {
                                            displayRepository.getCurrentDisplayData(deviceConfig)
                                        }
                                        else -> {
                                            displayRepository.getNextDisplayData(deviceConfig)
                                        }
                                    }

                                if (response.status.isHttpError()) {
                                    if (response.imageFileName == ERROR_TYPE_DEVICE_SETUP_REQUIRED) {
                                        // Special case for device setup required
                                        validationResult =
                                            ValidationResult.DeviceSetupRequired(
                                                response.error ?: "Device setup required. Please follow the setup instructions.",
                                            )
                                    } else {
                                        // Handle explicit error response
                                        val errorMessage = response.error ?: "Unexpected error occurred. Please check required inputs."
                                        validationResult = Failure(errorMessage)
                                    }
                                } else if (response.imageUrl.isNotBlank()) {
                                    // Success case - we have an image URL
                                    trmnlImageUpdateManager.updateImage(response.imageUrl, response.refreshIntervalSeconds)
                                    validationResult =
                                        Success(
                                            response.imageUrl,
                                            response.refreshIntervalSeconds ?: DEFAULT_REFRESH_INTERVAL_SEC,
                                        )
                                } else {
                                    // No error but also no image URL
                                    val errorMessage = response.error ?: ""
                                    validationResult = Failure("$errorMessage No image URL received.")
                                }
                                isLoading = false
                            }
                        }

                        AppSettingsScreen.Event.SaveAndContinue -> {
                            // Only save if validation was successful
                            val result = validationResult
                            if (result is Success) {
                                scope.launch {
                                    deviceConfigStore.saveDeviceConfig(
                                        TrmnlDeviceConfig(
                                            type = deviceType,
                                            apiBaseUrl = serverBaseUrl.forDevice(deviceType),
                                            apiAccessToken = accessToken,
                                            refreshRateSecs = result.refreshRateSecs,
                                            // Normalize the MAC address to standard format if provided in different format
                                            deviceMacId = normalizeMacAddress(deviceMacId)?.ifBlank { null },
                                        ),
                                    )
                                    trmnlWorkScheduler.updateRefreshInterval(result.refreshRateSecs)

                                    if (screen.returnToMirrorAfterSave) {
                                        navigator.goTo(TrmnlMirrorDisplayScreen)
                                    } else {
                                        navigator.pop()
                                    }
                                }
                            }
                        }

                        AppSettingsScreen.Event.BackPressed -> {
                            navigator.pop()
                        }

                        AppSettingsScreen.Event.CancelScheduledWork -> {
                            trmnlWorkScheduler.cancelImageRefreshWork()
                        }

                        is AppSettingsScreen.Event.DeviceTypeChanged -> {
                            deviceType = event.type
                            // Clear validation result when device type changes
                            validationResult = null
                            deviceSetupMessage = null
                        }

                        is AppSettingsScreen.Event.ServerUrlChanged -> {
                            serverBaseUrl = event.url
                            // Clear validation result when server URL changes
                            if (validationResult is InvalidServerUrl) {
                                validationResult = null
                                deviceSetupMessage = null
                            }
                        }

                        is AppSettingsScreen.Event.DeviceMacIdChanged -> {
                            deviceMacId = event.deviceMacId
                            // Clear previous validation when device ID changes
                            validationResult = null
                            deviceSetupMessage = null
                        }

                        AppSettingsScreen.Event.AppInfoPressed -> {
                            // Navigate to AppInfoScreen
                            navigator.goTo(AppInfoScreen)
                        }

                        is AppSettingsScreen.Event.SetupDevice -> {
                            isDeviceSetupLoading = true
                            deviceSetupMessage = null

                            scope.launch {
                                // Call the setup API with the provided device ID
                                val setupResult: DeviceSetupInfo =
                                    displayRepository.setupNewDevice(
                                        TrmnlDeviceConfig(
                                            type = deviceType,
                                            apiBaseUrl = serverBaseUrl.forDevice(deviceType),
                                            apiAccessToken = accessToken,
                                            deviceMacId = event.deviceMacId,
                                        ),
                                    )

                                isDeviceSetupLoading = false

                                if (!setupResult.success) {
                                    // Handle error response
                                    deviceSetupMessage = setupResult.message
                                } else {
                                    deviceSetupMessage = "Device setup successful! Re-validate ID/Token to continue."
                                    // Also prepopulate the access token
                                    accessToken = setupResult.apiKey
                                }
                            }
                        }
                    }
                },
            )
        }

        /**
         * Returns the server base URL for the [deviceType] (custom or TRMNL server).
         */
        private fun String.forDevice(deviceType: TrmnlDeviceType): String =
            if (deviceType == TrmnlDeviceType.BYOS) {
                // For BYOS, use the provided custom server URL
                this
            } else {
                // For any other device type, use the default TRMNL API server URL
                TRMNL_API_SERVER_BASE_URL
            }

        @CircuitInject(AppSettingsScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(
                navigator: Navigator,
                screen: AppSettingsScreen,
            ): AppSettingsPresenter
        }
    }

/**
 * Main Composable function for rendering the AppSettingsScreen.
 * Sets up the screen's structure including form, validation result display, and work schedule status.
 */
@CircuitInject(AppSettingsScreen::class, AppScope::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsContent(
    state: AppSettingsScreen.State,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val hasToken = state.accessToken.isNotBlank()

    // Control password visibility
    var passwordVisible by remember { mutableStateOf(false) }

    // Create masked version of the token for display
    val maskedToken =
        with(state.accessToken) {
            if (length > 4) {
                "${take(2)}${"*".repeat(length - 4)}${takeLast(2)}"
            } else {
                this // Don't mask if token is too short
            }
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    // Only show the back button if a token is already set
                    if (hasToken) {
                        IconButton(onClick = { state.eventSink(AppSettingsScreen.Event.BackPressed) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                // Add the info action button to navigate to AppInfoScreen
                actions = {
                    IconButton(onClick = { state.eventSink(AppSettingsScreen.Event.AppInfoPressed) }) {
                        Icon(
                            painter = painterResource(R.drawable.info_24dp),
                            contentDescription = "App Info",
                        )
                    }
                },
                // Configure the TopAppBar to handle status bar insets
                windowInsets = WindowInsets.statusBars,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Use safeDrawing insets to handle all system bars and cutouts
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp)
                    // Add navigation bar padding to bottom content
                    .navigationBarsPadding()
                    // Add IME padding for keyboard handling
                    .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (state.usesFakeApiData) {
                FakeApiInfoBanner(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                )
            }

            Icon(
                painter = painterResource(R.drawable.trmnl_logo_plain),
                contentDescription = "TRMNL Logo",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .size(64.dp)
                        .padding(bottom = 16.dp),
            )

            Text(
                text = "Display Mirror Configuration",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            DeviceTypeSelectorConfig(
                selectedType = state.deviceType,
                serverUrl = state.serverBaseUrl,
                deviceId = state.deviceMacId,
                onTypeSelected = { state.eventSink(AppSettingsScreen.Event.DeviceTypeChanged(it)) },
                onServerUrlChanged = { state.eventSink(AppSettingsScreen.Event.ServerUrlChanged(it)) },
                onDeviceIdChanged = { state.eventSink(AppSettingsScreen.Event.DeviceMacIdChanged(it)) },
                isServerUrlError = state.validationResult is InvalidServerUrl,
                serverUrlError = (state.validationResult as? InvalidServerUrl)?.message,
                isDeviceMacIdError = state.validationResult is ValidationResult.InvalidDeviceMacId,
                deviceIdError = (state.validationResult as? ValidationResult.InvalidDeviceMacId)?.message,
            )

            // Password field with toggle visibility button
            OutlinedTextField(
                value = state.accessToken,
                onValueChange = { state.eventSink(AppSettingsScreen.Event.AccessTokenChanged(it)) },
                label = { Text("Access Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            state.eventSink(AppSettingsScreen.Event.ValidateToken)
                        },
                    ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(if (passwordVisible) R.drawable.visibility_off_24dp else R.drawable.visibility_24dp),
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchDeviceTypeInfoText(deviceType = state.deviceType)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { state.eventSink(AppSettingsScreen.Event.ValidateToken) },
                enabled = state.accessToken.isNotBlank() && !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Validate Token")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show validation result
            state.validationResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    when (result) {
                        is Success -> {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "✅ Token Valid",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )

                                Text(
                                    "Token: $maskedToken",
                                    style = MaterialTheme.typography.bodyMedium,
                                )

                                // Image preview using Coil with improved caching
                                AsyncImage(
                                    model = CoilRequestUtils.createCachedImageRequest(context, result.imageUrl),
                                    contentDescription = "Preview image",
                                    contentScale = ContentScale.Fit,
                                    modifier =
                                        Modifier
                                            .size(240.dp)
                                            .padding(4.dp),
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { state.eventSink(AppSettingsScreen.Event.SaveAndContinue) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Save and Continue")
                                }
                            }
                        }
                        is InvalidServerUrl -> {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "❌ Server URL Invalid",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    result.message,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        is ValidationResult.InvalidDeviceMacId -> {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "❌ Device ID (Mac Address) Invalid Format",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    result.message,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        is Failure -> {
                            // Error state remains the same
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "❌ Validation Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    result.message,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        is ValidationResult.DeviceSetupRequired -> {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "⚠️ Device Setup Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (state.deviceSetupMessage != null) {
                                    Text(
                                        text = state.deviceSetupMessage,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    FilledTonalButton(
                                        onClick = {
                                            state.eventSink(
                                                AppSettingsScreen.Event.SetupDevice(
                                                    deviceMacId = state.deviceMacId,
                                                ),
                                            )
                                        },
                                        enabled = !state.isDeviceSetupLoading,
                                    ) {
                                        if (state.isDeviceSetupLoading) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Text("Setup Device")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(24.dp))
            WorkScheduleStatusCard(state = state, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DeviceTypeSelectorConfig(
    selectedType: TrmnlDeviceType,
    serverUrl: String = "",
    deviceId: String = "",
    onTypeSelected: (TrmnlDeviceType) -> Unit,
    onServerUrlChanged: (String) -> Unit,
    onDeviceIdChanged: (String) -> Unit,
    isServerUrlError: Boolean = false,
    serverUrlError: String? = null,
    isDeviceMacIdError: Boolean = false,
    deviceIdError: String? = null,
    modifier: Modifier = Modifier,
) {
    // Control device ID visibility
    var deviceIdVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TrmnlDeviceType.entries.forEachIndexed { index, deviceType ->
                SegmentedButton(
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TrmnlDeviceType.entries.size,
                        ),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = deviceType == selectedType) {
                            Icon(
                                painter =
                                    when (deviceType) {
                                        TrmnlDeviceType.TRMNL -> painterResource(R.drawable.trmnl_logo_plain)
                                        TrmnlDeviceType.BYOD -> painterResource(R.drawable.devices_24dp)
                                        TrmnlDeviceType.BYOS -> painterResource(R.drawable.storage_24dp)
                                    },
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = { onTypeSelected(deviceType) },
                    selected = deviceType == selectedType,
                ) {
                    Text(deviceType.name)
                }
            }
        }

        AnimatedVisibility(
            visible = selectedType == TrmnlDeviceType.BYOS,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChanged,
                    label = { Text("API Server Base URL") },
                    placeholder = { Text("https://your-trmnl-server.com") },
                    isError = isServerUrlError,
                    supportingText = {
                        if (isServerUrlError && serverUrlError != null) {
                            Text(serverUrlError)
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                    singleLine = true,
                )

                // Device ID (MAC address) input field
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = onDeviceIdChanged,
                    label = { Text("Device ID (MAC Address)") },
                    placeholder = { Text("A1:B2:C3:D4:E5:F6") },
                    isError = isDeviceMacIdError,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp, bottom = 8.dp),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                    visualTransformation = if (deviceIdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    supportingText = {
                        if (isDeviceMacIdError && deviceIdError != null) {
                            Text(deviceIdError)
                        } else {
                            Text("Required: Used for Terminus server APIs")
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { deviceIdVisible = !deviceIdVisible }) {
                            Icon(
                                painter =
                                    painterResource(
                                        if (deviceIdVisible) R.drawable.visibility_off_24dp else R.drawable.visibility_24dp,
                                    ),
                                contentDescription = if (deviceIdVisible) "Hide device ID" else "Show device ID",
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * Displays the status of the TRMNL display image refresh schedule.
 *
 * Shows details about the next scheduled refresh, its status, and provides an option
 * to cancel the scheduled work if applicable.
 */
@Composable
private fun WorkScheduleStatusCard(
    state: AppSettingsScreen.State,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "TRMNL Display Image Refresh Schedule Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            val nextRefreshJobInfo: NextImageRefreshDisplayInfo? = state.nextRefreshJobInfo
            if (nextRefreshJobInfo != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = nextRefreshJobInfo.workerState.toIcon(),
                        contentDescription = null,
                        tint = nextRefreshJobInfo.workerState.toColor(),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Status: ${nextRefreshJobInfo.workerState.toDisplayString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = nextRefreshJobInfo.workerState.toColor(),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Next refresh: ${nextRefreshJobInfo.timeUntilNextRefresh}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Text(
                    text = "Scheduled for: ${nextRefreshJobInfo.nextRefreshOnDateTime}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 28.dp, top = 2.dp),
                )

                // Add a button to cancel the work if it's scheduled
                if (nextRefreshJobInfo.workerState == WorkInfo.State.ENQUEUED ||
                    nextRefreshJobInfo.workerState == WorkInfo.State.RUNNING ||
                    nextRefreshJobInfo.workerState == WorkInfo.State.BLOCKED
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            state.eventSink(AppSettingsScreen.Event.CancelScheduledWork)
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Cancel scheduled work",
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Periodic Refresh Job")
                    }
                }
            } else {
                Text(
                    text = "No scheduled refresh work found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * A composable function that displays a banner indicating that the app is in developer mode
 * and is using mock data instead of real API calls.
 */
@Composable
private fun FakeApiInfoBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )

            Column {
                Text(
                    text = "Developer Mode - Using Fake API",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "This app is currently using mock data instead of real API calls.",
                    style = MaterialTheme.typography.bodySmall,
                )

                Text(
                    text = "Set `BuildConfig.USE_FAKE_API` to `false` using build.gradle to use real API.",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Preview(name = "App Settings Content - Initial State")
@Composable
private fun PreviewAppSettingsContentInitial() {
    TrmnlDisplayAppTheme {
        AppSettingsContent(
            state =
                AppSettingsScreen.State(
                    deviceType = TrmnlDeviceType.TRMNL,
                    serverBaseUrl = "https://example.com",
                    accessToken = "",
                    deviceMacId = "aa:bb:cc:dd:ee:ff",
                    usesFakeApiData = true,
                    isLoading = false,
                    validationResult = null,
                    nextRefreshJobInfo = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "App Settings Content - Loading State")
@Composable
private fun PreviewAppSettingsContentLoading() {
    TrmnlDisplayAppTheme {
        AppSettingsContent(
            state =
                AppSettingsScreen.State(
                    deviceType = TrmnlDeviceType.TRMNL,
                    serverBaseUrl = "https://example.com",
                    accessToken = "some-token",
                    deviceMacId = "aa:bb:cc:dd:ee:ff",
                    usesFakeApiData = false,
                    isLoading = true,
                    validationResult = null,
                    nextRefreshJobInfo = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "App Settings Content - Validation Success")
@Composable
private fun PreviewAppSettingsContentSuccess() {
    TrmnlDisplayAppTheme {
        AppSettingsContent(
            state =
                AppSettingsScreen.State(
                    deviceType = TrmnlDeviceType.TRMNL,
                    serverBaseUrl = "https://example.com",
                    accessToken = "valid-token-123",
                    deviceMacId = "aa:bb:cc:dd:ee:ff",
                    usesFakeApiData = false,
                    isLoading = false,
                    validationResult =
                        ValidationResult.Success(
                            imageUrl = "https://example.com/image.png", // Placeholder URL
                            refreshRateSecs = 3600,
                        ),
                    nextRefreshJobInfo = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "App Settings Content - Validation Failure")
@Composable
private fun PreviewAppSettingsContentFailure() {
    TrmnlDisplayAppTheme {
        AppSettingsContent(
            state =
                AppSettingsScreen.State(
                    deviceType = TrmnlDeviceType.TRMNL,
                    serverBaseUrl = "https://example.com",
                    accessToken = "invalid-token",
                    deviceMacId = "aa:bb:cc:dd:ee:ff",
                    usesFakeApiData = false,
                    isLoading = false,
                    validationResult =
                        ValidationResult.Failure(
                            message = "Invalid access token provided. Please check and try again.",
                        ),
                    nextRefreshJobInfo = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "App Settings Content - With Scheduled Work")
@Composable
private fun PreviewAppSettingsContentWithWork() {
    val formatter = DateTimeFormatter.ofPattern("MMM dd 'at' hh:mm:ss a")
    val nextRunTimeMillis = Instant.now().plusSeconds(15 * 60).toEpochMilli()
    val nextRunTimeFormatted = Instant.ofEpochMilli(nextRunTimeMillis).atZone(ZoneId.systemDefault()).format(formatter)

    TrmnlDisplayAppTheme {
        AppSettingsContent(
            state =
                AppSettingsScreen.State(
                    deviceType = TrmnlDeviceType.TRMNL,
                    serverBaseUrl = "https://example.com",
                    accessToken = "valid-token-123",
                    deviceMacId = "aa:bb:cc:dd:ee:ff",
                    usesFakeApiData = false,
                    isLoading = false,
                    validationResult = null, // Can also be Success state
                    nextRefreshJobInfo =
                        NextImageRefreshDisplayInfo(
                            workerState = WorkInfo.State.ENQUEUED,
                            timeUntilNextRefresh = "in 15 minutes",
                            nextRefreshOnDateTime = nextRunTimeFormatted,
                            nextRefreshTimeMillis = nextRunTimeMillis,
                        ),
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "Work Schedule Status Card - Scheduled")
@Composable
private fun PreviewWorkScheduleStatusCardScheduled() {
    val formatter = DateTimeFormatter.ofPattern("MMM dd 'at' hh:mm:ss a")
    val nextRunTimeMillis = Instant.now().plusSeconds(15 * 60).toEpochMilli()
    val nextRunTimeFormatted = Instant.ofEpochMilli(nextRunTimeMillis).atZone(ZoneId.systemDefault()).format(formatter)

    TrmnlDisplayAppTheme {
        WorkScheduleStatusCard(
            state =
                AppSettingsScreen.State(
                    deviceType = TrmnlDeviceType.TRMNL,
                    serverBaseUrl = "https://example.com",
                    accessToken = "some-token",
                    deviceMacId = "AA:BB:CC:DD:EE:FF",
                    usesFakeApiData = false,
                    nextRefreshJobInfo =
                        NextImageRefreshDisplayInfo(
                            workerState = WorkInfo.State.ENQUEUED,
                            timeUntilNextRefresh = "in 15 minutes",
                            nextRefreshOnDateTime = nextRunTimeFormatted,
                            nextRefreshTimeMillis = nextRunTimeMillis,
                        ),
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "Work Schedule Status Card - No Work")
@Composable
private fun PreviewWorkScheduleStatusCardNoWork() {
    TrmnlDisplayAppTheme {
        WorkScheduleStatusCard(
            state =
                AppSettingsScreen.State(
                    deviceType = TrmnlDeviceType.TRMNL,
                    serverBaseUrl = "https://example.com",
                    accessToken = "some-token",
                    deviceMacId = "aa:bb:cc:dd:ee:ff",
                    usesFakeApiData = false,
                    nextRefreshJobInfo = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "Fake API Info Banner")
@Composable
private fun PreviewFakeApiInfoBanner() {
    TrmnlDisplayAppTheme {
        FakeApiInfoBanner()
    }
}
