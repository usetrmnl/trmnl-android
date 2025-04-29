package dev.hossain.trmnl.data

import dev.hossain.trmnl.data.AppConfig.DEFAULT_REFRESH_INTERVAL_SEC

/**
 * Represents the display information for the TRMNL.
 * @see [TrmnlDisplayRepository]
 */
data class TrmnlDisplayInfo(
    val status: Int,
    val imageUrl: String,
    val imageName: String,
    val error: String? = null,
    val refreshIntervalSeconds: Long? = DEFAULT_REFRESH_INTERVAL_SEC,
)
