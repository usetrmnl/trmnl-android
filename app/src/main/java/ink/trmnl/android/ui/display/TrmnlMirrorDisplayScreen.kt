package ink.trmnl.android.ui.display

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import ink.trmnl.android.R
import ink.trmnl.android.data.AppConfig.AUTO_HIDE_APP_CONFIG_WINDOW_MS
import ink.trmnl.android.data.ImageMetadataStore
import ink.trmnl.android.data.TrmnlDeviceConfigDataStore
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.ui.FullScreenMode
import ink.trmnl.android.ui.refreshlog.DisplayRefreshLogScreen
import ink.trmnl.android.ui.settings.AppSettingsScreen
import ink.trmnl.android.util.CoilRequestUtils
import ink.trmnl.android.util.ImageSaver
import ink.trmnl.android.util.nextRunTime
import ink.trmnl.android.work.TrmnlImageUpdateManager
import ink.trmnl.android.work.TrmnlWorkScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

/**
 * This is the full screen view to show TRMNL's bitmap image for other e-ink display,
 * or any other devices like phone or tablet.
 */
@Parcelize
data object TrmnlMirrorDisplayScreen : Screen {
    data class State(
        val imageUrl: String?,
        val overlayControlsVisible: Boolean,
        val nextImageRefreshIn: String,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val saveImageResult: SaveImageResult? = null,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class SaveImageResult {
        data object Success : SaveImageResult()

        data class Error(
            val message: String,
        ) : SaveImageResult()
    }

    sealed class Event : CircuitUiEvent {
        data object RefreshCurrentPlaylistItemRequested : Event()

        data object LoadNextPlaylistItemImage : Event()

        data object ConfigureRequested : Event()

        data object ViewLogsRequested : Event()

        data object BackPressed : Event()

        data object ToggleOverlayControls : Event()

        data object SaveImageRequested : Event()

        data class ImageLoadingError(
            val message: String,
        ) : Event()
    }
}

class TrmnlMirrorDisplayPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val trmnlDeviceConfigDataStore: TrmnlDeviceConfigDataStore,
        private val trmnlWorkScheduler: TrmnlWorkScheduler,
        private val imageMetadataStore: ImageMetadataStore,
        private val trmnlImageUpdateManager: TrmnlImageUpdateManager,
    ) : Presenter<TrmnlMirrorDisplayScreen.State> {
        @Composable
        override fun present(): TrmnlMirrorDisplayScreen.State {
            var imageUrl by remember { mutableStateOf<String?>(null) }
            var overlayControlsVisible by remember { mutableStateOf(false) }
            var isLoading by remember { mutableStateOf(true) }
            var nextRefreshTime by remember { mutableStateOf("No scheduled work found. Please set API token.") }
            var error by remember { mutableStateOf<String?>(null) }
            var saveImageResult by remember { mutableStateOf<TrmnlMirrorDisplayScreen.SaveImageResult?>(null) }
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            // Collect updates from the image update manager to get the latest image URL
            // Latest image URL is received from WorkManager work requests.
            LaunchedEffect(Unit) {
                trmnlImageUpdateManager.imageUpdateFlow.collect { imageMetadata ->
                    Timber.d("Received new image URL from TRMNL Image Update Manager: $imageMetadata")
                    if (imageMetadata != null && imageMetadata.errorMessage == null) {
                        imageUrl = imageMetadata.url
                        isLoading = false
                        error = null
                    } else {
                        Timber.w("Failed to get cached image URL from TRMNL Image Update Manager `imageUpdateFlow`")
                        // Keep showing loading state until we have a valid response from the server
                        // Only set error state if we have a non-null imageMetadata with an error
                        if (imageMetadata != null) {
                            isLoading = false
                            error = imageMetadata.errorMessage ?: "An unknown error occurred."
                        }
                    }
                }
            }

            // Auto-hide timer for overlay controls
            LaunchedEffect(overlayControlsVisible) {
                if (overlayControlsVisible) {
                    delay(AUTO_HIDE_APP_CONFIG_WINDOW_MS)
                    overlayControlsVisible = false
                }
            }

            LaunchedEffect(overlayControlsVisible) {
                trmnlWorkScheduler.getScheduledWorkInfo().collect { workInfo ->
                    workInfo?.nextRunTime()?.let {
                        nextRefreshTime = it.timeUntilNextRefresh
                    } ?: "No scheduled work found. Please set API token."
                }
            }

            // Initialize by checking token and starting one-time work if needed
            LaunchedEffect(Unit) {
                val token = trmnlDeviceConfigDataStore.accessTokenFlow.firstOrNull()
                if (token.isNullOrBlank()) {
                    Timber.d("No access token found, navigating to configuration screen")
                    navigator.goTo(AppSettingsScreen(returnToMirrorAfterSave = true))
                    return@LaunchedEffect
                }

                // Check if we have a cached image
                val hasValidImage = imageMetadataStore.hasValidImageUrlFlow.firstOrNull() ?: false
                if (hasValidImage) {
                    // Initial loading state will be updated when imageUpdateFlow emits
                    Timber.d("Valid cached image URL exists in ImageMetadataStore")
                    trmnlImageUpdateManager.initialize()
                } else {
                    Timber.d("No valid cached image, starting one-time refresh work")
                    // Always keep in loading state until we get a valid result
                    isLoading = true
                    error = null
                    // No valid image, start a refresh work
                    trmnlWorkScheduler.startOneTimeImageRefreshWork()
                }
            }

            return TrmnlMirrorDisplayScreen.State(
                imageUrl = imageUrl,
                overlayControlsVisible = overlayControlsVisible,
                nextImageRefreshIn = nextRefreshTime,
                isLoading = isLoading,
                errorMessage = error,
                saveImageResult = saveImageResult,
                eventSink = { event ->
                    when (event) {
                        TrmnlMirrorDisplayScreen.Event.RefreshCurrentPlaylistItemRequested -> {
                            overlayControlsVisible = false
                            // Simply trigger the worker for refresh
                            scope.launch {
                                // Clear the image URL so that when the image is refreshed
                                // with old image URL it will load the image.
                                imageUrl = null
                                isLoading = true
                                error = null

                                if (trmnlDeviceConfigDataStore.hasTokenSync()) {
                                    Timber.d("Manually refreshing current image via WorkManager")
                                    trmnlWorkScheduler.startOneTimeImageRefreshWork()
                                } else {
                                    error = "No access token found"
                                    isLoading = false
                                    Timber.w("Refresh failed: No access token found")
                                }
                            }
                        }
                        TrmnlMirrorDisplayScreen.Event.ConfigureRequested -> {
                            navigator.goTo(AppSettingsScreen(returnToMirrorAfterSave = true))
                        }
                        TrmnlMirrorDisplayScreen.Event.BackPressed -> {
                            navigator.pop()
                        }
                        TrmnlMirrorDisplayScreen.Event.ViewLogsRequested -> {
                            navigator.goTo(DisplayRefreshLogScreen)
                        }

                        TrmnlMirrorDisplayScreen.Event.ToggleOverlayControls -> {
                            overlayControlsVisible = !overlayControlsVisible
                        }

                        TrmnlMirrorDisplayScreen.Event.LoadNextPlaylistItemImage -> {
                            imageUrl = null
                            isLoading = true
                            error = null

                            if (trmnlDeviceConfigDataStore.hasTokenSync()) {
                                Timber.d("Manually refreshing next playlist item image via WorkManager")
                                trmnlWorkScheduler.startOneTimeImageRefreshWork(loadNextPlaylistImage = true)
                            } else {
                                error = "No access token found"
                                isLoading = false
                                Timber.w("Refresh failed: No access token found")
                            }
                        }

                        is TrmnlMirrorDisplayScreen.Event.ImageLoadingError -> {
                            error = event.message
                            isLoading = false
                        }

                        TrmnlMirrorDisplayScreen.Event.SaveImageRequested -> {
                            scope.launch {
                                saveImageResult = null
                                val savedUri = ImageSaver.saveImageToDownloads(context, imageUrl)
                                saveImageResult =
                                    if (savedUri != null) {
                                        Timber.d("Image saved successfully to: $savedUri")
                                        TrmnlMirrorDisplayScreen.SaveImageResult.Success
                                    } else {
                                        Timber.w("Failed to save image")
                                        TrmnlMirrorDisplayScreen.SaveImageResult.Error("Failed to save image")
                                    }
                            }
                        }
                    }
                },
            )
        }

        @CircuitInject(TrmnlMirrorDisplayScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): TrmnlMirrorDisplayPresenter
        }
    }

@CircuitInject(TrmnlMirrorDisplayScreen::class, AppScope::class)
@Composable
fun TrmnlMirrorDisplayContent(
    state: TrmnlMirrorDisplayScreen.State,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Use for UI preview in Android Studio
    // https://developer.android.com/develop/ui/compose/tooling/previews#localinspectionmode
    val isPreviewMode = LocalInspectionMode.current

    // Show snackbar when save result changes
    LaunchedEffect(state.saveImageResult) {
        state.saveImageResult?.let { result ->
            when (result) {
                is TrmnlMirrorDisplayScreen.SaveImageResult.Success ->
                    snackbarHostState.showSnackbar(
                        message = "Image saved to Pictures/TRMNL",
                        duration = SnackbarDuration.Short,
                    )
                is TrmnlMirrorDisplayScreen.SaveImageResult.Error ->
                    snackbarHostState.showSnackbar(
                        message = result.message,
                        duration = SnackbarDuration.Short,
                    )
            }
        }
    }

    // Apply fullscreen mode and keep screen on
    FullScreenMode(enabled = true, keepScreenOn = true)

    Surface {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, // No visual indication when clicked
                    ) {
                        state.eventSink(TrmnlMirrorDisplayScreen.Event.ToggleOverlayControls)
                    },
            contentAlignment = Alignment.Center,
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else if (state.errorMessage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.trmnl_logo_plain),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier
                                .size(64.dp)
                                .padding(bottom = 8.dp),
                    )
                    Text(
                        text = "Error: ${state.errorMessage}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                // Use a regular Image in preview mode, AsyncImage in runtime
                if (isPreviewMode) {
                    // In preview mode, use static Image with drawable resource
                    // https://developer.android.com/develop/ui/compose/tooling/previews#localinspectionmode
                    Image(
                        painter = painterResource(R.drawable.trmnl_device_white),
                        contentDescription = "Terminal Display Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // In real app, use AsyncImage with network URL
                    AsyncImage(
                        model = CoilRequestUtils.createCachedImageRequest(context, state.imageUrl),
                        contentDescription = "Terminal Display",
                        contentScale = ContentScale.Fit,
                        placeholder = painterResource(R.drawable.trmnl_logo_semi_transparent),
                        error = painterResource(R.drawable.trmnl_logo_semi_transparent),
                        onError = { error ->
                            Timber.e("Image loading failed: ${error.result.throwable}")
                            state.eventSink(
                                TrmnlMirrorDisplayScreen.Event.ImageLoadingError(
                                    "Failed to load image: ${error.result.throwable.message ?: "Unknown error"}",
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Floating action buttons that appear when controls are visible
            AnimatedVisibility(
                visible = state.overlayControlsVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
            ) {
                OverlaySettingsView(state)
            }

            // Snackbar host for showing save results
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Preview(name = "TRMNL Display Image Preview")
@PreviewScreenSizes
@Composable
fun PreviewTrmnlMirrorDisplayImageContent() {
    Surface {
        TrmnlMirrorDisplayContent(
            state =
                TrmnlMirrorDisplayScreen.State(
                    imageUrl = "placeholder_url", // Not used in preview mode
                    overlayControlsVisible = false,
                    nextImageRefreshIn = "5 minutes",
                    isLoading = false,
                    errorMessage = null,
                    saveImageResult = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "TRMNL Display With Controls")
@PreviewScreenSizes
@Composable
fun PreviewTrmnlMirrorDisplayWithControls() {
    Surface {
        TrmnlMirrorDisplayContent(
            state =
                TrmnlMirrorDisplayScreen.State(
                    imageUrl = "placeholder_url", // Not used in preview mode
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

@Preview(name = "TRMNL Display Error Content Preview")
@Composable
fun PreviewTrmnlMirrorDisplayErrorContent() {
    Surface {
        TrmnlMirrorDisplayContent(
            state =
                TrmnlMirrorDisplayScreen.State(
                    imageUrl = null, // Don't need URL for error state
                    overlayControlsVisible = false,
                    nextImageRefreshIn = "5 minutes",
                    isLoading = false,
                    errorMessage = "Sample Error Message",
                    saveImageResult = null,
                    eventSink = {},
                ),
        )
    }
}
