package ink.trmnl.android.data

/**
 * Global application configuration.
 */
object AppConfig {
    /**
     * Default display refresh rate in case the server does not provide one.
     */
    const val DEFAULT_REFRESH_INTERVAL_SEC: Long = 7_200L // 2 hours

    /**
     * New base URL for the TRMNL API server.
     * Ref: https://discord.com/channels/1281055965508141100/1284986536357662740/1364623667337760910
     */
    const val TRMNL_API_SERVER_BASE_URL = "https://trmnl.app/"
}
