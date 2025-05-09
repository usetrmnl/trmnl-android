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
import ink.trmnl.android.data.AppConfig.DEFAULT_REFRESH_INTERVAL_SEC
import ink.trmnl.android.data.AppConfig.TRMNL_API_SERVER_BASE_URL
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.di.ApplicationContext
import ink.trmnl.android.model.TrmnlDeviceConfig
import ink.trmnl.android.model.TrmnlDeviceType
import kotlinx.coroutines.flow.Flow
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
        }

        private val deviceTypeAdapter = moshi.adapter(TrmnlDeviceType::class.java)
        private val deviceConfigAdapter = moshi.adapter(TrmnlDeviceConfig::class.java)

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
         * Gets the complete device config as a Flow
         */
        val deviceConfigFlow: Flow<TrmnlDeviceConfig?> =
            context.deviceConfigStore.data.map { preferences ->
                val configJson = preferences[CONFIG_JSON_KEY]
                if (configJson != null) {
                    try {
                        deviceConfigAdapter.fromJson(configJson)
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

                    if (token != null) {
                        TrmnlDeviceConfig(
                            type = type,
                            apiBaseUrl = url,
                            apiAccessToken = token,
                            refreshRateSecs = refreshRate,
                        )
                    } else {
                        null
                    }
                }
            }

        /**
         * Saves the complete device configuration
         */
        suspend fun saveDeviceConfig(config: TrmnlDeviceConfig) {
            try {
                val configJson = deviceConfigAdapter.toJson(config)
                context.deviceConfigStore.edit { preferences ->
                    // Save as JSON for future use
                    preferences[CONFIG_JSON_KEY] = configJson

                    // Also save individual fields for backward compatibility
                    preferences[DEVICE_TYPE_KEY] = deviceTypeAdapter.toJson(config.type)
                    preferences[ACCESS_TOKEN_KEY] = config.apiAccessToken
                    preferences[API_BASE_URL_KEY] = config.apiBaseUrl
                    preferences[REFRESH_RATE_SEC_KEY] = config.refreshRateSecs
                }
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
