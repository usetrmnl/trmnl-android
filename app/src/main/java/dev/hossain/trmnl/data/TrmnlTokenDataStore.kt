package dev.hossain.trmnl.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.anvil.annotations.optional.SingleIn
import dev.hossain.trmnl.di.AppScope
import dev.hossain.trmnl.di.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * DataStore for managing access tokens and refresh rate settings.
 */
private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "trmnl_token_store",
)

/**
 * Manages token and refresh rate settings for the TRMNL application.
 *
 * This class provides methods for storing, retrieving, and managing the access token
 * and refresh rate settings using Android's DataStore. It offers both Flow-based
 * asynchronous APIs and synchronous blocking alternatives for different usage scenarios.
 *
 * The token manager is scoped to the application lifecycle and serves as the
 * source of truth for authentication and refresh interval configuration.
 */
@SingleIn(AppScope::class)
class TrmnlTokenDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
            private val REFRESH_RATE_SEC_KEY = longPreferencesKey("refresh_rate_seconds")
        }

        /**
         * Gets the access token as a Flow
         */
        val accessTokenFlow: Flow<String?> =
            context.tokenDataStore.data.map { preferences ->
                preferences[ACCESS_TOKEN_KEY]
            }

        /**
         * Gets the refresh rate in seconds as a Flow
         */
        val refreshRateSecondsFlow: Flow<Long?> =
            context.tokenDataStore.data.map { preferences ->
                preferences[REFRESH_RATE_SEC_KEY]
            }

        /**
         * Saves the access token to DataStore
         */
        suspend fun saveAccessToken(token: String) {
            context.tokenDataStore.edit { preferences ->
                preferences[ACCESS_TOKEN_KEY] = token
            }
        }

        /**
         * Saves the refresh rate in seconds to DataStore
         */
        suspend fun saveRefreshRateSeconds(seconds: Long) {
            context.tokenDataStore.edit { preferences ->
                preferences[REFRESH_RATE_SEC_KEY] = seconds
            }
        }

        /**
         * Gets the access token synchronously (blocking)
         */
        fun getAccessTokenSync(): String? {
            return runBlocking {
                return@runBlocking accessTokenFlow.first()
            }
        }

        /**
         * Gets the refresh rate in seconds synchronously (blocking)
         */
        fun getRefreshRateSecondsSync(): Long? {
            return runBlocking {
                return@runBlocking refreshRateSecondsFlow.first()
            }
        }

        /**
         * Checks if a token is already set
         * @return Flow of Boolean indicating if token exists and is not empty
         */
        val hasTokenFlow: Flow<Boolean> =
            accessTokenFlow.map { token ->
                !token.isNullOrBlank()
            }

        /**
         * Checks synchronously if token is already set
         * @return true if token exists and is not empty
         */
        fun hasTokenSync(): Boolean {
            return runBlocking {
                return@runBlocking hasTokenFlow.first()
            }
        }

        /**
         * Clears the access token
         */
        suspend fun clearAccessToken() {
            context.tokenDataStore.edit { preferences ->
                preferences.remove(ACCESS_TOKEN_KEY)
            }
        }

        /**
         * Clears the refresh rate settings
         */
        suspend fun clearRefreshRateSeconds() {
            context.tokenDataStore.edit { preferences ->
                preferences.remove(REFRESH_RATE_SEC_KEY)
            }
        }

        /**
         * Clears all stored preferences
         */
        suspend fun clearAll() {
            context.tokenDataStore.edit { preferences ->
                preferences.clear()
            }
        }

        suspend fun shouldUpdateRefreshRate(newRefreshRateSec: Long): Boolean {
            val currentRefreshRate = refreshRateSecondsFlow.first()
            return currentRefreshRate != null && newRefreshRateSec != currentRefreshRate
        }
    }
