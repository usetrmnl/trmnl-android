package ink.trmnl.android.util

import android.content.Context
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Utility functions for Coil image loading.
 */
object CoilRequestUtils {
    /**
     * Creates an ImageRequest with optimal caching settings.
     *
     * @param context The Android context
     * @param url The image URL to load
     * @param enableCrossfade Whether to enable crossfade animation (default: true)
     * @return Configured ImageRequest
     */
    fun createCachedImageRequest(
        context: Context,
        url: String?,
        enableCrossfade: Boolean = true,
    ): ImageRequest =
        ImageRequest
            .Builder(context)
            .data(url)
            .apply {
                if (enableCrossfade) {
                    crossfade(true)
                }
            }.memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
}
