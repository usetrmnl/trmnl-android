package ink.trmnl.android.util

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
 * TRMNL server currently returns `0` for success for some APIs.
 */
internal const val HTTP_OK = 0

/**
 * Extension function to check if the HTTP status code is OK based on TRMNL server responses.
 */
internal fun Int?.isHttpOk(): Boolean = this == HTTP_OK || this == HTTP_200

/**
 * Extension function to check if the HTTP status code is an error.
 */
internal fun Int?.isHttpError(): Boolean = this == HTTP_500 || this == null
