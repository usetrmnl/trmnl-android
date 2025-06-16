package ink.trmnl.android.util

import java.util.regex.Pattern

/**
 * Validates if the provided string is a valid HTTPS URL.
 *
 * The function checks that the URL:
 * - Starts with https:// (HTTP is not accepted for security reasons)
 * - Contains valid URL characters in the domain and path components
 *
 * @param url The string to validate as a URL
 * @return true if the string is a valid HTTPS URL, false otherwise
 */
internal fun isValidUrl(url: String): Boolean {
    val httpsRegex =
        Pattern.compile(
            "^(https)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            Pattern.CASE_INSENSITIVE,
        )
    return httpsRegex.matcher(url).matches()
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

/**
 * Converts any valid MAC address format to standard XX:XX:XX:XX:XX:XX format
 * Supported input formats:
 * - XX:XX:XX:XX:XX:XX (already standard format)
 * - XX-XX-XX-XX-XX-XX (hyphenated)
 * - XXXXXXXXXXXX (no separators)
 *
 * @param mac The MAC address to normalize
 * @return The normalized MAC address in XX:XX:XX:XX:XX:XX format, or null if input is invalid
 */
internal fun normalizeMacAddress(mac: String): String? {
    if (mac.isBlank()) return null // Empty input

    // Clean and uppercase the input
    val cleanMac = mac.trim().uppercase()

    // Return early if already in standard format and valid
    if (Pattern.compile("^([0-9A-F]{2}[:]){5}([0-9A-F]{2})$").matcher(cleanMac).matches()) {
        return cleanMac
    }

    // If mac has hyphens, convert to colons
    if (Pattern.compile("^([0-9A-F]{2}[-]){5}([0-9A-F]{2})$").matcher(cleanMac).matches()) {
        return cleanMac.replace("-", ":")
    }

    // If mac has no separators, add colons
    if (Pattern.compile("^([0-9A-F]{12})$").matcher(cleanMac).matches()) {
        return cleanMac.chunked(2).joinToString(":")
    }

    // If we reach here, the format is invalid
    return null
}
