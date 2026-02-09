package ink.trmnl.android.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.anvil.annotations.optional.SingleIn
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import ink.trmnl.android.data.AppConfig.DEFAULT_REFRESH_INTERVAL_SEC
import ink.trmnl.android.data.AppConfig.TRMNL_API_SERVER_BASE_URL
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.di.ApplicationContext
import ink.trmnl.android.model.DeviceModelSelection
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.model.TrmnlDeviceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

/**
 * DataStore for managing TRMNL device configuration.
 */
private val Context.deviceConfigStore: DataStore<Preferences> by preferencesDataStore(
    name = "trmnl_device_config_store",
)

/**
 * Manages device configuration settings for the TRMNL application.
 *
 * This class provides methods for storing, retrieving, and managing the device configuration
 * including access token, device type, server URL, and refresh rate settings using
 * Android's DataStore.
 *
 * ## Preference Storage Strategy
 *
 * The device configuration uses a **dual-storage approach** for backward compatibility:
 *
 * ### 1. Modern Approach (Primary)
 * - Stores complete `TrmnlDeviceConfig` as JSON in `CONFIG_JSON_KEY`
 * - Single source of truth, easier to maintain
 * - Preferred method for reading and writing device config
 *
 * ### 2. Legacy Approach (Backward Compatibility)
 * - Stores individual fields as separate preference keys:
 *   - `DEVICE_TYPE_KEY` - Device type (TRMNL, BYOS, BYOD)
 *   - `ACCESS_TOKEN_KEY` - Device access token
 *   - `API_BASE_URL_KEY` - Server base URL
 *   - `REFRESH_RATE_SEC_KEY` - Refresh rate in seconds
 *   - `DEVICE_MAC_ID_KEY` - Device MAC address
 *   - `IS_MASTER_DEVICE_KEY` - Master device flag (BYOD only)
 *   - `USER_API_TOKEN_KEY` - User-level API token (BYOD only)
 * - Maintained for users upgrading from older app versions
 *
 * ### Loading Priority
 * When loading config via `deviceConfigFlow`:
 * 1. Check if `CONFIG_JSON_KEY` exists → load from JSON
 * 2. Otherwise → use legacy migration path (build config from individual keys)
 *
 * ### Saving Behavior
 * When saving via `saveDeviceConfig()`:
 * - **Always** saves JSON to `CONFIG_JSON_KEY` (modern)
 * - **Also** saves individual fields (legacy backward compatibility)
 * - This ensures both old and new app versions can read the config
 *
 * ## Device Model Preferences
 *
 * Device model preferences are stored separately from device config:
 * - Key: `DEVICE_MODEL_PREFERENCES_KEY`
 * - Value: JSON map of device type → `DeviceModelSelection`
 * - Example: `{"BYOD": {"name": "amazon_kindle_2024", "label": "Amazon Kindle 2024"}}`
 * - Allows different device models per device type
 *
 * @see TrmnlDeviceConfig for the device configuration data model
 * @see DeviceModelSelection for device model selection data
 */
@SingleIn(AppScope::class)
class TrmnlDeviceConfigDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val moshi: Moshi,
    ) {
        companion object {
            private const val TAG = "DeviceConfigStore"

            private val DEVICE_TYPE_KEY = stringPreferencesKey("device_type")
            private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
            private val API_BASE_URL_KEY = stringPreferencesKey("api_base_url")
            private val REFRESH_RATE_SEC_KEY = longPreferencesKey("refresh_rate_seconds")
            private val CONFIG_JSON_KEY = stringPreferencesKey("config_json")
            private val DEVICE_MAC_ID_KEY = stringPreferencesKey("device_mac_id")
            private val IS_MASTER_DEVICE_KEY = stringPreferencesKey("is_master_device")
            private val DEVICE_MODEL_PREFERENCES_KEY = stringPreferencesKey("device_model_preferences")
        }

        private val deviceTypeAdapter = moshi.adapter(TrmnlDeviceType::class.java)
        private val deviceConfigAdapter = moshi.adapter(TrmnlDeviceConfig::class.java)

        /**
         * Obfuscates a token string for logging purposes.
         * Shows only the first 8 characters followed by "..." for security.
         *
         * @return Obfuscated token string, or "null" if the token is null
         */
        private fun String?.obfuscated(): String = this?.take(8)?.plus("...") ?: "null"

        /**
         * Moshi adapter for device model preferences map.
         *
         * Stores a mapping of device type name (String) to DeviceModelSelection.
         * This allows each device type (TRMNL, BYOS, BYOD) to have its own selected model.
         *
         * Example JSON structure:
         * ```json
         * {
         *   "BYOD": {
         *     "name": "amazon_kindle_2024",
         *     "label": "Amazon Kindle 2024"
         *   },
         *   "BYOS": {
         *     "name": "waveshare_7in3f",
         *     "label": "Waveshare 7.3\" ACeP"
         *   }
         * }
         * ```
         */
        private val deviceModelPreferencesType =
            Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                DeviceModelSelection::class.java,
            )
        private val deviceModelPreferencesAdapter =
            moshi.adapter<Map<String, DeviceModelSelection>>(deviceModelPreferencesType)

        /**
         * Gets the device type as a Flow
         */
        val deviceTypeFlow: Flow<TrmnlDeviceType?> =
            context.deviceConfigStore.data.map { preferences ->
                preferences[DEVICE_TYPE_KEY]?.let {
                    try {
                        deviceTypeAdapter.fromJson(it)
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Failed to parse device type")
                        TrmnlDeviceType.TRMNL
                    }
                }
            }

        /**
         * Gets the access token as a Flow
         */
        val accessTokenFlow: Flow<String?> =
            context.deviceConfigStore.data.map { preferences ->
                preferences[ACCESS_TOKEN_KEY]
            }

        /**
         * Gets the server base URL as a Flow
         */
        val serverUrlFlow: Flow<String?> =
            context.deviceConfigStore.data.map { preferences ->
                preferences[API_BASE_URL_KEY] ?: TRMNL_API_SERVER_BASE_URL
            }

        /**
         * Gets the refresh rate in seconds as a Flow
         */
        val refreshRateSecondsFlow: Flow<Long?> =
            context.deviceConfigStore.data.map { preferences ->
                preferences[REFRESH_RATE_SEC_KEY]
            }

        /**
         * Gets the device MAC address as a Flow
         */
        val deviceMacIdFlow: Flow<String?> =
            context.deviceConfigStore.data.map { preferences ->
                preferences[DEVICE_MAC_ID_KEY]
            }

        /**
         * Gets the device model preferences (map of device type to model selection) as a Flow.
         * Returns a map where keys are device type names (e.g., "BYOD") and values are DeviceModelSelection objects.
         */
        val deviceModelPreferencesFlow: Flow<Map<String, DeviceModelSelection>> =
            context.deviceConfigStore.data
                .map { preferences ->
                    val json = preferences[DEVICE_MODEL_PREFERENCES_KEY]
                    if (json != null) {
                        try {
                            deviceModelPreferencesAdapter.fromJson(json) ?: emptyMap()
                        } catch (e: Exception) {
                            Timber.tag(TAG).e(e, "Failed to parse device model preferences")
                            emptyMap()
                        }
                    } else {
                        emptyMap()
                    }
                }.distinctUntilChanged()

        /**
         * Gets the complete device config as a Flow
         *
         * ## Loading Strategy
         * This Flow uses a dual-path approach to support both modern and legacy storage:
         *
         * **Primary Path (Modern):**
         * - Checks for `CONFIG_JSON_KEY` preference
         * - If exists, deserializes JSON to `TrmnlDeviceConfig`
         * - Logs: "Loading device config (JSON): type=..., userApiToken=..."
         *
         * **Fallback Path (Legacy Migration):**
         * - If `CONFIG_JSON_KEY` doesn't exist, builds config from individual preference keys
         * - Reads: `DEVICE_TYPE_KEY`, `ACCESS_TOKEN_KEY`, `API_BASE_URL_KEY`, etc.
         * - Logs: "Loading device config (legacy): type=..., userApiToken=..."
         * - Only returns config if `ACCESS_TOKEN_KEY` exists (required field)
         *
         * **Domain Migration:**
         * - Automatically migrates `usetrmnl.com` to `trmnl.com` for TRMNL device types
         * - See: https://github.com/usetrmnl/trmnl-android/issues/240
         *
         * This approach ensures seamless migration from older app versions while
         * maintaining forward compatibility with newer storage format.
         */
        val deviceConfigFlow: Flow<TrmnlDeviceConfig?> =
            context.deviceConfigStore.data
                .map { preferences ->
                    val configJson = preferences[CONFIG_JSON_KEY]
                    if (configJson != null) {
                        try {
                            val config: TrmnlDeviceConfig? = deviceConfigAdapter.fromJson(configJson)
                            Timber.tag(TAG).d(
                                "Loading device config (JSON): type=${config?.type}",
                            )
                            config
                        } catch (e: Exception) {
                            Timber.tag(TAG).e(e, "Failed to parse device config")
                            null
                        }
                    } else {
                        // Legacy migration path - build config from individual preferences
                        val type =
                            preferences[DEVICE_TYPE_KEY]?.let {
                                try {
                                    deviceTypeAdapter.fromJson(it)
                                } catch (e: Exception) {
                                    TrmnlDeviceType.TRMNL
                                }
                            } ?: TrmnlDeviceType.TRMNL

                        val token = preferences[ACCESS_TOKEN_KEY]
                        val url = preferences[API_BASE_URL_KEY] ?: TRMNL_API_SERVER_BASE_URL
                        val refreshRate = preferences[REFRESH_RATE_SEC_KEY] ?: DEFAULT_REFRESH_INTERVAL_SEC
                        val deviceMacId = preferences[DEVICE_MAC_ID_KEY]
                        val isMasterDevice = preferences[IS_MASTER_DEVICE_KEY]?.toBoolean()

                        Timber.tag(TAG).d(
                            "Loading device config (legacy): type=$type, deviceApiToken=${token.obfuscated()}",
                        )

                        if (token != null) {
                            TrmnlDeviceConfig(
                                type = type,
                                apiBaseUrl = url,
                                apiAccessToken = token,
                                deviceMacId = deviceMacId,
                                refreshRateSecs = refreshRate,
                                isMasterDevice = isMasterDevice,
                            )
                        } else {
                            null
                        }
                    }
                }.map { config ->
                    // Migrate usetrmnl.com -> trmnl.com for TRMNL device types
                    // See: https://github.com/usetrmnl/trmnl-android/issues/240
                    if (config != null &&
                        config.type == TrmnlDeviceType.TRMNL &&
                        config.apiBaseUrl.contains(AppConfig.LEGACY_TRMNL_DOMAIN, ignoreCase = true)
                    ) {
                        val newUrl = config.apiBaseUrl.replace(AppConfig.LEGACY_TRMNL_DOMAIN, AppConfig.TRMNL_DOMAIN, ignoreCase = true)
                        Timber.tag(TAG).i(
                            "Migrating API base URL from ${config.apiBaseUrl} to $newUrl for TRMNL device",
                        )
                        val migratedConfig = config.copy(apiBaseUrl = newUrl)
                        // Save the migrated config back to DataStore synchronously
                        // Using runBlocking is acceptable here as this is a one-time migration
                        runBlocking {
                            saveDeviceConfig(migratedConfig)
                        }
                        migratedConfig
                    } else {
                        config
                    }
                }

        /**
         * Saves the complete device configuration
         *
         * ## Dual-Storage Approach
         * This method saves the config in **both** formats for maximum compatibility:
         *
         * **Modern Storage:**
         * - Serializes entire `TrmnlDeviceConfig` to JSON
         * - Saves to `CONFIG_JSON_KEY` preference
         * - Single source of truth for modern app versions
         *
         * **Legacy Storage:**
         * - Also saves individual fields to separate preference keys
         * - Ensures older app versions can still read the config
         * - Fields: `DEVICE_TYPE_KEY`, `ACCESS_TOKEN_KEY`, `USER_API_TOKEN_KEY`, etc.
         *
         * **Null Handling:**
         * - Optional fields (e.g., `userApiToken`, `isMasterDevice`) use `let` operator
         * - If null, the preference key is removed with `preferences.remove()`
         * - This keeps DataStore clean and prevents storing empty strings
         *
         * @param config The complete device configuration to save
         */
        suspend fun saveDeviceConfig(config: TrmnlDeviceConfig) {
            try {
                Timber.tag(TAG).d(
                    "Saving device config: type=${config.type}",
                )
                val configJson = deviceConfigAdapter.toJson(config)
                context.deviceConfigStore.edit { preferences ->
                    // Save as JSON for future use
                    preferences[CONFIG_JSON_KEY] = configJson

                    // Also save individual fields for backward compatibility
                    preferences[DEVICE_TYPE_KEY] = deviceTypeAdapter.toJson(config.type)
                    preferences[ACCESS_TOKEN_KEY] = config.apiAccessToken
                    preferences[API_BASE_URL_KEY] = config.apiBaseUrl
                    preferences[REFRESH_RATE_SEC_KEY] = config.refreshRateSecs

                    // Save device ID if available
                    config.deviceMacId?.let { deviceMacId ->
                        preferences[DEVICE_MAC_ID_KEY] = deviceMacId
                    }

                    // Save isMasterDevice if available
                    config.isMasterDevice?.let { isMaster ->
                        preferences[IS_MASTER_DEVICE_KEY] = isMaster.toString()
                    } ?: preferences.remove(IS_MASTER_DEVICE_KEY)
                }
                Timber.tag(TAG).d("Device config saved successfully")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to save device config")
            }
        }

        /**
         * Saves the device type
         */
        suspend fun saveDeviceType(type: TrmnlDeviceType) {
            context.deviceConfigStore.edit { preferences ->
                preferences[DEVICE_TYPE_KEY] = deviceTypeAdapter.toJson(type)
            }
        }

        /**
         * Saves the access token
         */
        suspend fun saveAccessToken(token: String) {
            context.deviceConfigStore.edit { preferences ->
                preferences[ACCESS_TOKEN_KEY] = token
            }
        }

        /**
         * Saves the server URL
         */
        suspend fun saveServerUrl(url: String) {
            context.deviceConfigStore.edit { preferences ->
                preferences[API_BASE_URL_KEY] = url
            }
        }

        /**
         * Saves the refresh rate in seconds
         */
        suspend fun saveRefreshRateSeconds(seconds: Long) {
            context.deviceConfigStore.edit { preferences ->
                preferences[REFRESH_RATE_SEC_KEY] = seconds
            }
        }

        /**
         * Saves the device ID (MAC address)
         */
        suspend fun saveDeviceMacId(deviceMacId: String?) {
            context.deviceConfigStore.edit { preferences ->
                if (deviceMacId != null) {
                    preferences[DEVICE_MAC_ID_KEY] = deviceMacId
                } else {
                    preferences.remove(DEVICE_MAC_ID_KEY)
                }
            }
        }

        /**
         * Saves the selected device model for a specific device type.
         *
         * @param deviceType The device type (e.g., BYOD, BYOS)
         * @param modelName The model name (e.g., "amazon_kindle_2024")
         * @param modelLabel The model label (e.g., "Amazon Kindle 2024")
         */
        suspend fun saveDeviceModelForType(
            deviceType: TrmnlDeviceType,
            modelName: String,
            modelLabel: String,
        ) {
            try {
                context.deviceConfigStore.edit { preferences ->
                    // Get current map
                    val currentJson = preferences[DEVICE_MODEL_PREFERENCES_KEY]
                    val currentMap =
                        if (currentJson != null) {
                            try {
                                deviceModelPreferencesAdapter.fromJson(currentJson)?.toMutableMap() ?: mutableMapOf()
                            } catch (e: Exception) {
                                Timber.tag(TAG).e(e, "Failed to parse existing device model preferences")
                                mutableMapOf()
                            }
                        } else {
                            mutableMapOf()
                        }

                    // Update the map with new value
                    currentMap[deviceType.name] = DeviceModelSelection(modelName, modelLabel)

                    // Save back to preferences
                    preferences[DEVICE_MODEL_PREFERENCES_KEY] = deviceModelPreferencesAdapter.toJson(currentMap)

                    Timber.tag(TAG).d("Saved device model preference: ${deviceType.name} -> $modelName ($modelLabel)")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to save device model preference")
            }
        }

        /**
         * Gets the selected device model selection for a specific device type.
         *
         * @param deviceType The device type to query
         * @return The DeviceModelSelection if set, null otherwise
         */
        suspend fun getDeviceModelForType(deviceType: TrmnlDeviceType): DeviceModelSelection? =
            deviceModelPreferencesFlow.first()[deviceType.name]

        /**
         * Checks if a token is already set
         */
        val hasTokenFlow: Flow<Boolean> =
            accessTokenFlow.map { token ->
                !token.isNullOrBlank()
            }

        /**
         * Checks synchronously if token is already set
         */
        fun hasTokenSync(): Boolean {
            return runBlocking {
                return@runBlocking hasTokenFlow.first()
            }
        }

        /**
         * Gets the device config synchronously (blocking)
         */
        fun getDeviceConfigSync(): TrmnlDeviceConfig? {
            return runBlocking {
                return@runBlocking deviceConfigFlow.first()
            }
        }

        /**
         * Validates if the provided URL is properly formatted
         */
        fun isValidServerUrl(url: String): Boolean =
            try {
                val uri = Uri.parse(url)
                uri != null && (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrEmpty()
            } catch (e: Exception) {
                false
            }

        /**
         * Checks if refresh rate needs to be updated
         */
        suspend fun shouldUpdateRefreshRate(newRefreshRateSec: Long): Boolean {
            val currentRefreshRate = refreshRateSecondsFlow.first()
            return currentRefreshRate != null && newRefreshRateSec != currentRefreshRate
        }

        /**
         * Clears all stored preferences
         */
        suspend fun clearAll() {
            context.deviceConfigStore.edit { preferences ->
                preferences.clear()
            }
        }
    }
