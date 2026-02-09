package ink.trmnl.android.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
        moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
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

    @Test
    fun `deviceConfigFlow includes isMasterDevice when saved for BYOD`() =
        runTest {
            // Arrange - BYOD with master device setting
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOD,
                    apiAccessToken = "test-token",
                    apiBaseUrl = AppConfig.TRMNL_API_SERVER_BASE_URL,
                    refreshRateSecs = 60,
                    isMasterDevice = true,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val savedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert
            assertThat(savedConfig).isEqualTo(config)
            assertThat(savedConfig?.isMasterDevice).isTrue()
        }

    @Test
    fun `deviceConfigFlow handles isMasterDevice false for BYOD slave mode`() =
        runTest {
            // Arrange - BYOD in slave mode
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOD,
                    apiAccessToken = "test-token",
                    apiBaseUrl = AppConfig.TRMNL_API_SERVER_BASE_URL,
                    refreshRateSecs = 60,
                    isMasterDevice = false,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val savedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert
            assertThat(savedConfig).isEqualTo(config)
            assertThat(savedConfig?.isMasterDevice).isFalse()
        }

    @Test
    fun `deviceConfigFlow handles null isMasterDevice for TRMNL device`() =
        runTest {
            // Arrange - TRMNL device (isMasterDevice not applicable)
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.TRMNL,
                    apiAccessToken = "test-token",
                    apiBaseUrl = AppConfig.TRMNL_API_SERVER_BASE_URL,
                    refreshRateSecs = 60,
                    isMasterDevice = null,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val savedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert
            assertThat(savedConfig).isEqualTo(config)
            assertThat(savedConfig?.isMasterDevice).isNull()
        }

    @Test
    fun `deviceConfigFlow handles null isMasterDevice for BYOS device`() =
        runTest {
            // Arrange - BYOS device (isMasterDevice not applicable)
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOS,
                    apiAccessToken = "test-token",
                    apiBaseUrl = "https://custom.server.com",
                    refreshRateSecs = 120,
                    isMasterDevice = null,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val savedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert
            assertThat(savedConfig).isEqualTo(config)
            assertThat(savedConfig?.isMasterDevice).isNull()
        }

    @Test
    fun `backward compatibility - legacy config without isMasterDevice loads correctly`() =
        runTest {
            // Arrange - Save config the old way (without isMasterDevice)
            deviceConfigDataStore.saveDeviceType(TrmnlDeviceType.BYOD)
            deviceConfigDataStore.saveAccessToken("test-token")
            deviceConfigDataStore.saveServerUrl(AppConfig.TRMNL_API_SERVER_BASE_URL)
            deviceConfigDataStore.saveRefreshRateSeconds(60L)

            // Act - Load config (should trigger legacy migration path)
            val config = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - Config should be loaded with isMasterDevice as null
            assertThat(config).isNotNull()
            assertThat(config?.type).isEqualTo(TrmnlDeviceType.BYOD)
            assertThat(config?.apiAccessToken).isEqualTo("test-token")
            assertThat(config?.isMasterDevice).isNull()
        }

    @Test
    fun `updating BYOD config from master to slave mode persists correctly`() =
        runTest {
            // Arrange - Start with BYOD in master mode
            val masterConfig =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOD,
                    apiAccessToken = "test-token",
                    apiBaseUrl = AppConfig.TRMNL_API_SERVER_BASE_URL,
                    refreshRateSecs = 60,
                    isMasterDevice = true,
                )
            deviceConfigDataStore.saveDeviceConfig(masterConfig)

            // Act - Update to slave mode
            val slaveConfig = masterConfig.copy(isMasterDevice = false)
            deviceConfigDataStore.saveDeviceConfig(slaveConfig)

            // Assert
            val savedConfig = deviceConfigDataStore.deviceConfigFlow.first()
            assertThat(savedConfig?.isMasterDevice).isFalse()
        }

    @Test
    fun `deviceModelPreferencesFlow returns empty map when not saved`() =
        runTest {
            // Act
            val preferences = deviceConfigDataStore.deviceModelPreferencesFlow.first()

            // Assert
            assertThat(preferences).isEmpty()
        }

    @Test
    fun `saveDeviceModelForType stores model name for device type`() =
        runTest {
            // Arrange
            val deviceType = TrmnlDeviceType.BYOD
            val modelName = "amazon_kindle_2024"
            val modelLabel = "Amazon Kindle 2024"

            // Act
            deviceConfigDataStore.saveDeviceModelForType(deviceType, modelName, modelLabel)

            // Assert
            val preferences = deviceConfigDataStore.deviceModelPreferencesFlow.first()
            assertThat(preferences).containsKey("BYOD")
            assertThat(preferences["BYOD"]?.name).isEqualTo("amazon_kindle_2024")
            assertThat(preferences["BYOD"]?.label).isEqualTo("Amazon Kindle 2024")
        }

    @Test
    fun `saveDeviceModelForType updates existing model for device type`() =
        runTest {
            // Arrange - Save initial model
            deviceConfigDataStore.saveDeviceModelForType(
                TrmnlDeviceType.BYOD,
                "amazon_kindle_2024",
                "Amazon Kindle 2024",
            )

            // Act - Update to different model
            deviceConfigDataStore.saveDeviceModelForType(
                TrmnlDeviceType.BYOD,
                "boox_tab_ultra_c_pro",
                "Boox Tab Ultra C Pro",
            )

            // Assert
            val preferences = deviceConfigDataStore.deviceModelPreferencesFlow.first()
            assertThat(preferences["BYOD"]?.name).isEqualTo("boox_tab_ultra_c_pro")
            assertThat(preferences["BYOD"]?.label).isEqualTo("Boox Tab Ultra C Pro")
            assertThat(preferences).hasSize(1)
        }

    @Test
    fun `saveDeviceModelForType stores multiple device types independently`() =
        runTest {
            // Act - Save models for different device types
            deviceConfigDataStore.saveDeviceModelForType(
                TrmnlDeviceType.BYOD,
                "amazon_kindle_2024",
                "Amazon Kindle 2024",
            )
            deviceConfigDataStore.saveDeviceModelForType(
                TrmnlDeviceType.BYOS,
                "boox_tab_ultra_c_pro",
                "Boox Tab Ultra C Pro",
            )

            // Assert
            val preferences = deviceConfigDataStore.deviceModelPreferencesFlow.first()
            assertThat(preferences["BYOD"]?.name).isEqualTo("amazon_kindle_2024")
            assertThat(preferences["BYOD"]?.label).isEqualTo("Amazon Kindle 2024")
            assertThat(preferences["BYOS"]?.name).isEqualTo("boox_tab_ultra_c_pro")
            assertThat(preferences["BYOS"]?.label).isEqualTo("Boox Tab Ultra C Pro")
            assertThat(preferences).hasSize(2)
        }

    @Test
    fun `getDeviceModelForType returns null when no model saved`() =
        runTest {
            // Act
            val modelSelection = deviceConfigDataStore.getDeviceModelForType(TrmnlDeviceType.BYOD)

            // Assert
            assertThat(modelSelection).isNull()
        }

    @Test
    fun `getDeviceModelForType returns correct model selection when saved`() =
        runTest {
            // Arrange
            val deviceType = TrmnlDeviceType.BYOD
            val expectedModelName = "amazon_kindle_2024"
            val expectedModelLabel = "Amazon Kindle 2024"
            deviceConfigDataStore.saveDeviceModelForType(deviceType, expectedModelName, expectedModelLabel)

            // Act
            val modelSelection = deviceConfigDataStore.getDeviceModelForType(deviceType)

            // Assert
            assertThat(modelSelection).isNotNull()
            assertThat(modelSelection?.name).isEqualTo(expectedModelName)
            assertThat(modelSelection?.label).isEqualTo(expectedModelLabel)
        }

    @Test
    fun `getDeviceModelForType returns null for device type without saved model`() =
        runTest {
            // Arrange - Save model for BYOD only
            deviceConfigDataStore.saveDeviceModelForType(
                TrmnlDeviceType.BYOD,
                "amazon_kindle_2024",
                "Amazon Kindle 2024",
            )

            // Act - Query for BYOS which has no saved model
            val modelSelection = deviceConfigDataStore.getDeviceModelForType(TrmnlDeviceType.BYOS)

            // Assert
            assertThat(modelSelection).isNull()
        }

    @Test
    fun `deviceModelPreferencesFlow emits updated map when model saved`() =
        runTest {
            // Arrange - Start with empty preferences
            val initialPreferences = deviceConfigDataStore.deviceModelPreferencesFlow.first()
            assertThat(initialPreferences).isEmpty()

            // Act - Save a model
            deviceConfigDataStore.saveDeviceModelForType(
                TrmnlDeviceType.BYOD,
                "amazon_kindle_2024",
                "Amazon Kindle 2024",
            )

            // Assert - Flow emits updated map
            val updatedPreferences = deviceConfigDataStore.deviceModelPreferencesFlow.first()
            assertThat(updatedPreferences["BYOD"]?.name).isEqualTo("amazon_kindle_2024")
            assertThat(updatedPreferences["BYOD"]?.label).isEqualTo("Amazon Kindle 2024")
        }

    @Test
    fun `clearAll removes device model preferences`() =
        runTest {
            // Arrange - Save some device model preferences
            deviceConfigDataStore.saveDeviceModelForType(
                TrmnlDeviceType.BYOD,
                "amazon_kindle_2024",
                "Amazon Kindle 2024",
            )
            deviceConfigDataStore.saveDeviceModelForType(
                TrmnlDeviceType.BYOS,
                "boox_tab_ultra_c_pro",
                "Boox Tab Ultra C Pro",
            )

            // Act
            deviceConfigDataStore.clearAll()

            // Assert
            val preferences = deviceConfigDataStore.deviceModelPreferencesFlow.first()
            assertThat(preferences).isEmpty()
        }

    // ============================================================================
    // Domain Migration Tests (${AppConfig.LEGACY_TRMNL_DOMAIN} → ${AppConfig.TRMNL_DOMAIN})
    // See: https://github.com/usetrmnl/trmnl-android/issues/240
    // ============================================================================

    @Test
    fun `deviceConfigFlow migrates usetrmnl_com to trmnl_com for TRMNL device with JSON config`() =
        runTest {
            // Arrange - Save config with old ${AppConfig.LEGACY_TRMNL_DOMAIN} URL (JSON storage)
            val oldConfig =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.TRMNL,
                    apiAccessToken = "test-token",
                    apiBaseUrl = "https://${AppConfig.LEGACY_TRMNL_DOMAIN}/",
                    refreshRateSecs = 600,
                )
            deviceConfigDataStore.saveDeviceConfig(oldConfig)

            // Act - Read config (should trigger migration)
            val migratedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - URL should be migrated to ${AppConfig.TRMNL_DOMAIN}
            assertThat(migratedConfig).isNotNull()
            assertThat(migratedConfig?.type).isEqualTo(TrmnlDeviceType.TRMNL)
            assertThat(migratedConfig?.apiBaseUrl).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)
            assertThat(migratedConfig?.apiAccessToken).isEqualTo("test-token")
            assertThat(migratedConfig?.refreshRateSecs).isEqualTo(600)

            // Verify migrated config was saved back to DataStore
            val savedConfig = deviceConfigDataStore.deviceConfigFlow.first()
            assertThat(savedConfig?.apiBaseUrl).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)
        }

    @Test
    fun `deviceConfigFlow migrates usetrmnl_com to trmnl_com for TRMNL device with legacy config`() =
        runTest {
            // Arrange - Save config using legacy individual fields
            deviceConfigDataStore.saveDeviceType(TrmnlDeviceType.TRMNL)
            deviceConfigDataStore.saveAccessToken("test-token")
            deviceConfigDataStore.saveServerUrl("https://${AppConfig.LEGACY_TRMNL_DOMAIN}/")
            deviceConfigDataStore.saveRefreshRateSeconds(600L)

            // Act - Read config (should trigger migration)
            val migratedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - URL should be migrated to ${AppConfig.TRMNL_DOMAIN}
            assertThat(migratedConfig).isNotNull()
            assertThat(migratedConfig?.type).isEqualTo(TrmnlDeviceType.TRMNL)
            assertThat(migratedConfig?.apiBaseUrl).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)
            assertThat(migratedConfig?.apiAccessToken).isEqualTo("test-token")
        }

    @Test
    fun `deviceConfigFlow handles case-insensitive domain migration - uppercase`() =
        runTest {
            // Arrange - URL with uppercase USETRMNL.COM
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.TRMNL,
                    apiAccessToken = "test-token",
                    apiBaseUrl = "https://USETRMNL.COM/api",
                    refreshRateSecs = 600,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val migratedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - Should migrate regardless of case (replacement is always lowercase)
            assertThat(migratedConfig?.apiBaseUrl).isEqualTo("https://${AppConfig.TRMNL_DOMAIN}/api")
        }

    @Test
    fun `deviceConfigFlow handles case-insensitive domain migration - mixed case`() =
        runTest {
            // Arrange - URL with mixed case UseTrmnL.com
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.TRMNL,
                    apiAccessToken = "test-token",
                    apiBaseUrl = "https://UseTrmnL.com/",
                    refreshRateSecs = 600,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val migratedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - Should migrate with normalized lowercase domain
            assertThat(migratedConfig?.apiBaseUrl).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)
        }

    @Test
    fun `deviceConfigFlow does NOT migrate BYOS device with custom usetrmnl_com URL`() =
        runTest {
            // Arrange - BYOS device with custom URL that happens to contain ${AppConfig.LEGACY_TRMNL_DOMAIN}
            val customUrl = "https://${AppConfig.LEGACY_TRMNL_DOMAIN}.myserver.io/"
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOS,
                    apiAccessToken = "test-token",
                    apiBaseUrl = customUrl,
                    refreshRateSecs = 600,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val loadedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - BYOS URLs should NOT be migrated (preserve custom URLs)
            assertThat(loadedConfig?.apiBaseUrl).isEqualTo(customUrl)
        }

    @Test
    fun `deviceConfigFlow does NOT migrate BYOD device with usetrmnl_com URL`() =
        runTest {
            // Arrange - BYOD device (should use official TRMNL server, but no migration for BYOD type)
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOD,
                    apiAccessToken = "test-token",
                    apiBaseUrl = "https://${AppConfig.LEGACY_TRMNL_DOMAIN}/",
                    refreshRateSecs = 600,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val loadedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - Only TRMNL device type gets migrated
            assertThat(loadedConfig?.apiBaseUrl).isEqualTo("https://${AppConfig.LEGACY_TRMNL_DOMAIN}/")
        }

    @Test
    fun `deviceConfigFlow does NOT migrate already correct trmnl_com URL`() =
        runTest {
            // Arrange - Already using correct ${AppConfig.TRMNL_DOMAIN}
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.TRMNL,
                    apiAccessToken = "test-token",
                    apiBaseUrl = AppConfig.TRMNL_API_SERVER_BASE_URL,
                    refreshRateSecs = 600,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val loadedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - Should remain unchanged
            assertThat(loadedConfig?.apiBaseUrl).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)
        }

    @Test
    fun `deviceConfigFlow migrates URL without trailing slash`() =
        runTest {
            // Arrange
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.TRMNL,
                    apiAccessToken = "test-token",
                    apiBaseUrl = "https://${AppConfig.LEGACY_TRMNL_DOMAIN}",
                    refreshRateSecs = 600,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val migratedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert
            assertThat(migratedConfig?.apiBaseUrl).isEqualTo("https://${AppConfig.TRMNL_DOMAIN}")
        }

    @Test
    fun `deviceConfigFlow migrates URL with path after domain`() =
        runTest {
            // Arrange - URL with path
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.TRMNL,
                    apiAccessToken = "test-token",
                    apiBaseUrl = "https://${AppConfig.LEGACY_TRMNL_DOMAIN}/api/v1",
                    refreshRateSecs = 600,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val migratedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - Path should be preserved
            assertThat(migratedConfig?.apiBaseUrl).isEqualTo("https://${AppConfig.TRMNL_DOMAIN}/api/v1")
        }

    @Test
    fun `deviceConfigFlow migration is idempotent - no repeated saves`() =
        runTest {
            // Arrange
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.TRMNL,
                    apiAccessToken = "test-token",
                    apiBaseUrl = "https://${AppConfig.LEGACY_TRMNL_DOMAIN}/",
                    refreshRateSecs = 600,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act - Read config multiple times
            val firstRead = deviceConfigDataStore.deviceConfigFlow.first()
            val secondRead = deviceConfigDataStore.deviceConfigFlow.first()
            val thirdRead = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - All reads should return migrated URL
            assertThat(firstRead?.apiBaseUrl).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)
            assertThat(secondRead?.apiBaseUrl).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)
            assertThat(thirdRead?.apiBaseUrl).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)

            // Verify config is now persistently migrated
            deviceConfigDataStore.clearAll()
            val config2 =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.TRMNL,
                    apiAccessToken = "test-token",
                    apiBaseUrl = AppConfig.TRMNL_API_SERVER_BASE_URL,
                    refreshRateSecs = 600,
                )
            deviceConfigDataStore.saveDeviceConfig(config2)
            val afterReSave = deviceConfigDataStore.deviceConfigFlow.first()
            assertThat(afterReSave?.apiBaseUrl).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)
        }

    @Test
    fun `deviceConfigFlow preserves other config fields during migration`() =
        runTest {
            // Arrange - Config with all fields populated
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.TRMNL,
                    apiAccessToken = "test-token-12345",
                    apiBaseUrl = "https://${AppConfig.LEGACY_TRMNL_DOMAIN}/",
                    refreshRateSecs = 900,
                    deviceMacId = "aa:bb:cc:dd:ee:ff",
                    isMasterDevice = null,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val migratedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - All fields preserved except URL
            assertThat(migratedConfig?.type).isEqualTo(TrmnlDeviceType.TRMNL)
            assertThat(migratedConfig?.apiAccessToken).isEqualTo("test-token-12345")
            assertThat(migratedConfig?.apiBaseUrl).isEqualTo(AppConfig.TRMNL_API_SERVER_BASE_URL)
            assertThat(migratedConfig?.refreshRateSecs).isEqualTo(900)
            assertThat(migratedConfig?.deviceMacId).isEqualTo("aa:bb:cc:dd:ee:ff")
            assertThat(migratedConfig?.isMasterDevice).isNull()
        }

    @Test
    fun `deviceConfigFlow does NOT migrate BYOS with custom subdomain containing usetrmnl`() =
        runTest {
            // Arrange - BYOS with subdomain that contains "usetrmnl" but is not usetrmnl.com
            val customUrl = "https://api.usetrmnl.mycustomdomain.com/"
            val config =
                TrmnlDeviceConfig(
                    type = TrmnlDeviceType.BYOS,
                    apiAccessToken = "test-token",
                    apiBaseUrl = customUrl,
                    refreshRateSecs = 600,
                )
            deviceConfigDataStore.saveDeviceConfig(config)

            // Act
            val loadedConfig = deviceConfigDataStore.deviceConfigFlow.first()

            // Assert - Custom BYOS URLs are never migrated
            assertThat(loadedConfig?.apiBaseUrl).isEqualTo(customUrl)
        }
}
