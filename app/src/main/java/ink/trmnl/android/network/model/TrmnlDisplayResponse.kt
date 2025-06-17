package ink.trmnl.android.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import ink.trmnl.android.util.HTTP_NONE

/**
 * Data class representing the response from the TRMNL API for display data.
 *
 * Sample success response:
 * ```json
 * {
 *     "status": 0,
 *     "image_url": "https://trmnl.s3.us-east-2.amazonaws.com/...0f",
 *     "filename": "plugin-2025-....4640540",
 *     "refresh_rate": 3595,
 *     "reset_firmware": false,
 *     "update_firmware": true,
 *     "firmware_url": "https://trmnl-fw.s3.us-east-2.amazonaws.com/FW1.4.8.bin",
 *     "special_function": "restart_playlist"
 * }
 * ```
 *
 * Sample response when device is sleeping:
 * ```json
 * {
 *   "status": 0,
 *   "image_url": "https://usetrmnl.com/images/setup/sleep.bmp",
 *   "filename": "sleep",
 *   "refresh_rate": 28498
 * }
 * ```
 *
 * Sample fail response:
 * ```json
 * {"status":500,"error":"Device not found","reset_firmware":true}
 * ```
 *
 * Sample response from BYOS Hanami server:
 * ```json
 * {
 *   "filename": "demo.bmp",
 *   "firmware_url": "http://localhost:2443/assets/firmware/1.4.8.bin",
 *   "image_url": "https://localhost:2443/assets/screens/A1B2C3D4E5F6/demo.bmp",
 *   "image_url_timeout": 0,
 *   "refresh_rate": 130,
 *   "reset_firmware": false,
 *   "special_function": "sleep",
 *   "update_firmware": false
 * }
 * ```
 */
@JsonClass(generateAdapter = true)
data class TrmnlDisplayResponse(
    /**
     * Status code indicating the result of the request.
     * - 0: Success
     * - 500: Error (e.g., device not found)
     *
     * Also on `byos_hanami`, the status code is not provided.
     * See
     * - https://github.com/usetrmnl/byos_hanami?tab=readme-ov-file#apis
     * - https://discord.com/channels/1281055965508141100/1331360842809348106/1383221807716237433
     */
    val status: Int = HTTP_NONE,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "filename") val imageFileName: String?,
    @Json(name = "update_firmware") val updateFirmware: Boolean?,
    @Json(name = "firmware_url") val firmwareUrl: String?,
    @Json(name = "refresh_rate") val refreshRate: Long?,
    @Json(name = "reset_firmware") val resetFirmware: Boolean?,
    val error: String? = null, // Added for error responses
    @Json(name = "special_function") val specialFunction: String? = null,
)
