package ink.trmnl.android.data

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
         * @return Boolean value from [DevConfig.FAKE_API_RESPONSE]
         */
        val shouldUseFakeData: Boolean
            get() {
                return DevConfig.FAKE_API_RESPONSE
            }
    }
