package ink.trmnl.android.data

import ink.trmnl.android.BuildConfig
import javax.inject.Inject

/**
 * Provides configuration for repository data sources. This class helps control whether repositories
 * should use fake/mock data instead of real API data, which is useful for development and testing.
 */
class RepositoryConfigProvider
    @Inject
    constructor() {
        /**
         * Indicates if the app should use fake data instead of real API responses.
         *
         * @return Boolean value from [BuildConfig.USE_FAKE_API]
         */
        val shouldUseFakeData: Boolean
            get() {
                // To change this value, update the `buildConfigField` in the app's build.gradle file
                // Or, change the value here for local development. Do not commit this change.
                return false
            }
    }
