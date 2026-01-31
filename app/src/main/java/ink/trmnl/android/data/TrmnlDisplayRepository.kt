package ink.trmnl.android.data

import com.slack.eithernet.ApiResult
import com.slack.eithernet.exceptionOrNull
import com.squareup.anvil.annotations.optional.SingleIn
import ink.trmnl.android.BuildConfig.USE_FAKE_API
import ink.trmnl.android.data.fake.generateFakeDeviceSetupInfo
import ink.trmnl.android.data.fake.generateFakeTrmnlDisplayInfo
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.model.SupportedDeviceModel
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.network.TrmnlApiService
import ink.trmnl.android.network.TrmnlApiService.Companion.CURRENT_PLAYLIST_SCREEN_API_PATH
import ink.trmnl.android.network.TrmnlApiService.Companion.MODELS_API_PATH
import ink.trmnl.android.network.TrmnlApiService.Companion.NEXT_PLAYLIST_SCREEN_API_PATH
import ink.trmnl.android.network.TrmnlUserApiService
import ink.trmnl.android.network.TrmnlUserApiService.Companion.DEVICE_API_PATH
import ink.trmnl.android.network.TrmnlUserApiService.Companion.USER_INFO_API_PATH
import ink.trmnl.android.network.model.TrmnlDevice
import ink.trmnl.android.network.model.TrmnlDeviceModel
import ink.trmnl.android.network.model.TrmnlDeviceUpdateRequest
import ink.trmnl.android.network.model.TrmnlDisplayResponse
import ink.trmnl.android.network.model.TrmnlUser
import ink.trmnl.android.network.util.constructApiUrl
import ink.trmnl.android.network.util.extractHttpResponseMetadata
import ink.trmnl.android.network.util.extractHttpResponseMetadataFromFailure
import ink.trmnl.android.util.AndroidDeviceInfoProvider
import ink.trmnl.android.util.HTTP_500
import ink.trmnl.android.util.isHttpOk
import timber.log.Timber
import javax.inject.Inject

/**
 * Repository class responsible for fetching and mapping display data.
 *
 * ⚠️ NOTE: [USE_FAKE_API] is set to `true` in debug builds, meaning it will
 * use mock data and avoid network calls. In release builds, it is set to `false`
 * to enable real API calls.
 *
 * You can override this behavior by updating [RepositoryConfigProvider.shouldUseFakeData] for local development.
 */
@SingleIn(AppScope::class)
class TrmnlDisplayRepository
    @Inject
    constructor(
        private val apiService: TrmnlApiService,
        private val userApiService: TrmnlUserApiService,
        private val imageMetadataStore: ImageMetadataStore,
        private val repositoryConfigProvider: RepositoryConfigProvider,
        private val androidDeviceInfoProvider: AndroidDeviceInfoProvider,
    ) {
        /**
         * Fetches display data for next plugin from the server using the provided access token.
         * If the app is in debug mode, it uses mock data instead.
         *
         * @param trmnlDeviceConfig Device configuration containing the access token and other settings.
         * @return A [TrmnlDisplayInfo] object containing the display data.
         */
        suspend fun getNextDisplayData(trmnlDeviceConfig: TrmnlDeviceConfig): TrmnlDisplayInfo {
            Timber.i("Fetching next playlist item display data from server for device: ${trmnlDeviceConfig.type}")

            if (repositoryConfigProvider.shouldUseFakeData) {
                // Avoid using real API in debug mode
                return generateFakeTrmnlDisplayInfo(imageMetadataStore = imageMetadataStore, apiUsed = "next-image")
            }

            val result =
                apiService
                    .getNextDisplayData(
                        fullApiUrl = constructApiUrl(trmnlDeviceConfig.apiBaseUrl, NEXT_PLAYLIST_SCREEN_API_PATH),
                        accessToken = trmnlDeviceConfig.apiAccessToken,
                        // Send device MAC ID if available (used for BYOS service)
                        deviceMacId = trmnlDeviceConfig.deviceMacId,
                        // TEMP FIX: Use Base64 encoding to avoid relative path issue
                        // See https://github.com/usetrmnl/trmnl-android/issues/76#issuecomment-2980018109
                        // useBase64 = trmnlDeviceConfig.type == TrmnlDeviceType.BYOS, // Disabled for now
                    )

            when (result) {
                is ApiResult.Failure -> {
                    return failedTrmnlDisplayInfo(trmnlDeviceConfig, result)
                }
                is ApiResult.Success -> {
                    // Map the response to the display info
                    val response: TrmnlDisplayResponse = result.value

                    if (isDeviceSetupRequired(trmnlDeviceConfig, response)) {
                        return TrmnlDisplayInfo.setupRequired()
                    }

                    val displayInfo =
                        TrmnlDisplayInfo(
                            status = response.status,
                            trmnlDeviceType = trmnlDeviceConfig.type,
                            imageUrl = response.imageUrl ?: "",
                            imageFileName = response.imageFileName ?: "",
                            error = response.error,
                            refreshIntervalSeconds = response.refreshRate,
                            httpResponseMetadata = extractHttpResponseMetadata(result),
                        )

                    // If response was successful and has an image URL, save to data store
                    if (response.status.isHttpOk() && displayInfo.imageUrl.isNotEmpty()) {
                        imageMetadataStore.saveImageMetadata(
                            displayInfo.imageUrl,
                            displayInfo.refreshIntervalSeconds,
                        )
                    }

                    return displayInfo
                }
            }
        }

        /**
         * Fetches the current display data from the server using the provided access token.
         * If the app is in debug mode, it uses mock data instead.
         *
         * ⚠️ NOTE: This API is not available on BYOS servers.
         * See https://discord.com/channels/1281055965508141100/1331360842809348106/1382863253880963124
         *
         * @param trmnlDeviceConfig Device configuration containing the access token and other settings.
         * @return A [TrmnlDisplayInfo] object containing the current display data.
         */
        suspend fun getCurrentDisplayData(trmnlDeviceConfig: TrmnlDeviceConfig): TrmnlDisplayInfo {
            Timber.i("Fetching current display data from server for device: ${trmnlDeviceConfig.type}")

            if (trmnlDeviceConfig.type == TrmnlDeviceType.BYOS) {
                Timber.w("Current display image data API is not available for BYOS service.")
            }

            if (repositoryConfigProvider.shouldUseFakeData) {
                // Avoid using real API in debug mode
                return generateFakeTrmnlDisplayInfo(imageMetadataStore = imageMetadataStore, apiUsed = "current-image")
            }

            val result =
                apiService
                    .getCurrentDisplayData(
                        fullApiUrl = constructApiUrl(trmnlDeviceConfig.apiBaseUrl, CURRENT_PLAYLIST_SCREEN_API_PATH),
                        accessToken = trmnlDeviceConfig.apiAccessToken,
                    )

            when (result) {
                is ApiResult.Failure -> {
                    return failedTrmnlDisplayInfo(trmnlDeviceConfig, result)
                }
                is ApiResult.Success -> {
                    // Map the response to the display info
                    val response = result.value
                    val displayInfo =
                        TrmnlDisplayInfo(
                            status = response.status,
                            trmnlDeviceType = trmnlDeviceConfig.type,
                            imageUrl = response.imageUrl ?: "",
                            imageFileName = response.filename ?: "",
                            error = response.error,
                            refreshIntervalSeconds = response.refreshRateSec,
                            httpResponseMetadata = extractHttpResponseMetadata(result),
                        )

                    // If response was successful and has an image URL, save to data store
                    if (response.status.isHttpOk() && displayInfo.imageUrl.isNotEmpty()) {
                        imageMetadataStore.saveImageMetadata(
                            displayInfo.imageUrl,
                            displayInfo.refreshIntervalSeconds,
                        )
                    }

                    return displayInfo
                }
            }
        }

        /**
         * Sets up a new device by calling the setup API endpoint.
         *
         * This is only applicable for BYOS devices, as other device types do not require setup.
         *
         * @param trmnlDeviceConfig The configuration for the device to be set up.
         * @return A [DeviceSetupInfo] object containing the result of the setup operation.
         */
        suspend fun setupNewDevice(trmnlDeviceConfig: TrmnlDeviceConfig): DeviceSetupInfo {
            if (trmnlDeviceConfig.type != TrmnlDeviceType.BYOS) {
                Timber.w("Device setup is only applicable for BYOS devices.")
            }

            if (repositoryConfigProvider.shouldUseFakeData) {
                // Avoid using real API in debug mode
                return generateFakeDeviceSetupInfo()
            }

            val result =
                apiService.setupNewDevice(
                    fullApiUrl = constructApiUrl(trmnlDeviceConfig.apiBaseUrl, TrmnlApiService.SETUP_API_PATH),
                    deviceMacId = requireNotNull(trmnlDeviceConfig.deviceMacId) { "Device MAC ID is required for setup" },
                )
            when (result) {
                is ApiResult.Failure -> {
                    Timber.e("Failed to setup device: ${result.exceptionOrNull()}")
                    return DeviceSetupInfo(
                        success = false,
                        deviceMacId = trmnlDeviceConfig.deviceMacId,
                        apiKey = "",
                        message = "Failed to setup device with ID (${trmnlDeviceConfig.deviceMacId}). Reason: $result",
                    )
                }
                is ApiResult.Success -> {
                    Timber.i("Device setup successful: ${result.value}")
                    return DeviceSetupInfo(
                        success = true,
                        deviceMacId = trmnlDeviceConfig.deviceMacId,
                        apiKey = result.value.apiKey,
                        message = result.value.message,
                    )
                }
            }
        }

        /**
         * Converts an API failure result into a [TrmnlDisplayInfo] object with appropriate error details.
         *
         * This function handles different types of API failures and maps them to a standardized
         * [TrmnlDisplayInfo] object. The returned object contains error information and default values
         * for other fields.
         */
        private fun failedTrmnlDisplayInfo(
            trmnlDeviceConfig: TrmnlDeviceConfig,
            failure: ApiResult.Failure<Unit>,
        ): TrmnlDisplayInfo =
            TrmnlDisplayInfo(
                status = (failure as? ApiResult.Failure.HttpFailure)?.code ?: HTTP_500,
                trmnlDeviceType = trmnlDeviceConfig.type,
                imageUrl = "",
                imageFileName = "",
                error =
                    when (failure) {
                        is ApiResult.Failure.ApiFailure -> "API request failed to process response"
                        is ApiResult.Failure.HttpFailure -> "HTTP failure: ${failure.code}, error: ${failure.error}"
                        is ApiResult.Failure.NetworkFailure -> "Network failure: ${failure.error.localizedMessage}"
                        is ApiResult.Failure.UnknownFailure -> "Unknown failure: ${failure.error.localizedMessage}"
                    },
                refreshIntervalSeconds = 0L,
                httpResponseMetadata = extractHttpResponseMetadataFromFailure(failure),
            )

        /**
         * Right now there is no good known way to determine if a device requires setup.
         * The logic here is based on sample responses from the Terminus server API.
         *
         * See
         * - https://discord.com/channels/1281055965508141100/1331360842809348106/1384605617456545904
         * - https://discord.com/channels/1281055965508141100/1384605617456545904/1384613229086511135
         */
        private fun isDeviceSetupRequired(
            trmnlDeviceConfig: TrmnlDeviceConfig,
            response: TrmnlDisplayResponse,
        ): Boolean =
            (trmnlDeviceConfig.type == TrmnlDeviceType.BYOS) &&
                (response.imageFileName?.startsWith("setup", ignoreCase = true) == true) &&
                // This ensures that no screen is generated yet for the device
                // Example (when device is not set up):
                // -- "filename": "setup"
                // -- "image_url": "/assets/setup-A2B2C2.svg"
                // Example (when device is set up):
                // -- "filename": "setup.png"
                // -- "image_url": "https://my-trmnl-hub.com/assets/screens/ABCDEF123/setup.png",
                (response.imageUrl?.contains("screens", ignoreCase = true) == false)

        /**
         * Fetches the list of available device models from the TRMNL API.
         *
         * This provides information about all supported device models including
         * display specifications, supported palettes, and device characteristics.
         *
         * The API response is converted to simplified [SupportedDeviceModel] DTOs containing
         * only the essential information needed for device selection.
         *
         * @param serverBaseUrl The base URL of the server to fetch models from (defaults to TRMNL API).
         * @return A list of [SupportedDeviceModel] objects, or an empty list on failure.
         */
        suspend fun getDeviceModels(serverBaseUrl: String): List<SupportedDeviceModel> {
            Timber.i("Fetching device models from server: $serverBaseUrl")

            val result =
                apiService.getDeviceModels(
                    fullApiUrl = constructApiUrl(serverBaseUrl, MODELS_API_PATH),
                )

            return when (result) {
                is ApiResult.Failure -> {
                    Timber.e("Failed to fetch device models: ${result.exceptionOrNull()}")
                    emptyList()
                }
                is ApiResult.Success -> {
                    Timber.i("Successfully fetched ${result.value.data.size} device models")
                    // Convert API models to simplified SupportedDeviceModel DTOs
                    result.value.data.map { it.toSupportedDeviceModel() }
                }
            }
        }

        /**
         * Converts a [TrmnlDeviceModel] API response to a simplified [SupportedDeviceModel] DTO.
         *
         * This extension function extracts only the essential device information needed
         * for UI purposes, making it Parcelable for navigation results.
         */
        private fun TrmnlDeviceModel.toSupportedDeviceModel() =
            SupportedDeviceModel(
                name = name,
                label = label,
                description = description,
                width = width,
                height = height,
                colors = colors,
                bitDepth = bitDepth,
                scaleFactor = scaleFactor,
                rotation = rotation,
                mimeType = mimeType,
                kind = kind,
            )

        /**
         * Validates a user API token by calling the /api/me endpoint.
         *
         * This method is used to verify user-level (account) API tokens before saving them.
         * On success, returns the user's information.
         *
         * @param apiBaseUrl The base URL of the TRMNL API server
         * @param userApiToken The user API token to validate (should start with "user_")
         * @return A Result containing TrmnlUser on success or an exception on failure
         */
        suspend fun validateUserApiToken(
            apiBaseUrl: String,
            userApiToken: String,
        ): Result<TrmnlUser> {
            Timber.i("Validating user API token")

            if (repositoryConfigProvider.shouldUseFakeData) {
                // Return fake user data in debug mode
                return Result.success(
                    TrmnlUser(
                        id = 42,
                        name = "Test User",
                        email = "test@example.com",
                        firstName = "Test",
                        lastName = "User",
                        locale = "en",
                        timeZone = "Eastern Time (US & Canada)",
                        timeZoneIana = "America/New_York",
                        utcOffset = -14400,
                    ),
                )
            }

            val result =
                userApiService.getUserInfo(
                    fullApiUrl = constructApiUrl(apiBaseUrl, USER_INFO_API_PATH),
                    accessToken = "Bearer $userApiToken",
                )

            return when (result) {
                is ApiResult.Failure -> {
                    val exception = result.exceptionOrNull()
                    Timber.e(exception, "User API token validation failed")
                    Result.failure(exception ?: Exception("Failed to validate user token"))
                }
                is ApiResult.Success -> {
                    Timber.i("User API token validated successfully: ${result.value.data.email}")
                    Result.success(result.value.data)
                }
            }
        }

        /**
         * Fetches the device ID from the TRMNL API using the device API token.
         *
         * This method calls the /api/devices/me endpoint with device-level authentication
         * to retrieve device information including the device ID, which is needed for
         * user-level API calls to /api/devices/{id}.
         *
         * **Note:** This endpoint doesn't exist on the server yet, so this method
         * returns a mocked response until the server endpoint is implemented.
         *
         * @param config Device configuration containing the device API token
         * @return A Result containing the device ID on success or an exception on failure
         */
        suspend fun getDeviceIdFromApi(config: TrmnlDeviceConfig): Result<Int> {
            Timber.i("Fetching device ID from API for device type: ${config.type}")

            // Always use mocked response since the endpoint doesn't exist yet
            // TODO: Remove this mock when the server endpoint is implemented
            val mockedDevice =
                TrmnlDevice(
                    id = 41448,
                    name = "BYOD TRMNL",
                    friendlyId = "_____",
                    macAddress = "********",
                    batteryVoltage = null,
                    rssi = null,
                    sleepModeEnabled = false,
                    sleepStartTime = 1320,
                    sleepEndTime = 480,
                    percentCharged = 100.0,
                    wifiStrength = 100.0,
                )

            Timber.i("Using mocked device ID: ${mockedDevice.id}")
            return Result.success(mockedDevice.id)

            /*
             * TODO: Uncomment this when the server endpoint is implemented:
             *
             * val result = apiService.getDeviceMe(
             *     fullApiUrl = constructApiUrl(config.apiBaseUrl, DEVICE_ME_API_PATH),
             *     accessToken = config.apiAccessToken,
             * )
             *
             * return when (result) {
             *     is ApiResult.Failure -> {
             *         val exception = result.exceptionOrNull()
             *         Timber.e(exception, "Failed to fetch device ID")
             *         Result.failure(exception ?: Exception("Failed to fetch device ID"))
             *     }
             *     is ApiResult.Success -> {
             *         val deviceId = result.value.data.id
             *         Timber.i("Device ID fetched successfully: $deviceId")
             *         Result.success(deviceId)
             *     }
             * }
             */
        }

        /**
         * Reports the device's battery status to the TRMNL API for BYOD devices.
         *
         * This is a convenience method that checks if the device is a BYOD device with the necessary
         * configuration (deviceId and userApiToken), retrieves the current battery level,
         * and reports it to the server.
         *
         * This method should be called after successful image refresh operations.
         *
         * @param config Device configuration containing device type, device ID, and user API token
         */
        suspend fun reportDeviceBatteryStatus(config: TrmnlDeviceConfig) {
            // Only report battery for BYOD devices with required configuration
            if (config.type != TrmnlDeviceType.BYOD) {
                Timber.d("Battery reporting skipped: not a BYOD device (type: ${config.type})")
                return
            }

            if (config.deviceId == null) {
                Timber.w("Battery reporting skipped: device ID is null")
                return
            }

            if (config.userApiToken == null) {
                Timber.w("Battery reporting skipped: user API token is null")
                return
            }

            // Get current battery level
            val batteryLevel = androidDeviceInfoProvider.getBatteryLevel()
            if (batteryLevel == null) {
                Timber.w("Battery reporting skipped: unable to get battery level")
                return
            }

            // Report battery status
            reportBatteryStatus(config, batteryLevel)
        }

        /**
         * Reports the device's battery status to the TRMNL API.
         *
         * This method sends a PATCH request to /api/devices/{id} using user-level authentication
         * to update the device's battery percentage on the server.
         *
         * This operation is non-blocking and should not affect display updates.
         *
         * @param config Device configuration containing the device ID and user API token
         * @param batteryPercent The current battery percentage (0-100)
         * @return A Result containing Unit on success or an exception on failure
         */
        private suspend fun reportBatteryStatus(
            config: TrmnlDeviceConfig,
            batteryPercent: Int,
        ): Result<Unit> {
            val deviceId = config.deviceId
            val userApiToken = config.userApiToken

            if (deviceId == null) {
                Timber.w("Cannot report battery status: device ID is null")
                return Result.failure(IllegalStateException("Device ID is required"))
            }

            if (userApiToken == null) {
                Timber.w("Cannot report battery status: user API token is null")
                return Result.failure(IllegalStateException("User API token is required"))
            }

            Timber.d("Reporting battery status: $batteryPercent% for device ID: $deviceId")

            if (repositoryConfigProvider.shouldUseFakeData) {
                // Skip API call in debug mode
                Timber.d("Skipping battery status report (fake API mode)")
                return Result.success(Unit)
            }

            val updateRequest = TrmnlDeviceUpdateRequest(percentCharged = batteryPercent.toDouble())
            val apiUrl = constructApiUrl(config.apiBaseUrl, DEVICE_API_PATH.replace("{id}", deviceId.toString()))

            val result =
                userApiService.updateDevice(
                    fullApiUrl = apiUrl,
                    accessToken = "Bearer $userApiToken",
                    updateRequest = updateRequest,
                )

            return when (result) {
                is ApiResult.Failure -> {
                    val exception = result.exceptionOrNull()
                    Timber.e(exception, "Failed to report battery status")
                    Result.failure(exception ?: Exception("Failed to report battery status"))
                }
                is ApiResult.Success -> {
                    Timber.d("Battery status reported successfully")
                    Result.success(Unit)
                }
            }
        }
    }
