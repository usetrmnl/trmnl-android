package ink.trmnl.android.ui.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import ink.trmnl.android.R

/**
 * Displays a set of configuration and control actions for the TRMNL display.
 *
 * This overlay provides quick access to common display controls such as:
 * - Manual refresh functionality
 * - Loading next playlist image
 * - Viewing refresh history logs
 * - Accessing app settings
 *
 * @param state The current state of the TRMNL mirror display, including refresh info and event sink.
 * @param windowSizeClass The current window size class, used to adapt the UI for different screen sizes.
 */
@Composable
internal fun OverlaySettingsView(
    state: TrmnlMirrorDisplayScreen.State,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    // Shows larger button on tablets
    // https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes
    // https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
    val isExpandedWidth =
        windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND) ||
            windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)

    // Choose text style based on window width
    val fabTextStyle =
        if (isExpandedWidth) {
            MaterialTheme.typography.titleLarge
        } else {
            MaterialTheme.typography.bodyLarge
        }

    val infoTextStyle =
        if (isExpandedWidth) {
            MaterialTheme.typography.titleLarge
        } else {
            MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        }

    Card(
        modifier =
            Modifier
                .padding(16.dp),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp,
            ),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Display Configurations",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text("Display image will refresh: ${state.nextImageRefreshIn}", style = infoTextStyle)

            ExtendedFloatingActionButton(
                onClick = {
                    state.eventSink(TrmnlMirrorDisplayScreen.Event.ConfigureRequested)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = if (isExpandedWidth) Modifier.size(32.dp) else Modifier,
                    )
                },
                text = {
                    Text(
                        "Configure TRMNL",
                        style = fabTextStyle,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        state.eventSink(TrmnlMirrorDisplayScreen.Event.RefreshCurrentPlaylistItemRequested)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = if (isExpandedWidth) Modifier.size(32.dp) else Modifier,
                        )
                    },
                    text = {
                        Text(
                            "Reload Current Image",
                            style = fabTextStyle,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = {
                        state.eventSink(TrmnlMirrorDisplayScreen.Event.SaveImageRequested)
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.image_24dp),
                        contentDescription = "Save Image",
                        modifier = if (isExpandedWidth) Modifier.size(32.dp) else Modifier.size(24.dp),
                    )
                }
            }

            ExtendedFloatingActionButton(
                onClick = {
                    state.eventSink(TrmnlMirrorDisplayScreen.Event.LoadNextPlaylistItemImage)
                },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = if (isExpandedWidth) Modifier.size(32.dp) else Modifier,
                    )
                },
                text = {
                    Text(
                        "Load Next Playlist Image",
                        style = fabTextStyle,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )

            ExtendedFloatingActionButton(
                onClick = {
                    state.eventSink(TrmnlMirrorDisplayScreen.Event.ViewLogsRequested)
                },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = if (isExpandedWidth) Modifier.size(32.dp) else Modifier,
                    )
                },
                text = {
                    Text(
                        "View Image Refresh Logs",
                        style = fabTextStyle,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        }
    }
}

@Preview(name = "Overlay Settings Preview")
@Composable
fun PreviewOverlaySettingsView() {
    Surface {
        OverlaySettingsView(
            state =
                TrmnlMirrorDisplayScreen.State(
                    imageUrl = null,
                    overlayControlsVisible = true,
                    nextImageRefreshIn = "5 minutes",
                    isLoading = false,
                    errorMessage = null,
                    saveImageResult = null,
                    eventSink = {},
                ),
        )
    }
}
