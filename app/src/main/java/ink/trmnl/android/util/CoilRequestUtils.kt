package ink.trmnl.android.util

import android.content.Context
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import timber.log.Timber

/**
 * Utility functions for Coil image loading.
 */
object CoilRequestUtils {
    private const val COIL_IMAGE_LOADER_TAG = "CoilImageLoader"

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
            .listener(
                onStart = { request ->
                    Timber.tag(COIL_IMAGE_LOADER_TAG).d("Starting load: ${request.data}")
                },
                onSuccess = { request, result ->
                    Timber.tag(COIL_IMAGE_LOADER_TAG).d("Success load: ${request.data}, source: ${result.dataSource}")
                },
                onError = { request, error ->
                    Timber.tag(COIL_IMAGE_LOADER_TAG).e("Error loading: ${request.data}, error: $error")
                },
                onCancel = { request ->
                    Timber.tag(COIL_IMAGE_LOADER_TAG).d("Cancelled load: ${request.data}")
                },
            ).apply {
                if (enableCrossfade) {
                    crossfade(true)
                }
            }.memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
}
