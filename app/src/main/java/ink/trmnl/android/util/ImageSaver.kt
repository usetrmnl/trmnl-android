package ink.trmnl.android.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Utility class for saving images to the device storage.
 * Uses the MediaStore API which works without permissions on Android 10+ (API 29+).
 * For Android 9 (API 28), the legacy storage path requires WRITE_EXTERNAL_STORAGE permission,
 * but we handle it gracefully by showing an error if permission is not granted.
 */
object ImageSaver {
    private const val IMAGE_SAVER_TAG = "ImageSaver"

    /**
     * Saves an image from a URL to the device's Pictures/TRMNL directory.
     *
     * @param context The application context
     * @param imageUrl The URL of the image to save
     * @return The URI of the saved image, or null if the save failed
     */
    suspend fun saveImageToDownloads(
        context: Context,
        imageUrl: String?,
    ): Uri? =
        withContext(Dispatchers.IO) {
            if (imageUrl == null) {
                Timber.tag(IMAGE_SAVER_TAG).w("Cannot save image: URL is null")
                return@withContext null
            }

            try {
                // Load the image using Coil
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(imageUrl)
                        .build()

                val result = context.imageLoader.execute(request)
                if (result !is SuccessResult) {
                    Timber.tag(IMAGE_SAVER_TAG).e("Failed to load image from URL: $imageUrl")
                    return@withContext null
                }

                val bitmap = result.image.toBitmap()

                // Generate a unique filename
                val timestamp = System.currentTimeMillis()
                val filename = "TRMNL_$timestamp.png"

                return@withContext saveImageMediaStore(context, bitmap, filename)
            } catch (e: Exception) {
                Timber.tag(IMAGE_SAVER_TAG).e(e, "Error saving image")
                return@withContext null
            }
        }

    /**
     * Save image using MediaStore API.
     * This works without permissions on Android 10+ (API 29+).
     * For Android 9 (API 28), this may fail if WRITE_EXTERNAL_STORAGE permission is not granted,
     * but we handle it gracefully.
     */
    private fun saveImageMediaStore(
        context: Context,
        bitmap: Bitmap,
        filename: String,
    ): Uri? {
        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/TRMNL")
                }
            }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        return imageUri?.let { uri ->
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                Timber.tag(IMAGE_SAVER_TAG).d("Image saved successfully to: $uri")
                uri
            } catch (e: Exception) {
                Timber.tag(IMAGE_SAVER_TAG).e(e, "Error writing image to MediaStore")
                resolver.delete(uri, null, null)
                null
            }
        }
    }
}
