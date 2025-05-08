package ink.trmnl.android.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.model.TrmnlDeviceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TrmnlDeviceConfigDataStoreTest {
    private lateinit var context: Context
    private lateinit var moshi: Moshi
    private lateinit var deviceConfigDataStore: TrmnlDeviceConfigDataStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        moshi = Moshi.Builder().build()
        deviceConfigDataStore = TrmnlDeviceConfigDataStore(context, moshi)
    }

    @After
    fun tearDown() =
        runTest {
            // Clean up the data store after each test
            deviceConfigDataStore.clearAll()
        }

    @Test
    fun `deviceTypeFlow returns null when not saved`() =
        runTest {
            // Act
            val deviceType = deviceConfigDataStore.deviceTypeFlow.first()

            // Assert
            assertThat(deviceType).isNull()
        }

    @Test
    fun `deviceTypeFlow returns correct type when saved`() =
        runTest {
            // Arrange
            deviceConfigDataStore.saveDeviceType(TrmnlDeviceType.BYOD)

            // Act
            val deviceType = deviceConfigDataStore.deviceTypeFlow.first()

            // Assert
            assertThat(deviceType).isEqualTo(TrmnlDeviceType.BYOD)
        }

    @Test
    fun `accessTokenFlow returns null when not saved`() =
        runTest {
            // Act
            val token = deviceConfigDataStore.accessTokenFlow.first()

            // Assert
            assertThat(token).isNull()
        }

    @Test
    fun `accessTokenFlow returns correct token when saved`() =
        runTest {
            // Arrange
            val expectedToken = "test-token-123"
            deviceConfigDataStore.saveAccessToken(expectedToken)

            // Act
            val token = deviceConfigDataStore.accessTokenFlow.first()

            // Assert
            assertThat(token).isEqualTo(expectedToken)
        }

    @Test
    fun `serverUrlFlow returns default when not saved`() =
        runTest {
            // Act
            val url = deviceConfigDataStore.serverUrlFlow.first()

            // Assert
            assertThat(url).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)
        }

    @Test
    fun `serverUrlFlow returns custom URL when saved`() =
        runTest {
            // Arrange
            val customUrl = "https://custom.trmnl.app"
            deviceConfigDataStore.saveServerUrl(customUrl)

            // Act
            val url = deviceConfigDataStore.serverUrlFlow.first()

            // Assert
            assertThat(url).isEqualTo(customUrl)
        }

    @Test
    fun `refreshRateSecondsFlow returns null when not saved`() =
        runTest {
            // Act
            val rate = deviceConfigDataStore.refreshRateSecondsFlow.first()

            // Assert
            assertThat(rate).isNull()
        }

    @Test
    fun `refreshRateSecondsFlow returns correct rate when saved`() =
        runTest {
            // Arrange
            val expectedRate = 300L
            deviceConfigDataStore.saveRefreshRateSeconds(expectedRate)

            // Act
            val rate = deviceConfigDataStore.refreshRateSecondsFlow.first()

            // Assert
            assertThat(rate).isEqualTo(expectedRate)
        }

    @Test
    fun `deviceConfigFlow returns null when not saved`() =
        runTest {
            // Act
            val config = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert
            assertThat(config).isNull()
        }

    @Test
    fun `deviceConfigFlow returns correctly deserialized config when saved`() =
        runTest {
            // Arrange - Create and save a device config
            val expectedConfig =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.TRMNL,
                    apiAccessToken = "test-token",
                    apiBaseUrl = AppConfig.TRMNL_API_SERVER_BASE_URL,
                    refreshRateSecs = 60,
                )
            deviceConfigDataStore.saveDeviceConfig(expectedConfig)

            // Act
            val config = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert
            assertThat(config).isEqualTo(expectedConfig)
        }

    @Test
    fun `shouldUpdateRefreshRate returns true when rates differ`() =
        runTest {
            // Arrange
            deviceConfigDataStore.saveRefreshRateSeconds(60L)

            // Act
            val shouldUpdate = deviceConfigDataStore.shouldUpdateRefreshRate(120L)

            // Assert
            assertThat(shouldUpdate).isTrue()
        }

    @Test
    fun `shouldUpdateRefreshRate returns false when rates are identical`() =
        runTest {
            // Arrange
            val rate = 60L
            deviceConfigDataStore.saveRefreshRateSeconds(rate)

            // Act
            val shouldUpdate = deviceConfigDataStore.shouldUpdateRefreshRate(rate)

            // Assert
            assertThat(shouldUpdate).isFalse()
        }

    @Test
    fun `shouldUpdateRefreshRate returns false when current rate is missing`() =
        runTest {
            // Act - No current rate saved
            val shouldUpdate = deviceConfigDataStore.shouldUpdateRefreshRate(60L)

            // Assert
            assertThat(shouldUpdate).isFalse()
        }

    @Test
    fun `isValidServerUrl returns true for valid URLs`() =
        runTest {
            // Act & Assert
            assertThat(deviceConfigDataStore.isValidServerUrl("https://api.example.com")).isTrue()
            assertThat(deviceConfigDataStore.isValidServerUrl("http://192.168.1.100:8080")).isTrue()
            assertThat(deviceConfigDataStore.isValidServerUrl("https://trmnl.io/api")).isTrue()
        }

    @Test
    fun `isValidServerUrl returns false for invalid URLs`() =
        runTest {
            // Act & Assert
            assertThat(deviceConfigDataStore.isValidServerUrl("not-a-url")).isFalse()
            assertThat(deviceConfigDataStore.isValidServerUrl("ftp://example.com")).isFalse()
            assertThat(deviceConfigDataStore.isValidServerUrl("")).isFalse()
            assertThat(deviceConfigDataStore.isValidServerUrl("https://")).isFalse()
        }

    @Test
    fun `hasTokenFlow returns true when token exists`() =
        runTest {
            // Arrange
            deviceConfigDataStore.saveAccessToken("test-token")

            // Act
            val hasToken = deviceConfigDataStore.hasTokenFlow.first()

            // Assert
            assertThat(hasToken).isTrue()
        }

    @Test
    fun `hasTokenFlow returns false when token is empty`() =
        runTest {
            // Arrange
            deviceConfigDataStore.saveAccessToken("")

            // Act
            val hasToken = deviceConfigDataStore.hasTokenFlow.first()

            // Assert
            assertThat(hasToken).isFalse()
        }

    @Test
    fun `hasTokenSync returns true when token exists`() =
        runTest {
            // Arrange
            deviceConfigDataStore.saveAccessToken("test-token")

            // Act
            val hasToken = deviceConfigDataStore.hasTokenSync()

            // Assert
            assertThat(hasToken).isTrue()
        }

    @Test
    fun `getDeviceConfigSync returns null when no config saved`() =
        runTest {
            // Act
            val config = deviceConfigDataStore.getDeviceConfigSync()

            // Assert
            assertThat(config).isNull()
        }

    @Test
    fun `getDeviceConfigSync returns config when saved`() =
        runTest {
            // Arrange
            val expectedConfig =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOS,
                    apiAccessToken = "test-token",
                    apiBaseUrl = "https://custom.server.com",
                    refreshRateSecs = 120,
                )
            deviceConfigDataStore.saveDeviceConfig(expectedConfig)

            // Act
            val config = deviceConfigDataStore.getDeviceConfigSync()

            // Assert
            assertThat(config).isEqualTo(expectedConfig)
        }

    @Test
    fun `clearAll removes all saved preferences`() =
        runTest {
            // Arrange - Save various settings
            deviceConfigDataStore.saveDeviceType(TrmnlDeviceType.BYOD)
            deviceConfigDataStore.saveAccessToken("test-token")
            deviceConfigDataStore.saveServerUrl("https://custom.url")
            deviceConfigDataStore.saveRefreshRateSeconds(300L)

            // Verify settings were saved
            assertThat(deviceConfigDataStore.deviceTypeFlow.first()).isEqualTo(TrmnlDeviceType.BYOD)

            // Act
            deviceConfigDataStore.clearAll()

            // Assert
            assertThat(deviceConfigDataStore.deviceTypeFlow.first()).isNull()
            assertThat(deviceConfigDataStore.accessTokenFlow.first()).isNull()
            assertThat(deviceConfigDataStore.serverUrlFlow.first()).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)
            assertThat(deviceConfigDataStore.refreshRateSecondsFlow.first()).isNull()
            assertThat(deviceConfigDataStore.deviceConfigFlow.first()).isNull()
        }

    @Test
    fun `saveDeviceConfig updates individual fields as well`() =
        runTest {
            // Arrange
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOS,
                    apiAccessToken = "test-token",
                    apiBaseUrl = "https://custom.server.com",
                    refreshRateSecs = 120,
                )

            // Act
            deviceConfigDataStore.saveDeviceConfig(config)

            // Assert - Check both the config and individual fields
            assertThat(deviceConfigDataStore.deviceConfigFlow.first()).isEqualTo(config)
            assertThat(deviceConfigDataStore.deviceTypeFlow.first()).isEqualTo(TrmnlDeviceType.BYOS)
            assertThat(deviceConfigDataStore.accessTokenFlow.first()).isEqualTo("test-token")
            assertThat(deviceConfigDataStore.serverUrlFlow.first()).isEqualTo("https://custom.server.com")
            assertThat(deviceConfigDataStore.refreshRateSecondsFlow.first()).isEqualTo(120L)
        }
}
