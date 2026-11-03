package ink.trmnl.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [TrmnlDeviceConfig] and extension functions.
 */
class TrmnlDeviceConfigTest {
    @Test
    fun `hasSameFormContent returns true when only refreshRateSecs differs`() {
        val config1 =
            TrmnlDeviceConfig(
                type = TrmnlDeviceType.TRMNL,
                apiBaseUrl = "https://trmnl.com",
                apiAccessToken = "token-1",
                deviceMacId = "mac-1",
                refreshRateSecs = 300L,
                isMasterDevice = true,
            )
        val config2 = config1.copy(refreshRateSecs = 900L)

        assertThat(config1.hasSameFormContent(config2)).isTrue()
        assertThat(config2.hasSameFormContent(config1)).isTrue()
    }

    @Test
    fun `hasSameFormContent returns false when form fields differ`() {
        val baseConfig =
            TrmnlDeviceConfig(
                type = TrmnlDeviceType.TRMNL,
                apiBaseUrl = "https://trmnl.com",
                apiAccessToken = "token-1",
                deviceMacId = "mac-1",
                refreshRateSecs = 300L,
                isMasterDevice = true,
            )

        assertThat(baseConfig.hasSameFormContent(baseConfig.copy(type = TrmnlDeviceType.BYOD))).isFalse()
        assertThat(baseConfig.hasSameFormContent(baseConfig.copy(apiBaseUrl = "https://other.com"))).isFalse()
        assertThat(baseConfig.hasSameFormContent(baseConfig.copy(apiAccessToken = "token-2"))).isFalse()
        assertThat(baseConfig.hasSameFormContent(baseConfig.copy(deviceMacId = "mac-2"))).isFalse()
        assertThat(baseConfig.hasSameFormContent(baseConfig.copy(isMasterDevice = false))).isFalse()
    }

    @Test
    fun `hasSameFormContent handles null values correctly`() {
        val config =
            TrmnlDeviceConfig(
                type = TrmnlDeviceType.TRMNL,
                apiBaseUrl = "https://trmnl.com",
                apiAccessToken = "token-1",
            )
        val nullConfig: TrmnlDeviceConfig? = null

        assertThat(nullConfig.hasSameFormContent(null)).isTrue()
        assertThat(config.hasSameFormContent(null)).isFalse()
        assertThat(nullConfig.hasSameFormContent(config)).isFalse()
    }
}
