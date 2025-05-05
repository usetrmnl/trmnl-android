package ink.trmnl.android.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import ink.trmnl.android.data.log.TrmnlRefreshLog
import ink.trmnl.android.data.log.TrmnlRefreshLogs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale

/**
 * Exports the logs to a JSON file and shares it via Android's share intent.
 *
 * @param context Android context used to create files and launch the share intent
 * @param logs List of refresh logs to export
 */
internal suspend fun exportLogsAndShare(
    context: Context,
    logs: List<TrmnlRefreshLog>,
) {
    withContext(Dispatchers.IO) {
        try {
            // Create timestamp for filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Instant.now().toEpochMilli())
            val filename = "trmnl_refresh_logs_$timestamp.json"

            // Create cache directory if it doesn't exist
            val cacheDir = File(context.cacheDir, "logs")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Create the JSON file
            val file = File(cacheDir, filename)

            // Use Moshi to convert logs to JSON
            val moshi =
                com.squareup.moshi.Moshi
                    .Builder()
                    .build()
            val adapter = moshi.adapter(TrmnlRefreshLogs::class.java)
            val jsonContent = adapter.toJson(TrmnlRefreshLogs(logs))

            // Write JSON to file
            file.writeText(jsonContent)

            // Get content URI via FileProvider
            val fileUri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )

            // Create share intent
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "TRMNL Display Refresh Logs")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

            // Launch the share dialog
            val chooserIntent = Intent.createChooser(shareIntent, "Share TRMNL Refresh Logs")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Timber.e(e, "Error exporting logs")
        }
    }
}
