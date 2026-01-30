package ink.trmnl.android.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class representing the response from the TRMNL device API.
 *
 * Sample JSON response:
 * ```json
 * {
 *   "data": {
 *     "id": 1,
 *     "name": "BYOD TRMNL",
 *     "friendly_id": "_____",
 *     "mac_address": "********",
 *     "battery_voltage": null,
 *     "rssi": null,
 *     "sleep_mode_enabled": false,
 *     "sleep_start_time": 1320,
 *     "sleep_end_time": 480,
 *     "percent_charged": 100,
 *     "wifi_strength": 100
 *   }
 * }
 * ```
 *
 * @property data The device data object.
 * @see ink.trmnl.android.network.TrmnlApiService.getDevice
 * @see ink.trmnl.android.network.TrmnlApiService.updateDevice
 */
@JsonClass(generateAdapter = true)
data class TrmnlDeviceResponse(
    @Json(name = "data") val data: TrmnlDevice,
)

/**
 * Data class representing a TRMNL device.
 *
 * @property id The unique identifier for the device (e.g., 123).
 * @property name The name of the device (e.g., "My TRMNL").
 * @property friendlyId A user-friendly identifier for the device (e.g., "ABC-123").
 * @property macAddress The MAC address of the device (e.g., "12:34:56:78:9A:BC").
 * @property batteryVoltage The battery voltage of the device in volts (e.g., 3.7), nullable.
 * @property rssi The received signal strength indicator in dBm (e.g., -70), nullable.
 * @property sleepModeEnabled Whether sleep mode is enabled (e.g., false), nullable in API response.
 * @property sleepStartTime The time when sleep mode starts in minutes from midnight (e.g., 1320 = 10:00 PM), nullable.
 * @property sleepEndTime The time when sleep mode ends in minutes from midnight (e.g., 480 = 8:00 AM), nullable.
 * @property percentCharged The battery percentage charged (e.g., 85.0). Valid range: 0.0 to 100.0.
 * @property wifiStrength The WiFi signal strength percentage (e.g., 75.0). Valid range: 0.0 to 100.0.
 */
@JsonClass(generateAdapter = true)
data class TrmnlDevice(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "friendly_id") val friendlyId: String,
    @Json(name = "mac_address") val macAddress: String,
    @Json(name = "battery_voltage") val batteryVoltage: Double?,
    @Json(name = "rssi") val rssi: Int?,
    @Json(name = "sleep_mode_enabled") val sleepModeEnabled: Boolean?,
    @Json(name = "sleep_start_time") val sleepStartTime: Int?,
    @Json(name = "sleep_end_time") val sleepEndTime: Int?,
    @Json(name = "percent_charged") val percentCharged: Double,
    @Json(name = "wifi_strength") val wifiStrength: Double,
)
