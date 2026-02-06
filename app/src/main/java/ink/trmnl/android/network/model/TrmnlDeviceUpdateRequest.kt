package ink.trmnl.android.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class representing a request to update a TRMNL device.
 *
 * **DEPRECATED:** This model is no longer needed for battery reporting.
 * Battery percentage is now sent via the Percent-Charged header in /api/display call.
 *
 * This class is kept for backward compatibility and may be removed in a future version.
 *
 * All fields are optional - only include the fields you want to update.
 *
 * Sample JSON request:
 * ```json
 * {
 *   "sleep_mode_enabled": true,
 *   "sleep_start_time": 1320,
 *   "sleep_end_time": 480,
 *   "percent_charged": 69.0
 * }
 * ```
 *
 * @property sleepModeEnabled Whether sleep mode is enabled.
 * @property sleepStartTime The time when sleep mode starts (minutes from midnight).
 * @property sleepEndTime The time when sleep mode ends (minutes from midnight).
 * @property percentCharged The battery percentage charged.
 * @see ink.trmnl.android.network.TrmnlApiService.updateDevice
 */
@Deprecated(
    message = "No longer needed for battery reporting. Battery is now sent via Percent-Charged header.",
    level = DeprecationLevel.WARNING,
)
@JsonClass(generateAdapter = true)
data class TrmnlDeviceUpdateRequest(
    @Json(name = "sleep_mode_enabled") val sleepModeEnabled: Boolean? = null,
    @Json(name = "sleep_start_time") val sleepStartTime: Int? = null,
    @Json(name = "sleep_end_time") val sleepEndTime: Int? = null,
    @Json(name = "percent_charged") val percentCharged: Double? = null,
)
