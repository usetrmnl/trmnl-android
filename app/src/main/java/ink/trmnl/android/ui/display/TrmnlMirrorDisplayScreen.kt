package ink.trmnl.android.ui.display

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil3.compose.AsyncImage
import coil3.network.HttpException
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import ink.trmnl.android.R
import ink.trmnl.android.data.AppConfig.AUTO_HIDE_APP_CONFIG_WINDOW_MS
import ink.trmnl.android.data.ImageMetadataStore
import ink.trmnl.android.data.TrmnlDeviceConfigDataStore
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.network.RateLimitInterceptor
import ink.trmnl.android.ui.FullScreenMode
import ink.trmnl.android.ui.refreshlog.DisplayRefreshLogScreen
import ink.trmnl.android.ui.settings.AppSettingsScreen
import ink.trmnl.android.util.CoilRequestUtils
import ink.trmnl.android.util.HTTP_429
import ink.trmnl.android.util.ImageSaver
import ink.trmnl.android.util.formatRefreshInterval
import ink.trmnl.android.util.nextRunTime
import ink.trmnl.android.work.TrmnlImageUpdateManager
import ink.trmnl.android.work.TrmnlWorkScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
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
        val rateLimitMessage: String? = null,
        val retryInfo: RetryInfo? = null,
        val configuredRefreshIntervalSec: Long? = null,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    /**
     * Information about an ongoing retry attempt.
     *
     * @param attempt Current retry attempt number (1-indexed)
     * @param maxRetries Maximum number of retries allowed
     * @param delaySeconds Delay in seconds before this retry
     * @param reason Human-readable reason for the retry
     */
    data class RetryInfo(
        val attempt: Int,
        val maxRetries: Int,
        val delaySeconds: Int,
        val reason: String,
    )

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

@AssistedInject
class TrmnlMirrorDisplayPresenter(
    @Assisted private val navigator: Navigator,
    private val trmnlDeviceConfigDataStore: TrmnlDeviceConfigDataStore,
    private val trmnlWorkScheduler: TrmnlWorkScheduler,
    private val imageMetadataStore: ImageMetadataStore,
    private val trmnlImageUpdateManager: TrmnlImageUpdateManager,
    private val rateLimitInterceptor: RateLimitInterceptor,
) : Presenter<TrmnlMirrorDisplayScreen.State> {
    companion object {
        private const val TAG = "MirrorDisplay"
    }

    @Composable
    override fun present(): TrmnlMirrorDisplayScreen.State {
        var imageUrl by remember { mutableStateOf<String?>(null) }
        var overlayControlsVisible by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(true) }
        var nextRefreshTime by remember { mutableStateOf("No scheduled work found. Please set API token.") }
        var lastImageTimestamp by remember { mutableStateOf<Long?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var saveImageResult by remember { mutableStateOf<TrmnlMirrorDisplayScreen.SaveImageResult?>(null) }
        var rateLimitMessage by remember { mutableStateOf<String?>(null) }
        var retryInfo by remember { mutableStateOf<TrmnlMirrorDisplayScreen.RetryInfo?>(null) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        // Load configured refresh rate in seconds (updated from server or periodic work)
        val configuredRefreshInterval by produceState<Long?>(initialValue = null) {
            trmnlDeviceConfigDataStore.refreshRateSecondsFlow.collect { rate ->
                Timber.tag(TAG).i(
                    "Observed configured refresh interval: $rate seconds " +
                        "(${rate?.let { formatRefreshInterval(it) } ?: "null"})",
                )
                value = rate
            }
        }

        // Collect retry events from RateLimitInterceptor for UI feedback
        LaunchedEffect(Unit) {
            rateLimitInterceptor.retryEvents.collect { event ->
                val reasonText =
                    if (event.reason == ink.trmnl.android.network.RateLimitInterceptor.REASON_RETRY_AFTER_HEADER) {
                        "Server requested"
                    } else {
                        "Rate limited"
                    }
                retryInfo =
                    TrmnlMirrorDisplayScreen.RetryInfo(
                        attempt = event.attempt,
                        maxRetries = event.maxRetries,
                        delaySeconds = (event.delayMs / 1000).toInt(),
                        reason = reasonText,
                    )
                Timber.tag(TAG).d("Retry info updated: attempt ${event.attempt}/${event.maxRetries}, delay ${event.delayMs}ms")
            }
        }

        // Monitor image metadata for rate limit status (HTTP 429) and update last timestamp
        LaunchedEffect(Unit) {
            imageMetadataStore.imageMetadataFlow.collect { metadata ->
                lastImageTimestamp = metadata?.timestamp
                Timber.tag(TAG).d(
                    "Image metadata update received: timestamp=${metadata?.timestamp}, " +
                        "statusCode=${metadata?.httpStatusCode}",
                )
                if (metadata?.httpStatusCode == HTTP_429) {
                    rateLimitMessage = "Showing cached image - rate limited by API server. Retrying in background..."
                    Timber.tag(TAG).d("Rate limit detected (HTTP 429), showing cached image with notification")
                } else {
                    rateLimitMessage = null
                }
            }
        }

        // Collect updates from the image update manager to get the latest image URL
        // Latest image URL is received from WorkManager work requests.
        LaunchedEffect(Unit) {
            trmnlImageUpdateManager.imageUpdateFlow.collect { imageMetadata ->
                Timber.tag(TAG).d("Received new image URL from TRMNL Image Update Manager: $imageMetadata")
                if (imageMetadata != null && imageMetadata.errorMessage == null) {
                    imageUrl = imageMetadata.url
                    isLoading = false
                    error = null
                    retryInfo = null // Clear retry info on successful load
                    lastImageTimestamp = imageMetadata.timestamp
                } else {
                    Timber.tag(TAG).w("Failed to get cached image URL from TRMNL Image Update Manager `imageUpdateFlow`")
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

        // Observe lifecycle for on-resume stale image catch-up
        val activity = LocalActivity.current as? ComponentActivity
        DisposableEffect(activity) {
            if (activity == null) return@DisposableEffect onDispose {}
            val observer =
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        Timber.tag(TAG).d("Activity ON_RESUME event received")
                        scope.launch {
                            val metadata = imageMetadataStore.imageMetadataFlow.firstOrNull()
                            val interval =
                                trmnlDeviceConfigDataStore.refreshRateSecondsFlow.firstOrNull()
                                    ?: metadata?.refreshIntervalSecs
                            if (metadata != null && interval != null && interval > 0) {
                                val ageMs = System.currentTimeMillis() - metadata.timestamp
                                val ageSec = ageMs / 1000
                                if (ageMs >= interval * 1000L && !isLoading) {
                                    Timber.tag(TAG).i(
                                        "Cached image is STALE on resume (age: ${ageSec}s >= interval: ${interval}s). " +
                                            "Triggering fresh fetch.",
                                    )
                                    trmnlWorkScheduler.triggerScheduledRefresh()
                                } else {
                                    Timber.tag(TAG).d(
                                        "Cached image is FRESH on resume (age: ${ageSec}s < interval: ${interval}s, " +
                                            "remaining: ${interval - ageSec}s).",
                                    )
                                }
                            }
                        }
                    }
                }
            activity.lifecycle.addObserver(observer)
            onDispose {
                activity.lifecycle.removeObserver(observer)
            }
        }

        // Active foreground display refresh ticker
        // When configuredRefreshInterval is present, runs an in-app timer to refresh the display
        // on exact interval (supporting 1m, 5m, etc.) while the display screen is open.
        LaunchedEffect(configuredRefreshInterval, lastImageTimestamp) {
            val interval = configuredRefreshInterval
            if (interval == null || interval <= 0) {
                Timber.tag(TAG).w("No configured refresh interval found; falling back to WorkManager scheduled info.")
                trmnlWorkScheduler.getScheduledWorkInfo().collect { workInfo ->
                    workInfo?.nextRunTime()?.let {
                        nextRefreshTime = it.timeUntilNextRefresh
                        Timber.tag(TAG).d("WorkManager next run time: ${it.timeUntilNextRefresh} (at ${it.nextRefreshOnDateTime})")
                    } ?: run {
                        nextRefreshTime = "No scheduled work found. Please set API token."
                    }
                }
                return@LaunchedEffect
            }

            Timber.tag(TAG).i("Starting active foreground display ticker: interval=${interval}s, lastImageTimestamp=$lastImageTimestamp")
            var lastLoggedRemainingSec = -1L

            while (isActive) {
                val now = System.currentTimeMillis()
                val lastTimestamp = lastImageTimestamp ?: now
                val nextRefreshTimeMillis = lastTimestamp + (interval * 1000L)
                val remainingMillis = nextRefreshTimeMillis - now

                if (remainingMillis <= 0) {
                    if (!isLoading) {
                        Timber.tag(TAG).i("Foreground interval elapsed ($interval s). Triggering scheduled refresh now!")
                        trmnlWorkScheduler.triggerScheduledRefresh()
                    } else {
                        Timber.tag(TAG).d(
                            "Foreground interval elapsed ($interval s), but refresh is already in progress " +
                                "(isLoading=true). Skipping duplicate trigger.",
                        )
                    }
                    nextRefreshTime = "any moment now"
                    delay(5_000L)
                } else {
                    val remainingSeconds = remainingMillis / 1000L
                    nextRefreshTime =
                        when {
                            remainingSeconds <= 0 -> "any moment now"
                            remainingSeconds < 60 -> "in $remainingSeconds seconds"
                            remainingSeconds < 3600 -> "in ${remainingMillis / 60_000L} minutes"
                            else -> "in ${remainingMillis / 3_600_000L} hours"
                        }

                    // Log on minute boundaries or when under 60s at 10s intervals
                    val shouldLog =
                        lastLoggedRemainingSec == -1L ||
                            (remainingSeconds < 60 && (remainingSeconds % 10L == 0L || remainingSeconds <= 5L)) ||
                            (remainingSeconds >= 60 && remainingSeconds / 60 != lastLoggedRemainingSec / 60)
                    if (shouldLog) {
                        Timber.tag(TAG).d("Foreground ticker countdown: $nextRefreshTime (${remainingSeconds}s remaining)")
                        lastLoggedRemainingSec = remainingSeconds
                    }

                    val delayMs = if (remainingSeconds < 60) 1_000L else 5_000L
                    delay(delayMs)
                }
            }
        }

        // Initialize by checking token and starting one-time work if needed
        LaunchedEffect(Unit) {
            val token = trmnlDeviceConfigDataStore.accessTokenFlow.firstOrNull()
            if (token.isNullOrBlank()) {
                Timber.tag(TAG).d("No access token found, navigating to configuration screen")
                navigator.goTo(AppSettingsScreen(returnToMirrorAfterSave = true))
                return@LaunchedEffect
            }

            // Check if we have a cached image
            val cachedMetadata = imageMetadataStore.imageMetadataFlow.firstOrNull()
            val hasValidImage = imageMetadataStore.hasValidImageUrlFlow.firstOrNull() ?: false
            Timber.tag(TAG).d("Initial check: hasValidImage=$hasValidImage, cachedTimestamp=${cachedMetadata?.timestamp}")
            if (hasValidImage && cachedMetadata != null) {
                // Initial loading state will be updated when imageUpdateFlow emits
                Timber.tag(TAG).d("Valid cached image URL exists in ImageMetadataStore")
                lastImageTimestamp = cachedMetadata.timestamp
                trmnlImageUpdateManager.initialize()

                // Check if cached image is already stale according to configured interval
                val interval =
                    trmnlDeviceConfigDataStore.refreshRateSecondsFlow.firstOrNull()
                        ?: cachedMetadata.refreshIntervalSecs
                if (interval != null && interval > 0) {
                    val ageMs = System.currentTimeMillis() - cachedMetadata.timestamp
                    val ageSec = ageMs / 1000
                    if (ageMs >= interval * 1000L) {
                        Timber.tag(TAG).i(
                            "Cached image is STALE on launch (age: ${ageSec}s >= interval: ${interval}s). " +
                                "Triggering immediate refresh.",
                        )
                        trmnlWorkScheduler.triggerScheduledRefresh()
                    } else {
                        Timber.tag(TAG).d(
                            "Cached image is FRESH on launch (age: ${ageSec}s < interval: ${interval}s, " +
                                "remaining: ${interval - ageSec}s).",
                        )
                    }
                }
            } else {
                Timber.tag(TAG).d("No valid cached image, starting one-time refresh work")
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
            rateLimitMessage = rateLimitMessage,
            retryInfo = retryInfo,
            configuredRefreshIntervalSec = configuredRefreshInterval,
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
                is TrmnlMirrorDisplayScreen.SaveImageResult.Success -> {
                    snackbarHostState.showSnackbar(
                        message = "Image saved to Pictures/TRMNL",
                        duration = SnackbarDuration.Short,
                    )
                }

                is TrmnlMirrorDisplayScreen.SaveImageResult.Error -> {
                    snackbarHostState.showSnackbar(
                        message = result.message,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    // Show snackbar when rate limit message is present
    LaunchedEffect(state.rateLimitMessage) {
        state.rateLimitMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
            )
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp),
                ) {
                    CircularProgressIndicator()

                    // Show retry information if available
                    state.retryInfo?.let { retry ->
                        Text(
                            text = "${retry.reason} - Retrying in ${retry.delaySeconds}s",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        Text(
                            text = "Attempt ${retry.attempt} of ${retry.maxRetries}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
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
                            val throwable = error.result.throwable
                            Timber.e("Image loading failed: $throwable")

                            // Check if this is an HTTP 403 error (expired URL)
                            if (throwable is HttpException && throwable.response.code == 403) {
                                Timber.w("Image URL expired (HTTP 403), triggering refresh to get fresh URL")
                                // Trigger refresh to get a fresh image URL from the server
                                state.eventSink(TrmnlMirrorDisplayScreen.Event.RefreshCurrentPlaylistItemRequested)
                            } else {
                                // For other errors, show the error message
                                state.eventSink(
                                    TrmnlMirrorDisplayScreen.Event.ImageLoadingError(
                                        "Failed to load image: ${throwable.message ?: "Unknown error"}",
                                    ),
                                )
                            }
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
                    retryInfo = null,
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
                    retryInfo = null,
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
                    retryInfo = null,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "TRMNL Display Retrying Preview")
@Composable
fun PreviewTrmnlMirrorDisplayRetryingContent() {
    Surface {
        TrmnlMirrorDisplayContent(
            state =
                TrmnlMirrorDisplayScreen.State(
                    imageUrl = null,
                    overlayControlsVisible = false,
                    nextImageRefreshIn = "5 minutes",
                    isLoading = true,
                    errorMessage = null,
                    saveImageResult = null,
                    retryInfo =
                        TrmnlMirrorDisplayScreen.RetryInfo(
                            attempt = 2,
                            maxRetries = 5,
                            delaySeconds = 6,
                            reason = "Rate limited",
                        ),
                    eventSink = {},
                ),
        )
    }
}
