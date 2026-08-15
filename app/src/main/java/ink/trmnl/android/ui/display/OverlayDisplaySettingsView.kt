package ink.trmnl.android.ui.display

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import ink.trmnl.android.R
import ink.trmnl.android.ui.icons.Icons
import ink.trmnl.android.ui.theme.TrmnlDisplayAppTheme

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
 * @param modifier Modifier applied to the outer container.
 * @param windowSizeClass The current window size class, used to adapt the UI for different screen sizes.
 */
@Composable
internal fun OverlaySettingsView(
    state: TrmnlMirrorDisplayScreen.State,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    val isExpandedWidth =
        windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND) ||
            windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)

    val buttonHeight = if (isExpandedWidth) 52.dp else 48.dp
    val iconSize = if (isExpandedWidth) 22.dp else 20.dp

    OutlinedCard(
        modifier =
            modifier
                .padding(16.dp)
                .widthIn(max = 480.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Text(
                text = "Display Controls",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            // Status chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Next refresh: ${state.nextImageRefreshIn}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action 1: Configure TRMNL (Top action)
            OutlinedButton(
                onClick = {
                    state.eventSink(TrmnlMirrorDisplayScreen.Event.ConfigureRequested)
                },
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configure TRMNL",
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Action 2: Load Next Playlist Image (Primary action)
            Button(
                onClick = {
                    state.eventSink(TrmnlMirrorDisplayScreen.Event.LoadNextPlaylistItemImage)
                },
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Load Next Playlist Image",
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Action 3: Reload Image & Save Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        state.eventSink(TrmnlMirrorDisplayScreen.Event.RefreshCurrentPlaylistItemRequested)
                    },
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reload Image",
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                FilledTonalButton(
                    onClick = {
                        state.eventSink(TrmnlMirrorDisplayScreen.Event.SaveImageRequested)
                    },
                    modifier = Modifier.height(buttonHeight),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.download_photo),
                        contentDescription = "Save Image",
                        modifier = Modifier.size(iconSize),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Action 4: View Image Refresh Logs
            OutlinedButton(
                onClick = {
                    state.eventSink(TrmnlMirrorDisplayScreen.Event.ViewLogsRequested)
                },
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Refresh Logs",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(name = "Overlay Settings Preview")
@Composable
fun PreviewOverlaySettingsView() {
    TrmnlDisplayAppTheme {
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
}
