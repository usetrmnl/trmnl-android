package ink.trmnl.android.network

import com.slack.eithernet.ApiResult
import ink.trmnl.android.data.TrmnlDisplayRepository
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
         * https://docs.usetrmnl.com/go/private-api/fetch-screen-content#auto-advance-content
         *
         * @see getNextDisplayData
         */
        internal const val NEXT_PLAYLIST_SCREEN_API_PATH = "api/display"

        /**
         * https://docs.usetrmnl.com/go/private-api/fetch-screen-content#current-screen
         *
         * @see getCurrentDisplayData
         */
        internal const val CURRENT_PLAYLIST_SCREEN_API_PATH = "api/current_screen"
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
}
