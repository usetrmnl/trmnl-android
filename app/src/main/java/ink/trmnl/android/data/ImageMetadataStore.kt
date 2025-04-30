package ink.trmnl.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ink.trmnl.android.di.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

private val Context.imageDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "trmnl_image_metadata",
)

/**
 * Store for image metadata, including URL, timestamp, and refresh interval.
 * These are used as cache to avoid unnecessary network calls.
 *
 * @see ImageMetadata
 */
class ImageMetadataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private val IMAGE_URL_KEY = stringPreferencesKey("last_image_url")
            private val TIMESTAMP_KEY = longPreferencesKey("last_image_timestamp")
            private val REFRESH_RATE_KEY = longPreferencesKey("last_refresh_rate")
        }

        /**
         * Get the image metadata as a Flow
         */
        val imageMetadataFlow: Flow<ImageMetadata?> =
            context.imageDataStore.data.map { preferences ->
                val imageUrl = preferences[IMAGE_URL_KEY] ?: return@map null
                val timestamp = preferences[TIMESTAMP_KEY] ?: Instant.now().toEpochMilli()
                val refreshRate = preferences[REFRESH_RATE_KEY]

                ImageMetadata(
                    url = imageUrl,
                    refreshIntervalSecs = refreshRate,
                    errorMessage = null,
                    timestamp = timestamp,
                )
            }

        /**
         * Save new image metadata
         */
        suspend fun saveImageMetadata(
            imageUrl: String,
            refreshIntervalSec: Long? = null,
        ) {
            Timber.d("Saving image metadata: url=$imageUrl, refreshIntervalSec=$refreshIntervalSec")
            context.imageDataStore.edit { preferences ->
                preferences[IMAGE_URL_KEY] = imageUrl
                preferences[TIMESTAMP_KEY] = Instant.now().toEpochMilli()
                refreshIntervalSec?.let { preferences[REFRESH_RATE_KEY] = it }
            }
        }

        /**
         * Checks if the stored image URL is still valid based on refresh rate
         * @return Flow of Boolean indicating if a valid, non-expired image URL exists
         */
        val hasValidImageUrlFlow: Flow<Boolean> =
            context.imageDataStore.data.map { preferences ->
                val url = preferences[IMAGE_URL_KEY] ?: return@map false
                val timestamp = preferences[TIMESTAMP_KEY] ?: return@map false
                val refreshRate = preferences[REFRESH_RATE_KEY] ?: return@map false

                // Calculate if the image is expired based on timestamp + refresh rate
                val expirationTime = timestamp + (refreshRate * 1000) // Convert seconds to milliseconds
                val currentTime = Instant.now().toEpochMilli()

                // Image is valid if current time is before expiration
                url.isNotEmpty() && currentTime < expirationTime
            }

        /**
         * Checks synchronously if a valid, non-expired image URL exists
         * @return true if valid image URL exists and is not expired
         */
        fun hasValidImageUrlSync(): Boolean {
            return runBlocking {
                return@runBlocking hasValidImageUrlFlow.first()
            }
        }

        /**
         * Returns the amount of time in milliseconds until the current image expires
         * @return Positive value if image is still valid, negative if already expired, null if no valid image
         */
        val timeUntilExpirationFlow: Flow<Long?> =
            context.imageDataStore.data.map { preferences ->
                val timestamp = preferences[TIMESTAMP_KEY] ?: return@map null
                val refreshRate = preferences[REFRESH_RATE_KEY] ?: return@map null

                // Calculate time until expiration
                val expirationTime = timestamp + (refreshRate * 1000) // Convert seconds to milliseconds
                val currentTime = Instant.now().toEpochMilli()

                expirationTime - currentTime
            }

        /**
         * Clear stored image metadata
         */
        suspend fun clearImageMetadata() {
            context.imageDataStore.edit { preferences ->
                preferences.remove(IMAGE_URL_KEY)
                preferences.remove(TIMESTAMP_KEY)
                preferences.remove(REFRESH_RATE_KEY)
            }
        }
    }
