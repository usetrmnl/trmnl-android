package ink.trmnl.android.network

import com.slack.eithernet.ApiResult
import ink.trmnl.android.network.model.TrmnlCurrentImageResponse
import ink.trmnl.android.network.model.TrmnlDisplayResponse
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * API service interface for TRMNL.
 *
 * This interface defines the endpoints for the TRMNL API.
 *
 * See:
 * - https://docs.usetrmnl.com/go
 */
interface TrmnlApiService {
    /**
     * Retrieve TRMNL image data, device-free.
     *
     * NOTE: This API always loads the next plugin image from the playlist.
     *
     * @see getCurrentDisplayData
     */
    @GET("api/display")
    suspend fun getNextDisplayData(
        @Header("access-token") accessToken: String,
    ): ApiResult<TrmnlDisplayResponse, Unit>

    /**
     * Retrieve TRMNL image that is currently being displayed.
     *
     * NOTE: This API always loads the current plugin image from the playlist.
     *
     * @see getNextDisplayData
     */
    @GET("api/current_screen")
    suspend fun getCurrentDisplayData(
        @Header("access-token") accessToken: String,
    ): ApiResult<TrmnlCurrentImageResponse, Unit>
}
