package ink.trmnl.android.network

import com.slack.eithernet.ApiResult
import ink.trmnl.android.data.TrmnlDisplayRepository
import ink.trmnl.android.network.model.TrmnlCurrentImageResponse
import ink.trmnl.android.network.model.TrmnlDeviceResponse
import ink.trmnl.android.network.model.TrmnlDisplayResponse
import ink.trmnl.android.network.model.TrmnlModelsResponse
import ink.trmnl.android.network.model.TrmnlSetupResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Url

/**
 * API service interface for TRMNL device-level API endpoints.
 *
 * This interface defines endpoints that require device-level authentication via
 * Access-Token header (Device API key), as opposed to user-level Bearer token authentication.
 *
 * For user-level (account) API endpoints, see [TrmnlUserApiService].
 *
 * See:
 * - https://docs.trmnl.com/go
 * - https://docs.trmnl.com/go/private-api/introduction
 * - https://trmnl.com/api-docs/index.html (OpenAPI documentation)
 *
 * @see TrmnlDisplayRepository
 * @see TrmnlUserApiService
 */
interface TrmnlApiService {
    companion object {
        /**
         * Path for the TRMNL API endpoint that provides the next image in a playlist.
         *
         * - https://docs.trmnl.com/go/private-api/fetch-screen-content#auto-advance-content
         * - https://github.com/usetrmnl/byos_hanami?tab=readme-ov-file#display
         *
         * @see getNextDisplayData
         */
        internal const val NEXT_PLAYLIST_SCREEN_API_PATH = "api/display"

        /**
         * Path for the TRMNL API endpoint that provides the current image in a playlist.
         *
         * https://docs.trmnl.com/go/private-api/fetch-screen-content#current-screen
         *
         * @see getCurrentDisplayData
         */
        internal const val CURRENT_PLAYLIST_SCREEN_API_PATH = "api/current_screen"

        /**
         * Path for the TRMNL API endpoint used for new device setup.
         *
         * https://github.com/usetrmnl/byos_hanami?tab=readme-ov-file#setup-1
         *
         * @see setupNewDevice
         */
        internal const val SETUP_API_PATH = "api/setup/"

        /**
         * Path for the TRMNL API endpoint that provides the list of available device models.
         *
         * https://help.trmnl.com/en/articles/11547008-device-model-faq
         *
         * @see getDeviceModels
         */
        internal const val MODELS_API_PATH = "api/models"

        /**
         * Path for the TRMNL API endpoint to get the device information.
         *
         * **Authentication:** Requires device-level Access-Token header
         *
         * **Note:** This endpoint doesn't exist on the server yet. The repository layer
         * provides a mocked response until the server endpoint is implemented.
         *
         * See https://discord.com/channels/1281055965508141100/1466924426460397765
         *
         * @see getDeviceMe
         */
        internal const val DEVICE_ME_API_PATH = "api/devices/me"
    }

    /**
     * Retrieve TRMNL image data, device-free using [NEXT_PLAYLIST_SCREEN_API_PATH].
     *
     * NOTE: This API always loads the next plugin image from the playlist.
     *
     * See [API Doc](https://docs.trmnl.com/go/private-api/screens) for additional details.
     *
     * @param fullApiUrl The complete API URL to call
     * @param accessToken The device's API key (required)
     * @param deviceMacId The device's MAC address (optional)
     * @param useBase64 Whether to request Base64-encoded image data (optional)
     * @param rssi WiFi signal strength in dBm (optional, -100 to 0). See https://github.com/usetrmnl/trmnl-firmware/blob/main/src/api-client/display.cpp for additional references.
     * @param percentCharged Battery percentage (optional, 0.0 to 100.0). Only sent for BYOD devices.
     *
     * @see getCurrentDisplayData
     */
    @GET
    suspend fun getNextDisplayData(
        @Url fullApiUrl: String,
        @Header("access-token") accessToken: String,
        @Header("ID") deviceMacId: String? = null,
        @Header("BASE64") useBase64: Boolean? = null,
        @Header("RSSI") rssi: Int? = null,
        @Header("Percent-Charged") percentCharged: Int? = null,
    ): ApiResult<TrmnlDisplayResponse, Unit>

    /**
     * Retrieve TRMNL image that is currently being displayed using [CURRENT_PLAYLIST_SCREEN_API_PATH].
     *
     * NOTE: This API always loads the current plugin image from the playlist.
     *
     * See [API Doc](https://docs.trmnl.com/go/private-api/screens) for additional details.
     *
     * @see getNextDisplayData
     */
    @GET
    suspend fun getCurrentDisplayData(
        @Url fullApiUrl: String,
        @Header("access-token") accessToken: String,
    ): ApiResult<TrmnlCurrentImageResponse, Unit>

    /**
     * Setup a new TRMNL device using it's MAC ID. Using same API with same ID has no effect.
     *
     * This API is typically used once during the initial setup of a BYOS device.
     * See https://github.com/usetrmnl/byos_hanami?tab=readme-ov-file#setup-1
     *
     * @param fullApiUrl The complete API URL to call (e.g., "https://your-server.com/api/setup").
     * @param deviceMacId The device's MAC address, sent in the "ID" header.
     * @return An [ApiResult] containing [TrmnlSetupResponse] on success.
     */
    @Headers("Content-Type: application/json")
    @GET
    suspend fun setupNewDevice(
        @Url fullApiUrl: String,
        @Header("ID") deviceMacId: String,
    ): ApiResult<TrmnlSetupResponse, Unit>

    /**
     * Retrieve the list of available device models using [MODELS_API_PATH].
     *
     * This endpoint provides information about all supported device models including
     * display specifications, supported palettes, and device characteristics.
     *
     * See https://help.trmnl.com/en/articles/11547008-device-model-faq
     *
     * @param fullApiUrl The complete API URL to call (e.g., "https://trmnl.com/api/models")
     * @return An [ApiResult] containing [TrmnlModelsResponse] with the list of device models
     */
    @GET
    suspend fun getDeviceModels(
        @Url fullApiUrl: String,
    ): ApiResult<TrmnlModelsResponse, Unit>

    /**
     * Retrieve device information using [DEVICE_ME_API_PATH].
     *
     * This endpoint provides device details including the device ID, which is needed
     * for making user-level API calls to `/api/devices/{id}`.
     *
     * **Authentication:** Requires device-level Access-Token header (device API key)
     *
     * **Note:** This endpoint doesn't exist on the server yet. The repository layer
     * provides a mocked response until the server endpoint is implemented.
     *
     * See https://discord.com/channels/1281055965508141100/1466924426460397765
     *
     * @param fullApiUrl The complete API URL to call (e.g., "https://trmnl.com/api/devices/me")
     * @param accessToken The device's API key (required)
     * @return An [ApiResult] containing [TrmnlDeviceResponse] with the device information
     */
    @GET
    suspend fun getDeviceMe(
        @Url fullApiUrl: String,
        @Header("access-token") accessToken: String,
    ): ApiResult<TrmnlDeviceResponse, Unit>
}
