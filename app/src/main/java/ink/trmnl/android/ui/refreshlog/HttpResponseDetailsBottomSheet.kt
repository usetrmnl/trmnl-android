package ink.trmnl.android.ui.refreshlog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ink.trmnl.android.data.HttpResponseMetadata
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet that displays detailed HTTP response metadata
 *
 * @param httpResponseMetadata The HTTP response metadata to display
 * @param onDismiss Callback when the bottom sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpResponseDetailsBottomSheet(
    httpResponseMetadata: HttpResponseMetadata,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = "HTTP Response Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Request URL
            DetailItem(label = "Request URL", value = httpResponseMetadata.url)

            // HTTP Status
            DetailItem(
                label = "HTTP Status",
                value = "${httpResponseMetadata.statusCode} ${httpResponseMetadata.message}",
            )

            // Protocol
            DetailItem(label = "HTTP Protocol", value = httpResponseMetadata.protocol)

            // Server
            httpResponseMetadata.serverName?.let {
                DetailItem(label = "Server", value = it)
            }

            // Content Type
            httpResponseMetadata.contentType?.let {
                DetailItem(label = "Content Type", value = it)
            }

            // Content Length
            if (httpResponseMetadata.contentLength > 0) {
                DetailItem(
                    label = "Content Length",
                    value = formatBytes(httpResponseMetadata.contentLength),
                )
            }

            // ETag (for cache validation)
            httpResponseMetadata.etag?.let {
                DetailItem(label = "ETag", value = it)
            }

            // Request ID (for server-side tracing)
            httpResponseMetadata.requestId?.let {
                DetailItem(label = "Request ID", value = it)
            }

            // Request Duration
            if (httpResponseMetadata.requestDuration > 0) {
                DetailItem(
                    label = "Request Duration",
                    value = "${httpResponseMetadata.requestDuration}ms",
                )
            }

            // Timestamp
            val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss.SSS a", Locale.getDefault())
            val formattedTimestamp = dateFormat.format(Date(httpResponseMetadata.timestamp))
            DetailItem(label = "Response Time", value = formattedTimestamp)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp),
        )

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * Format bytes into a human-readable string
 */
private fun formatBytes(bytes: Long): String =
    when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
