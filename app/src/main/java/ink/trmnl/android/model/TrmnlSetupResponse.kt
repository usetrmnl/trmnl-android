package ink.trmnl.android.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class representing the response from the TRMNL setup API.
 *
 * Sample JSON response:
 * ```json
 * {
 *     "api_key": "abc1234567890abcdef",
 *     "friendly_id": "GO87665",
 *     "image_url": "https://localhost:1234/assets/setup.bmp",
 *     "message": "Welcome to Terminus!"
 * }
 * ```
 *
 * @property apiKey The API key for the device.
 * @property friendlyId A user-friendly identifier for the device.
 * @property imageUrl The URL of an image to display during setup.
 * @property message A welcome message or setup instructions.
 * @see ink.trmnl.android.network.TrmnlApiService.setupNewDevice
 */
@JsonClass(generateAdapter = true)
data class TrmnlSetupResponse(
    @Json(name = "api_key") val apiKey: String,
    @Json(name = "friendly_id") val friendlyId: String,
    @Json(name = "image_url") val imageUrl: String,
    @Json(name = "message") val message: String,
)
