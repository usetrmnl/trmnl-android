package ink.trmnl.android.util

import ink.trmnl.android.data.TrmnlDisplayInfo

/**
 * 500 Internal Server Error - A generic error message, given when an unexpected
 * condition was encountered and no more specific message is suitable.
 */
internal const val HTTP_500 = 500

/**
 * 200 OK - The request has succeeded.
 */
internal const val HTTP_200 = 200

/**
 * 429 Too Many Requests - The user has sent too many requests in a given amount of time ("rate limiting").
 */
internal const val HTTP_429 = 429

/**
 * TRMNL server currently returns `0` for success for some APIs.
 */
internal const val HTTP_OK = 0

/**
 * -1 - Indicates no HTTP status code was received in JSON response.
 * This is the case for the BYOS Hanami server, which does not return a status code.
 * - https://discord.com/channels/1281055965508141100/1331360842809348106/1383221807716237433
 */
internal const val HTTP_NONE = -1

/**
 * Extension function to check if the HTTP status code is OK based on TRMNL server responses.
 */
internal fun Int?.isHttpOk(): Boolean = this == HTTP_OK || this == HTTP_200 || this == HTTP_NONE

/**
 * Extension function to check if the HTTP status code is an error.
 */
internal fun Int?.isHttpError(): Boolean = this == HTTP_500 || this == null

/**
 * Extension function to check if the HTTP status code indicates rate limiting (429).
 * These errors should trigger a retry with exponential backoff.
 */
internal fun Int?.isRateLimitError(): Boolean = this == HTTP_429

/**
 * Special error code provided in the [TrmnlDisplayInfo.imageFileName] as hack to indicate that the device requires setup.
 *
 * See following for additional context:
 * - https://discord.com/channels/1281055965508141100/1331360842809348106/1384605617456545904
 * - https://github.com/usetrmnl/trmnl-android/issues/83
 */
internal const val ERROR_TYPE_DEVICE_SETUP_REQUIRED = "device_requires_setup"
