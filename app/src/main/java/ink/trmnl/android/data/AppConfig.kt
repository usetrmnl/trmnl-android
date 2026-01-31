package ink.trmnl.android.data

/**
 * Global application configuration.
 */
object AppConfig {
    /**
     * Number of HTTP request-response log entries to keep in the data store before they are pruned.
     * See [ink.trmnl.android.data.log.TrmnlRefreshLogManager].
     */
    const val MAX_LOG_ENTRIES = 100

    /**
     * Duration in milliseconds to wait before automatically hiding the app config window.
     */
    const val AUTO_HIDE_APP_CONFIG_WINDOW_MS: Long = 4_000L // 4 seconds

    /**
     * Default display refresh rate in case the server does not provide one.
     */
    const val DEFAULT_REFRESH_INTERVAL_SEC: Long = 7_200L // 2 hours

    /**
     * Base URL for the TRMNL API server.
     * Ref: https://github.com/usetrmnl/trmnl-android/issues/171
     */
    const val TRMNL_API_SERVER_BASE_URL = "https://trmnl.com/"

    /**
     * URL for the TRMNL Android app on GitHub.
     */
    const val TRMNL_ANDROID_APP_GITHUB_URL = "https://github.com/usetrmnl/trmnl-android"

    /**
     * URL for the TRMNL main website.
     */
    const val TRMNL_SITE_URL = "https://trmnl.com/"
}
