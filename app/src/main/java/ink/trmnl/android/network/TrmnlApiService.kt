package ink.trmnl.android.network

import com.slack.eithernet.ApiResult
import ink.trmnl.android.data.TrmnlDisplayRepository
import ink.trmnl.android.model.TrmnlSetupResponse
import ink.trmnl.android.network.model.TrmnlCurrentImageResponse
import ink.trmnl.android.network.model.TrmnlDisplayResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

/**
 * API service interface for TRMNL.
 *
 * This interface defines the endpoints for the TRMNL API.
 *
 * See:
 * - https://docs.usetrmnl.com/go
 * - https://docs.usetrmnl.com/go/private-api/introduction
 *
 * @see TrmnlDisplayRepository
 */
interface TrmnlApiService {
    companion object {
        /**
         * Path for the TRMNL API endpoint that provides the next image in a playlist.
         *
         * https://docs.usetrmnl.com/go/private-api/fetch-screen-content#auto-advance-content
         *
         * @see getNextDisplayData
         */
        internal const val NEXT_PLAYLIST_SCREEN_API_PATH = "api/display"

        /**
         * Path for the TRMNL API endpoint that provides the current image in a playlist.
         *
         * https://docs.usetrmnl.com/go/private-api/fetch-screen-content#current-screen
         *
         * @see getCurrentDisplayData
         */
        internal const val CURRENT_PLAYLIST_SCREEN_API_PATH = "api/current_screen"

        /**
         * Path for the TRMNL API endpoint used for new device setup.
         *
         * https://github.com/usetrmnl/byos_hanami?tab=readme-ov-file#setup-1
         */
        internal const val SETUP_API_PATH = "api/setup"
    }

    /**
     * Retrieve TRMNL image data, device-free using [NEXT_PLAYLIST_SCREEN_API_PATH].
     *
     * NOTE: This API always loads the next plugin image from the playlist.
     *
     * @param fullApiUrl The complete API URL to call
     * @param accessToken The device's API key (required)
     * @param deviceMacId The device's MAC address (optional)
     * @param useBase64 Whether to request Base64-encoded image data (optional)
     *
     * @see getCurrentDisplayData
     */
    @GET
    suspend fun getNextDisplayData(
        @Url fullApiUrl: String,
        @Header("access-token") accessToken: String,
        @Header("ID") deviceMacId: String? = null,
        @Header("BASE64") useBase64: Boolean? = null,
    ): ApiResult<TrmnlDisplayResponse, Unit>

    /**
     * Retrieve TRMNL image that is currently being displayed using [CURRENT_PLAYLIST_SCREEN_API_PATH].
     *
     * NOTE: This API always loads the current plugin image from the playlist.
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
    @GET
    suspend fun setupNewDevice(
        @Url fullApiUrl: String,
        @Header("ID") deviceMacId: String,
    ): ApiResult<TrmnlSetupResponse, Unit>
}
