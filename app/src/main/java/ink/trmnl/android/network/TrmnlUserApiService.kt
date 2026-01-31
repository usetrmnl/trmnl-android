package ink.trmnl.android.network

import com.slack.eithernet.ApiResult
import ink.trmnl.android.network.model.TrmnlDeviceResponse
import ink.trmnl.android.network.model.TrmnlDeviceUpdateRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.Url

/**
 * API service interface for TRMNL user-level (account) API endpoints.
 *
 * This interface defines endpoints that require user-level authentication via Bearer token
 * (Account API key), as opposed to device-level authentication.
 *
 * See:
 * - https://docs.usetrmnl.com/go
 * - https://trmnl.com/api-docs/index.html (OpenAPI documentation)
 */
interface TrmnlUserApiService {
    companion object {
        /**
         * Path template for the TRMNL API endpoint to get or update a specific device.
         *
         * Replace `{id}` with the actual device ID.
         *
         * **Authentication:** Requires Bearer token (user-level Account API key)
         *
         * See: https://trmnl.com/api-docs/index.html#/Devices
         *
         * @see getDevice
         * @see updateDevice
         */
        internal const val DEVICE_API_PATH = "api/devices/{id}"
    }

    /**
     * Retrieve device data for a specific device using [DEVICE_API_PATH].
     *
     * This endpoint provides information about a single device including its configuration,
     * battery status, WiFi strength, and sleep mode settings.
     *
     * **Authentication:** Requires Bearer token with user-level Account API key
     *
     * @param fullApiUrl The complete API URL to call (e.g., "https://usetrmnl.com/api/devices/1")
     * @param accessToken The bearer authentication token (format: "Bearer your_api_key")
     * @return An [ApiResult] containing [TrmnlDeviceResponse] with the device data
     */
    @GET
    suspend fun getDevice(
        @Url fullApiUrl: String,
        @Header("Authorization") accessToken: String,
    ): ApiResult<TrmnlDeviceResponse, Unit>

    /**
     * Update device settings for a specific device using [DEVICE_API_PATH].
     *
     * This endpoint allows updating device configuration such as sleep mode settings
     * and battery charge percentage.
     *
     * **Authentication:** Requires Bearer token with user-level Account API key
     *
     * @param fullApiUrl The complete API URL to call (e.g., "https://usetrmnl.com/api/devices/1")
     * @param accessToken The bearer authentication token (format: "Bearer your_api_key")
     * @param updateRequest The device update request containing the fields to update
     * @return An [ApiResult] containing [TrmnlDeviceResponse] with the updated device data
     */
    @Headers("Content-Type: application/json")
    @PATCH
    suspend fun updateDevice(
        @Url fullApiUrl: String,
        @Header("Authorization") accessToken: String,
        @Body updateRequest: TrmnlDeviceUpdateRequest,
    ): ApiResult<TrmnlDeviceResponse, Unit>
}
