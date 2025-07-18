package ink.trmnl.android.ui.refreshlog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.trmnl.android.data.HttpResponseMetadata
import ink.trmnl.android.ui.theme.TrmnlDisplayAppTheme
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
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        HttpResponseMetadataDisplay(httpResponseMetadata)
    }
}

@Composable
private fun HttpResponseMetadataDisplay(httpResponseMetadata: HttpResponseMetadata) {
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
        DetailItem(
            label = "HTTP Protocol",
            value = if (httpResponseMetadata.protocol == "h2") "HTTP/2" else httpResponseMetadata.protocol,
        )

        // Server
        httpResponseMetadata.serverName?.let {
            DetailItem(label = "Server", value = it)
        }

        // Content Type
        httpResponseMetadata.contentType?.let {
            DetailItem(label = "Content Type", value = it)
        }

        // Content Length
        if (httpResponseMetadata.contentLength >= 0) {
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
        val dateFormat =
            remember { SimpleDateFormat("MMM dd, yyyy hh:mm:ss.SSS a", Locale.getDefault()) }
        val formattedTimestamp = dateFormat.format(Date(httpResponseMetadata.timestamp))
        DetailItem(label = "Response Time", value = formattedTimestamp)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )

        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

private const val BYTES_IN_KB = 1024
private const val BYTES_IN_MB = BYTES_IN_KB * 1024
private const val BYTES_IN_GB = BYTES_IN_MB * 1024

/**
 * Format bytes into a human-readable string
 */
private fun formatBytes(bytes: Long): String =
    when {
        bytes < BYTES_IN_KB -> "$bytes B"
        bytes < BYTES_IN_MB -> String.format(Locale.US, "%.2f KB", bytes / BYTES_IN_KB.toDouble())
        bytes < BYTES_IN_GB -> String.format(Locale.US, "%.2f MB", bytes / BYTES_IN_MB.toDouble())
        else -> String.format(Locale.US, "%.2f GB", bytes / BYTES_IN_GB.toDouble())
    }

/**
 * Previews for HttpResponseMetadataDisplay with different response scenarios
 */
@Preview(name = "HTTP Response Details - Full", showBackground = true)
@Composable
private fun PreviewHttpResponseMetadataDisplay() {
    val sampleMetadata =
        HttpResponseMetadata(
            url = "https://trmnl.app/api/current_screen",
            protocol = "h2",
            statusCode = 200,
            message = "OK",
            contentType = "application/json; charset=utf-8",
            contentLength = 317,
            serverName = null,
            requestDuration = 100,
            etag = "W/\"3fa28fbc90b01b99c92466b34bc476d8\"",
            requestId = "f064d019-0d34-4819-bef4-6132bbef4d8c",
            timestamp = System.currentTimeMillis(),
        )

    TrmnlDisplayAppTheme {
        HttpResponseMetadataDisplay(httpResponseMetadata = sampleMetadata)
    }
}

@Preview(name = "HTTP Response Details - Minimal (Dark)", showBackground = true)
@Composable
private fun PreviewHttpResponseMetadataDisplayMinimal() {
    val sampleMetadata =
        HttpResponseMetadata(
            url = "https://trmnl.app/api/current_screen",
            protocol = "http/1.1",
            statusCode = 304,
            message = "Not Modified",
            contentType = null,
            contentLength = 0,
            serverName = null,
            requestDuration = 35,
            etag = "W/\"3fa28fbc90b01b99c92466b34bc476d8\"",
            requestId = null,
            timestamp = System.currentTimeMillis(),
        )

    TrmnlDisplayAppTheme(darkTheme = true) {
        HttpResponseMetadataDisplay(httpResponseMetadata = sampleMetadata)
    }
}
