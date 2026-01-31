package ink.trmnl.android.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class representing the response from the TRMNL API for the current image.
 *
 * Sample response:
 * ```json
 * {
 *   "status": 200,
 *   "refresh_rate": 3590,
 *   "image_url": "https://trmnl.com/plugin-image.bmp",
 *   "filename": "plugin-image",
 *   "rendered_at": null
 * }
 * ```
 *
 * Sample error response:
 * ```json
 * {
 *   "status": 500,
 *   "error": "Device not found",
 *   "reset_firmware": true
 * }
 * ```
 */
@JsonClass(generateAdapter = true)
data class TrmnlCurrentImageResponse(
    val status: Int,
    @Json(name = "refresh_rate")
    val refreshRateSec: Long?,
    @Json(name = "image_url")
    val imageUrl: String?,
    val filename: String?,
    /**
     * Timestamp when the image was rendered.
     */
    @Json(name = "rendered_at")
    val renderedAt: Long?,
    /**
     * Error message for non 200 status codes.
     */
    val error: String?,
)
