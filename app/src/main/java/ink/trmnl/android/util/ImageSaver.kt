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
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class for saving images to the device storage.
 * Uses the MediaStore API for Android 10+ and legacy storage for older versions.
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

                return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveImageMediaStore(context, bitmap, filename)
                } else {
                    saveImageLegacy(bitmap, filename)
                }
            } catch (e: Exception) {
                Timber.tag(IMAGE_SAVER_TAG).e(e, "Error saving image")
                return@withContext null
            }
        }

    /**
     * Save image using MediaStore API (Android 10+)
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
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/TRMNL")
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

    /**
     * Save image using legacy storage (Android 9 and below)
     */
    @Suppress("DEPRECATION")
    private fun saveImageLegacy(
        bitmap: Bitmap,
        filename: String,
    ): Uri? {
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val trmnlDir = File(picturesDir, "TRMNL")

        if (!trmnlDir.exists()) {
            trmnlDir.mkdirs()
        }

        val imageFile = File(trmnlDir, filename)

        return try {
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            Timber.tag(IMAGE_SAVER_TAG).d("Image saved successfully to: ${imageFile.absolutePath}")
            Uri.fromFile(imageFile)
        } catch (e: Exception) {
            Timber.tag(IMAGE_SAVER_TAG).e(e, "Error writing image using legacy storage")
            null
        }
    }
}
