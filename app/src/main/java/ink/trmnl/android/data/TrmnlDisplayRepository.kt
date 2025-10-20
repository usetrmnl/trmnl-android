package ink.trmnl.android.data

import com.slack.eithernet.ApiResult
import com.slack.eithernet.exceptionOrNull
import com.squareup.anvil.annotations.optional.SingleIn
import ink.trmnl.android.BuildConfig.USE_FAKE_API
import ink.trmnl.android.data.fake.generateFakeDeviceSetupInfo
import ink.trmnl.android.data.fake.generateFakeTrmnlDisplayInfo
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.network.TrmnlApiService
import ink.trmnl.android.network.TrmnlApiService.Companion.CURRENT_PLAYLIST_SCREEN_API_PATH
import ink.trmnl.android.network.TrmnlApiService.Companion.NEXT_PLAYLIST_SCREEN_API_PATH
import ink.trmnl.android.network.model.TrmnlDisplayResponse
import ink.trmnl.android.network.util.constructApiUrl
import ink.trmnl.android.network.util.extractHttpResponseMetadata
import ink.trmnl.android.network.util.extractHttpResponseMetadataFromFailure
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
        private val imageMetadataStore: ImageMetadataStore,
        private val repositoryConfigProvider: RepositoryConfigProvider,
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
    }
