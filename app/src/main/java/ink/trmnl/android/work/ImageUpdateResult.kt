package ink.trmnl.android.work

import androidx.annotation.Keep
import ink.trmnl.android.data.ImageMetadata

/**
 * Sealed class representing the result of an image update operation.
 *
 * This provides a type-safe way to handle different outcomes of image refresh operations:
 * - [Success]: New image was successfully fetched and should be displayed
 * - [RateLimited]: Rate limit encountered, keep showing current image with notification
 * - [Error]: Fatal error occurred, display error state
 */
@Keep
sealed class ImageUpdateResult {
    /**
     * Successfully fetched a new image to display.
     *
     * @param metadata The metadata for the new image including URL and refresh interval
     */
    data class Success(
        val metadata: ImageMetadata,
    ) : ImageUpdateResult()

    /**
     * Rate limit encountered (HTTP 429).
     * Keep showing the current image and notify the user.
     *
     * NOTE: This usually happens for current display API. See details below:
     * - https://discord.com/channels/1281055965508141100/1336424981495676978/1429827943902744618
     *
     * @param message User-friendly message to display (e.g., "Rate limit (10s cooldown)")
     */
    data class RateLimited(
        val message: String,
    ) : ImageUpdateResult()

    /**
     * Fatal error occurred during image fetch.
     * Display error state to the user.
     *
     * @param message Error message to display to the user
     */
    data class Error(
        val message: String,
    ) : ImageUpdateResult()
}
