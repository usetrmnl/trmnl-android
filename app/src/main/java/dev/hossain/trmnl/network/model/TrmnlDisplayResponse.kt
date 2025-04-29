package dev.hossain.trmnl.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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
 */
@JsonClass(generateAdapter = true)
data class TrmnlDisplayResponse(
    val status: Int,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "filename") val imageName: String?,
    @Json(name = "update_firmware") val updateFirmware: Boolean?,
    @Json(name = "firmware_url") val firmwareUrl: String?,
    @Json(name = "refresh_rate") val refreshRate: Long?,
    @Json(name = "reset_firmware") val resetFirmware: Boolean?,
    val error: String? = null, // Added for error responses
    @Json(name = "special_function") val specialFunction: String? = null,
)
