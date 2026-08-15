package ink.trmnl.android.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
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
import com.slack.circuit.runtime.rememberAnsweringNavigator
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import ink.trmnl.android.R
import ink.trmnl.android.data.AppConfig.DEFAULT_REFRESH_INTERVAL_SEC
import ink.trmnl.android.data.AppConfig.TRMNL_API_SERVER_BASE_URL
import ink.trmnl.android.data.DeviceSetupInfo
import ink.trmnl.android.data.TrmnlDeviceConfigDataStore
import ink.trmnl.android.data.TrmnlDisplayRepository
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.model.DeviceModelSelection
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.ui.aboutapp.AppInfoScreen
import ink.trmnl.android.ui.devicemodel.DeviceModelSelectorScreen
import ink.trmnl.android.ui.display.TrmnlMirrorDisplayScreen
import ink.trmnl.android.ui.icons.Icons
import ink.trmnl.android.ui.refreshlog.DisplayRefreshLogScreen
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
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen for configuring the TRMNL app settings.
 *
 * This screen allows users to:
 * - Configure API authentication (access token or device ID)
 * - Set custom server URLs for BYOS installations
 * - Set user access tokens for BYOD devices
 * - Configure refresh intervals and behavior
 * - Manage display preferences
 * - Validate settings before saving
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
        val isByodMasterDevice: Boolean,
        val isTokenConfigured: Boolean = false,
        val isLoading: Boolean = false,
        val validationResult: ValidationResult? = null,
        val isDeviceSetupLoading: Boolean = false,
        val deviceSetupMessage: String? = null,
        val nextRefreshJobInfo: NextImageRefreshDisplayInfo? = null,
        val savedDeviceModel: DeviceModelSelection? = null,
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

        data class UserTokenSuccess(
            val userName: String,
            val userEmail: String,
        ) : ValidationResult()

        data class InvalidUserToken(
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
         * Event triggered when BYOD master device setting is changed.
         */
        data class ByodMasterDeviceChanged(
            val isMaster: Boolean,
        ) : Event()

        /**
         * Event triggered when the info icon is clicked.
         */
        data object AppInfoPressed : Event()

        data class SetupDevice(
            val deviceMacId: String,
        ) : Event()

        /**
         * Event triggered when the override display model button is clicked.
         */
        data object OverrideDisplayModelPressed : Event()

        /**
         * Event triggered when the user wants to view the refresh logs.
         */
        data object ViewLogsRequested : Event()
    }
}

/**
 * Presenter for the [AppSettingsScreen].
 * Manages the screen's state and handles events from the UI.
 */
@AssistedInject
class AppSettingsPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: AppSettingsScreen,
    private val displayRepository: TrmnlDisplayRepository,
    private val deviceConfigStore: TrmnlDeviceConfigDataStore,
    private val trmnlWorkScheduler: TrmnlWorkScheduler,
    private val trmnlImageUpdateManager: TrmnlImageUpdateManager,
) : Presenter<AppSettingsScreen.State> {
    @Composable
    override fun present(): AppSettingsScreen.State {
        var deviceType by remember { mutableStateOf(TrmnlDeviceType.TRMNL) }
        var serverBaseUrl by remember { mutableStateOf("") }
        var accessToken by remember { mutableStateOf("") }
        var deviceMacId by remember { mutableStateOf("") }
        var isByodMasterDevice by remember { mutableStateOf(true) }
        var isTokenConfigured by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var validationResult by remember { mutableStateOf<ValidationResult?>(null) }
        var isDeviceSetupLoading by remember { mutableStateOf(false) }
        var deviceSetupMessage by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current

        val nextRefreshInfo by produceState<NextImageRefreshDisplayInfo?>(null) {
            trmnlWorkScheduler.getScheduledWorkInfo().collect { workInfo ->
                value = workInfo?.nextRunTime()
            }
        }

        // Load saved device model preference based on current device type
        // Flow automatically updates when preferences change in DataStore
        // Use a single collector that filters by current deviceType value instead of restarting on deviceType change
        val savedDeviceModel by produceState<DeviceModelSelection?>(initialValue = null) {
            deviceConfigStore.deviceModelPreferencesFlow.collect { preferences ->
                // Update value based on current deviceType (captured from closure)
                val newValue = preferences[deviceType.name]
                if (value != newValue) {
                    value = newValue
                }
            }
        }

        // Create answering navigator for DeviceModelSelectorScreen
        val deviceModelNavigator =
            rememberAnsweringNavigator<DeviceModelSelectorScreen.Result>(navigator) { result ->
                // Save the selected device model using the device type from the result
                // This ensures we save to the correct device type even if the user
                // switched device types while on the selector screen
                scope.launch {
                    deviceConfigStore.saveDeviceModelForType(
                        deviceType = result.deviceType,
                        modelName = result.selectedModel.name,
                        modelLabel = result.selectedModel.label,
                    )
                    Timber.d(
                        "Saved device model preference: ${result.deviceType.name} -> ${result.selectedModel.name}",
                    )
                }
            }

        // Load saved token if available
        LaunchedEffect(Unit) {
            deviceConfigStore.deviceConfigFlow.collect { config ->
                if (config != null && config.apiAccessToken.isNotBlank()) {
                    isTokenConfigured = true
                    deviceType = config.type
                    accessToken = config.apiAccessToken

                    if (config.type == TrmnlDeviceType.BYOS) {
                        // On initial load, prefill only if the device type is BYOS
                        serverBaseUrl = config.apiBaseUrl
                        config.deviceMacId?.let { savedDeviceId ->
                            deviceMacId = savedDeviceId
                        }
                    }

                    // Load BYOD-specific settings
                    if (config.type == TrmnlDeviceType.BYOD) {
                        isByodMasterDevice = config.isMasterDevice ?: true
                    }
                } else {
                    isTokenConfigured = false
                }
            }
        }

        return AppSettingsScreen.State(
            deviceType = deviceType,
            serverBaseUrl = serverBaseUrl,
            accessToken = accessToken,
            deviceMacId = deviceMacId,
            isByodMasterDevice = isByodMasterDevice,
            isTokenConfigured = isTokenConfigured,
            isLoading = isLoading,
            validationResult = validationResult,
            isDeviceSetupLoading = isDeviceSetupLoading,
            deviceSetupMessage = deviceSetupMessage,
            nextRefreshJobInfo = nextRefreshInfo,
            savedDeviceModel = savedDeviceModel,
            eventSink = { event ->
                when (event) {
                    is AppSettingsScreen.Event.AccessTokenChanged -> {
                        accessToken = event.token.trim()
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
                            // For TRMNL device type, use getCurrentDisplayData
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
                                // Determine isMasterDevice based on device type
                                val isMaster =
                                    when (deviceType) {
                                        TrmnlDeviceType.BYOD -> isByodMasterDevice
                                        TrmnlDeviceType.BYOS -> true
                                        TrmnlDeviceType.TRMNL -> false
                                    }

                                deviceConfigStore.saveDeviceConfig(
                                    TrmnlDeviceConfig(
                                        type = deviceType,
                                        apiBaseUrl = serverBaseUrl.forDevice(deviceType),
                                        apiAccessToken = accessToken,
                                        refreshRateSecs = result.refreshRateSecs,
                                        // Normalize the MAC address to standard format if provided in different format
                                        deviceMacId = normalizeMacAddress(deviceMacId)?.ifBlank { null },
                                        isMasterDevice = isMaster,
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
                        trmnlWorkScheduler.cancelPeriodicImageRefreshWork()
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

                    is AppSettingsScreen.Event.ByodMasterDeviceChanged -> {
                        isByodMasterDevice = event.isMaster
                    }

                    AppSettingsScreen.Event.AppInfoPressed -> {
                        // Navigate to AppInfoScreen
                        navigator.goTo(AppInfoScreen)
                    }

                    AppSettingsScreen.Event.ViewLogsRequested -> {
                        // Navigate to DisplayRefreshLogScreen
                        navigator.goTo(DisplayRefreshLogScreen)
                    }

                    AppSettingsScreen.Event.OverrideDisplayModelPressed -> {
                        // Navigate to DeviceModelSelectorScreen using answering navigator
                        // Pass the current device type so the screen knows which type this selection is for
                        Timber.d("Navigating to DeviceModelSelectorScreen for device type: ${deviceType.name}")
                        deviceModelNavigator.goTo(DeviceModelSelectorScreen(deviceType))
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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    // Add navigation bar padding to bottom content
                    .navigationBarsPadding()
                    // Add IME padding for keyboard handling
                    .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.trmnl_logo_plain),
                    contentDescription = "TRMNL Logo",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .padding(bottom = 8.dp),
                )

                Text(
                    text = "Display Configuration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Configure your TRMNL connection and refresh settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }

            // Main Configuration Card
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Device Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    DeviceTypeSelectorConfig(
                        selectedType = state.deviceType,
                        serverUrl = state.serverBaseUrl,
                        deviceId = state.deviceMacId,
                        isByodMasterDevice = state.isByodMasterDevice,
                        savedDeviceModel = state.savedDeviceModel,
                        onTypeSelected = { state.eventSink(AppSettingsScreen.Event.DeviceTypeChanged(it)) },
                        onServerUrlChanged = { state.eventSink(AppSettingsScreen.Event.ServerUrlChanged(it)) },
                        onDeviceIdChanged = { state.eventSink(AppSettingsScreen.Event.DeviceMacIdChanged(it)) },
                        onByodMasterDeviceChanged = { state.eventSink(AppSettingsScreen.Event.ByodMasterDeviceChanged(it)) },
                        onOverrideDisplayModelPressed = { state.eventSink(AppSettingsScreen.Event.OverrideDisplayModelPressed) },
                        isServerUrlError = state.validationResult is InvalidServerUrl,
                        serverUrlError = (state.validationResult as? InvalidServerUrl)?.message,
                        isDeviceMacIdError = state.validationResult is ValidationResult.InvalidDeviceMacId,
                        deviceIdError = (state.validationResult as? ValidationResult.InvalidDeviceMacId)?.message,
                    )

                    OutlinedTextField(
                        value = state.accessToken,
                        onValueChange = { state.eventSink(AppSettingsScreen.Event.AccessTokenChanged(it)) },
                        label = { Text("Device API Key") },
                        placeholder = { Text("Enter your API key / token") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.accessToken.isNotEmpty()) {
                                    IconButton(onClick = { state.eventSink(AppSettingsScreen.Event.AccessTokenChanged("")) }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear token",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (passwordVisible) R.drawable.visibility_off_24dp else R.drawable.visibility_24dp,
                                            ),
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    )
                                }
                            }
                        },
                    )

                    SwitchDeviceTypeInfoText(deviceType = state.deviceType)

                    Button(
                        onClick = { state.eventSink(AppSettingsScreen.Event.ValidateToken) },
                        enabled = state.accessToken.isNotBlank() && !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validating Token...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validate Token")
                        }
                    }
                }
            }

            // Show validation result
            AnimatedVisibility(
                visible = state.validationResult != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                state.validationResult?.let { result ->
                    when (result) {
                        is Success -> {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        Text(
                                            "Token Validated",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    ) {
                                        Text(
                                            text = "Token: $maskedToken",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        )
                                    }

                                    // Image preview using Coil with improved caching and styled container
                                    AsyncImage(
                                        model = CoilRequestUtils.createCachedImageRequest(context, result.imageUrl),
                                        contentDescription = "Preview image",
                                        contentScale = ContentScale.Fit,
                                        modifier =
                                            Modifier
                                                .size(240.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                                .padding(4.dp),
                                    )

                                    Button(
                                        onClick = { state.eventSink(AppSettingsScreen.Event.SaveAndContinue) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Save and Continue")
                                    }
                                }
                            }
                        }

                        is InvalidServerUrl -> {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Text(
                                        "Server URL Invalid",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        result.message,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }

                        is ValidationResult.InvalidDeviceMacId -> {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Text(
                                        "Invalid MAC Address Format",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        result.message,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }

                        is Failure -> {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Text(
                                        "Validation Failed",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        result.message,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }

                        is ValidationResult.UserTokenSuccess -> {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Text(
                                        "User Token Valid",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        "Welcome, ${result.userName}!",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        result.userEmail,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        is ValidationResult.InvalidUserToken -> {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Text(
                                        "Invalid User Token",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        result.message,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }

                        is ValidationResult.DeviceSetupRequired -> {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Text(
                                        "Device Setup Required",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                    if (state.deviceSetupMessage != null) {
                                        Text(
                                            text = state.deviceSetupMessage,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        )
                                    } else {
                                        Text(
                                            text = "This device needs initial registration with the server before continuing.",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        )
                                        FilledTonalButton(
                                            onClick = {
                                                state.eventSink(
                                                    AppSettingsScreen.Event.SetupDevice(
                                                        deviceMacId = state.deviceMacId,
                                                    ),
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !state.isDeviceSetupLoading,
                                        ) {
                                            if (state.isDeviceSetupLoading) {
                                                CircularProgressIndicator(
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Setting up...")
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
            }

            // Refresh Schedule Status Section - only show if a valid token has been configured
            AnimatedVisibility(
                visible = state.isTokenConfigured,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                WorkScheduleStatusCard(state = state, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DeviceTypeSelectorConfig(
    selectedType: TrmnlDeviceType,
    serverUrl: String = "",
    deviceId: String = "",
    isByodMasterDevice: Boolean = true,
    savedDeviceModel: DeviceModelSelection? = null,
    onTypeSelected: (TrmnlDeviceType) -> Unit,
    onServerUrlChanged: (String) -> Unit,
    onDeviceIdChanged: (String) -> Unit,
    onByodMasterDeviceChanged: (Boolean) -> Unit = {},
    onOverrideDisplayModelPressed: () -> Unit = {},
    isServerUrlError: Boolean = false,
    serverUrlError: String? = null,
    isDeviceMacIdError: Boolean = false,
    deviceIdError: String? = null,
    modifier: Modifier = Modifier,
) {
    // Control device ID visibility (defaults to visible for MAC address text)
    var deviceIdVisible by remember { mutableStateOf(true) }

    /**
     * Device model choosing is disabled for now as it is not supported yet.
     *
     * See following references:
     * - https://github.com/usetrmnl/trmnl-android/issues/229
     * - https://github.com/usetrmnl/trmnl-android/issues/163
     * - https://discord.com/channels/1281055965508141100/1331360842809348106/1446953346278625433
     * - https://discord.com/channels/1281055965508141100/1331360842809348106/1446954432007766140
     * - https://discord.com/channels/1281055965508141100/1331360842809348106/1446959843507306678
     * - https://discord.com/channels/1281055965508141100/1331360842809348106/1446959897571889182
     */
    val shouldDisableDeviceModel = true

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = serverUrl,
                    onServerUrlChanged,
                    label = { Text("API Server Base URL") },
                    placeholder = { Text("https://your-trmnl-server.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    isError = isServerUrlError,
                    supportingText = {
                        if (isServerUrlError && serverUrlError != null) {
                            Text(serverUrlError)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                    singleLine = true,
                )

                // Device ID (MAC address) input field
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = onDeviceIdChanged,
                    label = { Text("Device ID (MAC Address)") },
                    placeholder = { Text("A1:B2:C3:D4:E5:F6") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    isError = isDeviceMacIdError,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next,
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

        // BYOD Master/Slave Configuration
        AnimatedVisibility(
            visible = selectedType == TrmnlDeviceType.BYOD,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isByodMasterDevice,
                            onCheckedChange = { onByodMasterDeviceChanged(it) },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Act as master device",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text =
                                    "Uncheck if this device should mirror another BYOD device " +
                                        "that automatically auto-advances playlist image",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Show saved device model if available
                if (!shouldDisableDeviceModel && savedDeviceModel != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Text(
                                text = "Current Display Model",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = savedDeviceModel.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                if (!shouldDisableDeviceModel) {
                    OutlinedButton(
                        onClick = onOverrideDisplayModelPressed,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Override Display Model")
                    }
                }
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
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val nextRefreshJobInfo: NextImageRefreshDisplayInfo? = state.nextRefreshJobInfo

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Refresh Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (nextRefreshJobInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = nextRefreshJobInfo.workerState.toColor().copy(alpha = 0.15f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = nextRefreshJobInfo.workerState.toIcon(),
                                contentDescription = null,
                                tint = nextRefreshJobInfo.workerState.toColor(),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = nextRefreshJobInfo.workerState.toDisplayString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = nextRefreshJobInfo.workerState.toColor(),
                            )
                        }
                    }
                }
            }

            if (nextRefreshJobInfo != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Column {
                                Text(
                                    text = "Next Refresh",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = nextRefreshJobInfo.timeUntilNextRefresh,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        Text(
                            text = "Scheduled for: ${nextRefreshJobInfo.nextRefreshOnDateTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 26.dp),
                        )
                    }
                }

                // Add action buttons
                if (nextRefreshJobInfo.workerState == WorkInfo.State.ENQUEUED ||
                    nextRefreshJobInfo.workerState == WorkInfo.State.RUNNING ||
                    nextRefreshJobInfo.workerState == WorkInfo.State.BLOCKED
                ) {
                    OutlinedButton(
                        onClick = {
                            state.eventSink(AppSettingsScreen.Event.ViewLogsRequested)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "View logs",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Refresh Logs")
                    }

                    Button(
                        onClick = {
                            state.eventSink(AppSettingsScreen.Event.CancelScheduledWork)
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Cancel scheduled work",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Periodic Refresh Job")
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No scheduled refresh work found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
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
                    isByodMasterDevice = true,
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
                    isByodMasterDevice = true,
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
                    isByodMasterDevice = true,
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
                    isByodMasterDevice = true,
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
                    isByodMasterDevice = true,
                    isTokenConfigured = true,
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
                    isByodMasterDevice = true,
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
                    isByodMasterDevice = true,
                    nextRefreshJobInfo = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "App Settings Content - BYOD Selected")
@Composable
private fun PreviewAppSettingsContentByod() {
    TrmnlDisplayAppTheme {
        AppSettingsContent(
            state =
                AppSettingsScreen.State(
                    deviceType = TrmnlDeviceType.BYOD,
                    serverBaseUrl = "https://trmnl.com",
                    accessToken = "byod-access-token-here",
                    deviceMacId = "",
                    isByodMasterDevice = false,
                    isLoading = false,
                    validationResult = null,
                    nextRefreshJobInfo = null,
                    savedDeviceModel = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "App Settings Content - BYOS Selected")
@Composable
private fun PreviewAppSettingsContentByos() {
    TrmnlDisplayAppTheme {
        AppSettingsContent(
            state =
                AppSettingsScreen.State(
                    deviceType = TrmnlDeviceType.BYOS,
                    serverBaseUrl = "https://my-custom-server.com",
                    accessToken = "byos-access-token-here",
                    deviceMacId = "AA:BB:CC:DD:EE:FF",
                    isByodMasterDevice = true,
                    isLoading = false,
                    validationResult = null,
                    nextRefreshJobInfo = null,
                    savedDeviceModel = null,
                    eventSink = {},
                ),
        )
    }
}
