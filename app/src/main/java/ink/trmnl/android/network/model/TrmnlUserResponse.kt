package ink.trmnl.android.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response wrapper for the TRMNL /api/me endpoint.
 *
 * This response provides information about the authenticated user.
 *
 * See: https://trmnl.com/api-docs/index.html#/Users/get_api_me
 *
 * @property data The user data
 */
@JsonClass(generateAdapter = true)
data class TrmnlUserResponse(
    @Json(name = "data")
    val data: TrmnlUser,
)

/**
 * Represents a TRMNL user's information.
 *
 * Contains details about the authenticated user including their profile information,
 * timezone settings, and API key.
 *
 * @property id The unique identifier for the user
 * @property name The user's full name
 * @property email The user's email address
 * @property firstName The user's first name
 * @property lastName The user's last name
 * @property locale The user's locale (e.g., "en")
 * @property timeZone The user's timezone in human-readable format (e.g., "Eastern Time (US & Canada)")
 * @property timeZoneIana The user's timezone in IANA format (e.g., "America/New_York")
 * @property utcOffset The user's UTC offset in seconds
 * @property apiKey The user's API key
 */
@JsonClass(generateAdapter = true)
data class TrmnlUser(
    @Json(name = "id")
    val id: Int,
    @Json(name = "name")
    val name: String,
    @Json(name = "email")
    val email: String,
    @Json(name = "first_name")
    val firstName: String,
    @Json(name = "last_name")
    val lastName: String,
    @Json(name = "locale")
    val locale: String,
    @Json(name = "time_zone")
    val timeZone: String,
    @Json(name = "time_zone_iana")
    val timeZoneIana: String,
    @Json(name = "utc_offset")
    val utcOffset: Int,
    @Json(name = "api_key")
    val apiKey: String,
)
