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
 *
 * @see TrmnlDisplayRepository
 */
interface TrmnlApiService {
    companion object {
        /**
         * @see getNextDisplayData
         */
        internal const val NEXT_DISPLAY_ENDPOINT = "api/display"

        /**
         * @see getCurrentDisplayData
         */
        internal const val CURRENT_SCREEN_ENDPOINT = "api/current_screen"
    }

    /**
     * Retrieve TRMNL image data, device-free using [NEXT_DISPLAY_ENDPOINT].
     *
     * NOTE: This API always loads the next plugin image from the playlist.
     *
     * @see getCurrentDisplayData
     */
    @GET
    suspend fun getNextDisplayData(
        @Url fullApiUrl: String,
        @Header("access-token") accessToken: String,
    ): ApiResult<TrmnlDisplayResponse, Unit>

    /**
     * Retrieve TRMNL image that is currently being displayed using [CURRENT_SCREEN_ENDPOINT].
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
