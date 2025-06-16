package ink.trmnl.android.util

import java.util.regex.Pattern

/**
 * Validates if the provided string is a valid HTTP or HTTPS URL.
 *
 * The function checks that the URL:
 * - Starts with http:// or https://
 * - Contains valid URL characters in the domain and path components
 *
 * @param url The string to validate as a URL
 * @return true if the string is a valid URL, false otherwise
 */
internal fun isValidUrl(url: String): Boolean {
    val urlRegex =
        Pattern.compile(
            "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            Pattern.CASE_INSENSITIVE,
        )
    return urlRegex.matcher(url).matches()
}

/**
 * Validates MAC address format. Supports formats:
 * - XX:XX:XX:XX:XX:XX
 * - XX-XX-XX-XX-XX-XX
 * - XXXXXXXXXXXX
 * where X is a hexadecimal digit (case insensitive)
 */
internal fun isValidMacAddress(mac: String): Boolean {
    if (mac.isBlank()) return true // Empty is valid (optional field)

    // Standard format with colons XX:XX:XX:XX:XX:XX
    val colonFormatRegex =
        Pattern.compile(
            "^([0-9A-Fa-f]{2}[:]){5}([0-9A-Fa-f]{2})$",
        )

    // Format with hyphens XX-XX-XX-XX-XX-XX
    val hyphenFormatRegex =
        Pattern.compile(
            "^([0-9A-Fa-f]{2}[-]){5}([0-9A-Fa-f]{2})$",
        )

    // No separator XXXXXXXXXXXX
    val noSeparatorRegex =
        Pattern.compile(
            "^([0-9A-Fa-f]{12})$",
        )

    return colonFormatRegex.matcher(mac).matches() ||
        hyphenFormatRegex.matcher(mac).matches() ||
        noSeparatorRegex.matcher(mac).matches()
}
