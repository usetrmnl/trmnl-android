package ink.trmnl.android.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for input validation and formatting functions.
 */
class InputValidatorTest {
    @Test
    fun `isValidUrl should return true for valid HTTPS URLs`() {
        val validUrls =
            listOf(
                "https://localhost:2443",
                "https://example.com",
                "https://example.com/path",
                "https://example.com/path?query=value",
                "https://example.com:8080",
                "https://subdomain.example.com",
                "https://example-domain.com",
                "https://example.com/path/to/resource",
                "https://example.com/path-with-dash",
                "https://example.com/path_with_underscore",
            )

        validUrls.forEach { url ->
            assertThat(isValidUrl(url)).isTrue() // URL should be valid: $url
        }
    }

    @Test
    fun `isValidUrl should return false for invalid URLs and HTTP URLs`() {
        val invalidUrls =
            listOf(
                "",
                "example.com",
                "www.example.com",
                "http://example.com", // HTTP is now invalid - HTTPS only
                "http://subdomain.example.com", // HTTP is now invalid - HTTPS only
                "http://example.com/path", // HTTP is now invalid - HTTPS only
                "ftp://example.com",
                "file:///path/to/file",
                "http:/example.com",
                "http://",
                "https://",
                "http:example.com",
            )

        invalidUrls.forEach { url ->
            assertThat(isValidUrl(url)).isFalse() // URL should be invalid: $url
        }
    }

    @Test
    fun `isValidMacAddress should return true for valid MAC addresses`() {
        val validMacAddresses =
            listOf(
                // Colon-separated format
                "00:11:22:33:44:55",
                "AA:BB:CC:DD:EE:FF",
                "aa:bb:cc:dd:ee:ff",
                "Aa:Bb:Cc:Dd:Ee:Ff",
                "01:23:45:67:89:aB",
                // Hyphen-separated format
                "00-11-22-33-44-55",
                "AA-BB-CC-DD-EE-FF",
                "aa-bb-cc-dd-ee-ff",
                "Aa-Bb-Cc-Dd-Ee-Ff",
                "01-23-45-67-89-aB",
                // No separator format
                "001122334455",
                "AABBCCDDEEFF",
                "aabbccddeeff",
                "AaBbCcDdEeFf",
                "0123456789aB",
                // Empty string (considered valid as optional field)
                "",
            )

        validMacAddresses.forEach { mac ->
            assertThat(isValidMacAddress(mac)).isTrue() // MAC address should be valid: $mac
        }
    }

    @Test
    fun `isValidMacAddress should return false for invalid MAC addresses`() {
        val invalidMacAddresses =
            listOf(
                // Wrong format
                "00:11:22:33:44", // Too short
                "00:11:22:33:44:55:66", // Too long
                "00:11:22:33:44:GG", // Invalid hex (GG)
                "00:11:22:33:44:", // Missing last octet
                "00-11-22-33-44", // Too short
                "00-11-22-33-44-55-66", // Too long
                "00-11-22-33-44-GG", // Invalid hex (GG)
                "00112233445", // Too short
                "0011223344556", // Too long
                "00112233445G", // Invalid hex (G)
                // Mixed separators
                "00:11-22:33-44:55",
                // Invalid characters
                "00:11:22:33:44:ZZ",
                "00-11-22-33-44-ZZ",
                "00112233445Z",
                // Wrong separator positions
                "0:01:12:23:34:45",
                "001:122:334:455",
                // Invalid formats
                "00.11.22.33.44.55",
                "00/11/22/33/44/55",
                "00 11 22 33 44 55",
            )

        invalidMacAddresses.forEach { mac ->
            assertThat(isValidMacAddress(mac)).isFalse() // MAC address should be invalid: $mac
        }
    }

    @Test
    fun `normalizeMacAddress should return standardized MAC address format`() {
        // Format: input, expected output
        val testCases =
            mapOf(
                // Already standard format cases (uppercase)
                "00:11:22:33:44:55" to "00:11:22:33:44:55",
                "AA:BB:CC:DD:EE:FF" to "AA:BB:CC:DD:EE:FF",
                // Lowercase to uppercase conversion
                "aa:bb:cc:dd:ee:ff" to "AA:BB:CC:DD:EE:FF",
                "1a:2b:3c:4d:5e:6f" to "1A:2B:3C:4D:5E:6F",
                // Hyphen format conversion
                "00-11-22-33-44-55" to "00:11:22:33:44:55",
                "AA-BB-CC-DD-EE-FF" to "AA:BB:CC:DD:EE:FF",
                "aa-bb-cc-dd-ee-ff" to "AA:BB:CC:DD:EE:FF",
                // No separator format conversion
                "001122334455" to "00:11:22:33:44:55",
                "AABBCCDDEEFF" to "AA:BB:CC:DD:EE:FF",
                "aabbccddeeff" to "AA:BB:CC:DD:EE:FF",
                "1a2b3c4d5e6f" to "1A:2B:3C:4D:5E:6F",
                // Whitespace trimming
                " 00:11:22:33:44:55 " to "00:11:22:33:44:55",
                " 00-11-22-33-44-55 " to "00:11:22:33:44:55",
                " 001122334455 " to "00:11:22:33:44:55",
            )

        testCases.forEach { (input, expected) ->
            // For each test case, verify that normalizing the input produces the expected output
            assertThat(normalizeMacAddress(input)).isEqualTo(expected)
        }
    }

    @Test
    fun `normalizeMacAddress should return null for invalid MAC addresses`() {
        val invalidMacAddresses =
            listOf(
                "",
                "   ",
                "00:11:22:33:44", // Too short
                "00:11:22:33:44:55:66", // Too long
                "00:11:22:33:44:GG", // Invalid hex (GG)
                "00-11-22-33-44", // Too short
                "00-11-22-33-44-55-66", // Too long
                "00-11-22-33-44-GG", // Invalid hex (GG)
            )

        invalidMacAddresses.forEach { mac ->
            // Invalid MAC address should return null: $mac
            assertThat(normalizeMacAddress(mac)).isNull()
        }
    }

    @Test
    fun `normalizeMacAddress should return null for blank input`() {
        assertThat(normalizeMacAddress("")).isNull()
        assertThat(normalizeMacAddress("   ")).isNull()
    }
}
