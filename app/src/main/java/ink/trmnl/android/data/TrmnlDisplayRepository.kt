package ink.trmnl.android.data

import com.slack.eithernet.ApiResult
import com.squareup.anvil.annotations.optional.SingleIn
import ink.trmnl.android.BuildConfig.USE_FAKE_API
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.model.TrmnlDeviceType
import ink.trmnl.android.network.TrmnlApiService
import ink.trmnl.android.network.TrmnlApiService.Companion.CURRENT_PLAYLIST_SCREEN_API_PATH
import ink.trmnl.android.network.TrmnlApiService.Companion.NEXT_PLAYLIST_SCREEN_API_PATH
import ink.trmnl.android.util.HTTP_200
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
            if (repositoryConfigProvider.shouldUseFakeData) {
                // Avoid using real API in debug mode
                return fakeTrmnlDisplayInfo(apiUsed = "next-image")
            }

            Timber.i("Fetching next playlist item display data from server")

            val result =
                apiService
                    .getNextDisplayData(
                        fullApiUrl = constructApiUrl(trmnlDeviceConfig.apiBaseUrl, NEXT_PLAYLIST_SCREEN_API_PATH),
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
                            imageName = response.imageName ?: "",
                            error = response.error,
                            refreshIntervalSeconds = response.refreshRate,
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
         * @param trmnlDeviceConfig Device configuration containing the access token and other settings.
         * @return A [TrmnlDisplayInfo] object containing the current display data.
         */
        suspend fun getCurrentDisplayData(trmnlDeviceConfig: TrmnlDeviceConfig): TrmnlDisplayInfo {
            if (repositoryConfigProvider.shouldUseFakeData) {
                // Avoid using real API in debug mode
                return fakeTrmnlDisplayInfo(apiUsed = "current-image")
            }

            Timber.i("Fetching current display data from server")

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
                            imageName = response.filename ?: "",
                            error = response.error,
                            refreshIntervalSeconds = response.refreshRateSec,
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
         * Generates fake display info for debugging purposes without wasting an API request.
         *
         * ℹ️ This is only used when [RepositoryConfigProvider.shouldUseFakeData] is true.
         */
        private suspend fun fakeTrmnlDisplayInfo(apiUsed: String): TrmnlDisplayInfo {
            Timber.d("DEBUG: Using mock data for display info")
            val timestampMin = System.currentTimeMillis() / 60_000 // Changes every minute
            val mockImageUrl = "https://picsum.photos/300/200?grayscale&time=$timestampMin&api=$apiUsed"
            val mockRefreshRate = 600L

            // Save mock data to the data store
            imageMetadataStore.saveImageMetadata(mockImageUrl, mockRefreshRate)

            return TrmnlDisplayInfo(
                status = HTTP_200,
                trmnlDeviceType = TrmnlDeviceType.TRMNL,
                imageUrl = mockImageUrl,
                imageName = "mocked-image-" + mockImageUrl.substringAfterLast('?'),
                error = null,
                refreshIntervalSeconds = mockRefreshRate,
            )
        }

        /**
         * Constructs the full URL for API requests based on the configured base URL for device.
         */
        private fun constructApiUrl(
            baseUrl: String,
            endpoint: String,
        ): String =
            if (baseUrl.endsWith("/")) {
                "${baseUrl}$endpoint"
            } else {
                "$baseUrl/$endpoint"
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
            when (failure) {
                /**
                 * Handles API-specific failures, returning a [TrmnlDisplayInfo] with a generic API failure message.
                 */
                is ApiResult.Failure.ApiFailure -> {
                    TrmnlDisplayInfo(
                        status = HTTP_500,
                        trmnlDeviceType = trmnlDeviceConfig.type,
                        imageUrl = "",
                        imageName = "",
                        error = "API failure",
                        refreshIntervalSeconds = 0L,
                    )
                }

                /**
                 * Handles HTTP failures, including the HTTP status code and error message in the result.
                 */
                is ApiResult.Failure.HttpFailure -> {
                    TrmnlDisplayInfo(
                        status = HTTP_500,
                        trmnlDeviceType = trmnlDeviceConfig.type,
                        imageUrl = "",
                        imageName = "",
                        error = "HTTP failure: ${failure.code}, error: ${failure.error}",
                        refreshIntervalSeconds = 0L,
                    )
                }

                /**
                 * Handles network-related failures, including the localized error message in the result.
                 */
                is ApiResult.Failure.NetworkFailure -> {
                    TrmnlDisplayInfo(
                        status = HTTP_500,
                        trmnlDeviceType = trmnlDeviceConfig.type,
                        imageUrl = "",
                        imageName = "",
                        error = "Network failure: ${failure.error.localizedMessage}",
                        refreshIntervalSeconds = 0L,
                    )
                }

                /**
                 * Handles unknown failures, including the localized error message in the result.
                 */
                is ApiResult.Failure.UnknownFailure -> {
                    TrmnlDisplayInfo(
                        status = HTTP_500,
                        trmnlDeviceType = trmnlDeviceConfig.type,
                        imageUrl = "",
                        imageName = "",
                        error = "Unknown failure: ${failure.error.localizedMessage}",
                        refreshIntervalSeconds = 0L,
                    )
                }
            }
    }
