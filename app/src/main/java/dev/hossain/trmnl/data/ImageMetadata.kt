package dev.hossain.trmnl.data

import java.time.Instant

/**
 * Data class to store information about the last retrieved image
 */
data class ImageMetadata constructor(
    val url: String,
    /**
     * (OPTIONAL) Image refresh interval provided by API server
     */
    val refreshIntervalSecs: Long? = null,
    /**
     * (OPTIONAL) Error message if the image retrieval failed
     */
    val errorMessage: String? = null,
    val timestamp: Long = Instant.now().toEpochMilli(),
)
