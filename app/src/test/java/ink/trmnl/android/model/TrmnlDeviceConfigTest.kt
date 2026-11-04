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

    @Test
    fun `shouldAdvancePlaylist returns true for BYOS`() {
        val config =
            TrmnlDeviceConfig(
                type = TrmnlDeviceType.BYOS,
                apiBaseUrl = "https://custom.com",
                apiAccessToken = "token-1",
            )
        assertThat(config.shouldAdvancePlaylist()).isTrue()
    }

    @Test
    fun `shouldAdvancePlaylist returns true for BYOD master or default null and false for slave`() {
        val masterConfig =
            TrmnlDeviceConfig(
                type = TrmnlDeviceType.BYOD,
                apiBaseUrl = "https://trmnl.com",
                apiAccessToken = "token-1",
                isMasterDevice = true,
            )
        val defaultByodConfig = masterConfig.copy(isMasterDevice = null)
        val slaveConfig = masterConfig.copy(isMasterDevice = false)

        assertThat(masterConfig.shouldAdvancePlaylist()).isTrue()
        assertThat(defaultByodConfig.shouldAdvancePlaylist()).isTrue()
        assertThat(slaveConfig.shouldAdvancePlaylist()).isFalse()
    }

    @Test
    fun `shouldAdvancePlaylist returns false for TRMNL and null config`() {
        val trmnlConfig =
            TrmnlDeviceConfig(
                type = TrmnlDeviceType.TRMNL,
                apiBaseUrl = "https://trmnl.com",
                apiAccessToken = "token-1",
            )
        val nullConfig: TrmnlDeviceConfig? = null

        assertThat(trmnlConfig.shouldAdvancePlaylist()).isFalse()
        assertThat(nullConfig.shouldAdvancePlaylist()).isFalse()
    }
}
