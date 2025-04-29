package dev.hossain.trmnl.ui.refreshlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.hossain.trmnl.BuildConfig
import dev.hossain.trmnl.R
import dev.hossain.trmnl.data.AppConfig.DEFAULT_REFRESH_INTERVAL_SEC
import dev.hossain.trmnl.data.log.TrmnlRefreshLog
import dev.hossain.trmnl.data.log.TrmnlRefreshLogManager
import dev.hossain.trmnl.di.AppScope
import dev.hossain.trmnl.ui.theme.TrmnlDisplayAppTheme
import dev.hossain.trmnl.util.getTimeElapsedString
import dev.hossain.trmnl.work.RefreshWorkType
import dev.hossain.trmnl.work.TrmnlWorkScheduler
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * A screen that displays the refresh logs of the TRMNL display.
 * This is meant to validate how often the refresh rate is set and when the image is updated.
 *
 * The screen provides functionality to:
 * - View chronological logs of display refresh attempts
 * - See detailed information about successful and failed refresh operations
 * - Clear logs (via action button)
 * - Add test logs (debug builds only)
 */
@Parcelize
data object DisplayRefreshLogScreen : Screen {
    /**
     * Represents the UI state for the [DisplayRefreshLogScreen].
     */
    data class State(
        val logs: List<TrmnlRefreshLog>,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    /**
     * Events that can be triggered from the DisplayRefreshLogScreen UI.
     */
    sealed class Event : CircuitUiEvent {
        /**
         * Event triggered when the user presses the back button.
         */
        data object BackPressed : Event()

        /**
         * Event triggered when the user requests to clear all logs.
         */
        data object ClearLogs : Event()

        /**
         * Event triggered when a test success log should be added (debug only).
         */
        data object AddSuccessLog : Event()

        /**
         * Event triggered when a test failure log should be added (debug only).
         */
        data object AddFailLog : Event()

        /**
         * Event triggered when the refresh worker should be started (debug only).
         */
        data object StartRefreshWorker : Event()
    }
}

/**
 * Presenter for the DisplayRefreshLogScreen.
 * Manages the screen's state and handles events from the UI.
 */
class DisplayRefreshLogPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val activityLogManager: TrmnlRefreshLogManager,
        private val trmnlWorkScheduler: TrmnlWorkScheduler,
    ) : Presenter<DisplayRefreshLogScreen.State> {
        /**
         * Creates and returns the state for the DisplayRefreshLogScreen.
         * Collects logs from the log manager and sets up event handling.
         *
         * @return The current UI state.
         */
        @Composable
        override fun present(): DisplayRefreshLogScreen.State {
            val logs by activityLogManager.logsFlow.collectAsState(initial = emptyList())
            val scope = rememberCoroutineScope()

            return DisplayRefreshLogScreen.State(
                logs = logs,
                eventSink = { event ->
                    when (event) {
                        DisplayRefreshLogScreen.Event.BackPressed -> navigator.pop()
                        DisplayRefreshLogScreen.Event.ClearLogs -> {
                            scope.launch {
                                activityLogManager.clearLogs()
                            }
                        }

                        DisplayRefreshLogScreen.Event.AddFailLog -> {
                            scope.launch {
                                activityLogManager.addLog(
                                    TrmnlRefreshLog.createFailure(error = "Test failure"),
                                )
                            }
                        }
                        DisplayRefreshLogScreen.Event.AddSuccessLog -> {
                            scope.launch {
                                activityLogManager.addLog(
                                    TrmnlRefreshLog.createSuccess(
                                        imageUrl = "https://debug.example.com/image.png",
                                        imageName = "test-image.png",
                                        refreshIntervalSeconds = 300L,
                                        imageRefreshWorkType = RefreshWorkType.ONE_TIME.name,
                                    ),
                                )
                            }
                        }

                        DisplayRefreshLogScreen.Event.StartRefreshWorker -> {
                            trmnlWorkScheduler.startOneTimeImageRefreshWork()
                        }
                    }
                },
            )
        }

        /**
         * Factory interface for creating DisplayRefreshLogPresenter instances.
         */
        @CircuitInject(DisplayRefreshLogScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): DisplayRefreshLogPresenter
        }
    }

/**
 * Main composable function for rendering the DisplayRefreshLogScreen.
 * Sets up the screen's structure including toolbar, log list, and debug controls.
 */
@CircuitInject(DisplayRefreshLogScreen::class, AppScope::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayRefreshLogContent(
    state: DisplayRefreshLogScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Display Refresh Logs") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(DisplayRefreshLogScreen.Event.BackPressed) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { state.eventSink(DisplayRefreshLogScreen.Event.ClearLogs) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear logs")
                    }
                },
            )
        },
        bottomBar = {
            // Debug controls only visible in debug builds
            if (BuildConfig.DEBUG) {
                DebugControls(
                    onAddSuccessLog = {
                        state.eventSink(DisplayRefreshLogScreen.Event.AddSuccessLog)
                    },
                    onAddFailLog = {
                        state.eventSink(DisplayRefreshLogScreen.Event.AddFailLog)
                    },
                    onStartRefreshWorker = {
                        state.eventSink(DisplayRefreshLogScreen.Event.StartRefreshWorker)
                    },
                    // Use a modifier that takes navigation bar padding into account
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        },
        // Use WindowInsets.navigationBars to ensure content doesn't overlap with the navigation bar
        contentWindowInsets = WindowInsets.navigationBars,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (state.logs.isEmpty()) {
                Text(
                    text = "No logs available",
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    items(state.logs) { log ->
                        LogItemView(log = log)
                    }
                }
            }
        }
    }
}

/**
 * Composable function that renders a single log entry as a card.
 * Displays different information based on whether the log represents a success or failure.
 */
@Composable
private fun LogItemView(
    log: TrmnlRefreshLog,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Format timestamp
            val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(log.timestamp))

            // Calculate time elapsed
            val timeElapsed = getTimeElapsedString(log.timestamp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = timeElapsed,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (log.success) Icons.Default.CheckCircle else Icons.Default.Clear,
                        contentDescription = if (log.success) "Success" else "Failed",
                        tint = if (log.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        text = if (log.success) "Success" else "Failed",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (log.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (log.success) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.image_24dp),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )

                    Text(
                        text = "Image File Name:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = log.imageName ?: "N/A",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (log.refreshIntervalSeconds != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.timer_24dp),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )

                        Text(
                            text =
                                "API Refresh Rate: " +
                                    when {
                                        log.refreshIntervalSeconds > 60 -> "${log.refreshIntervalSeconds / 60} minutes"
                                        else -> "${log.refreshIntervalSeconds} seconds"
                                    },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (log.imageRefreshWorkType != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Icon(
                            painter =
                                when (log.imageRefreshWorkType) {
                                    RefreshWorkType.ONE_TIME.name -> painterResource(R.drawable.counter_1_24dp)
                                    RefreshWorkType.PERIODIC.name -> painterResource(R.drawable.time_auto_24dp)
                                    else -> painterResource(R.drawable.question_mark_24dp)
                                },
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )

                        Text(
                            text =
                                when (log.imageRefreshWorkType) {
                                    RefreshWorkType.ONE_TIME.name -> "Refresh Type: Manual One-time Refresh"
                                    RefreshWorkType.PERIODIC.name -> "Refresh Type: Automatic Scheduled Refresh"
                                    else -> "Unknown (${log.imageRefreshWorkType})"
                                },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                Text(
                    text = "Error:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = log.error ?: "Unknown error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Debug controls for manually adding test logs. Only visible in debug builds.
 */
@Composable
private fun DebugControls(
    onAddSuccessLog: () -> Unit,
    onAddFailLog: () -> Unit,
    onStartRefreshWorker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
    ) {
        Text(
            text = "Debug Controls (for testing)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = onAddSuccessLog,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            ) {
                Text("Add Success Log")
            }

            Button(
                onClick = onAddFailLog,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("Add Fail Log")
            }
        }

        Button(
            onClick = onStartRefreshWorker,
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                ),
        ) {
            Text("Start Refresh Worker")
        }
    }
}

@Preview(name = "Display Refresh Log Content - Empty")
@Composable
private fun PreviewDisplayRefreshLogContentEmpty() {
    TrmnlDisplayAppTheme {
        DisplayRefreshLogContent(
            state =
                DisplayRefreshLogScreen.State(
                    logs = emptyList(),
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "Display Refresh Log Content - With Logs")
@Composable
private fun PreviewDisplayRefreshLogContentWithLogs() {
    val sampleLogs =
        listOf(
            TrmnlRefreshLog(
                timestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5),
                success = true,
                imageUrl = "https://example.com/image1.png",
                imageName = "preview-image.bmp",
                refreshIntervalSeconds = DEFAULT_REFRESH_INTERVAL_SEC,
                imageRefreshWorkType = RefreshWorkType.PERIODIC.name,
                error = null,
            ),
            TrmnlRefreshLog(
                timestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
                success = false,
                imageUrl = null,
                imageName = "preview-image.bmp",
                refreshIntervalSeconds = DEFAULT_REFRESH_INTERVAL_SEC,
                imageRefreshWorkType = RefreshWorkType.ONE_TIME.name,
                error = "Failed to connect to server (HTTP 500)",
            ),
            TrmnlRefreshLog(
                timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
                success = true,
                imageUrl = "https://example.com/image2.png",
                imageName = "preview-image.bmp",
                refreshIntervalSeconds = DEFAULT_REFRESH_INTERVAL_SEC,
                imageRefreshWorkType = RefreshWorkType.PERIODIC.name,
                error = null,
            ),
        )
    TrmnlDisplayAppTheme {
        DisplayRefreshLogContent(
            state =
                DisplayRefreshLogScreen.State(
                    logs = sampleLogs,
                    eventSink = {},
                ),
        )
    }
}

@Preview(name = "Log Item View - Success")
@Composable
private fun PreviewLogItemViewSuccess() {
    val log =
        TrmnlRefreshLog(
            timestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10),
            success = true,
            imageUrl = "https://images.unsplash.com/photo-1617591897383-14876a3e6d1a",
            imageName = "preview-image.bmp",
            refreshIntervalSeconds = DEFAULT_REFRESH_INTERVAL_SEC,
            imageRefreshWorkType = RefreshWorkType.PERIODIC.name,
            error = null,
        )
    TrmnlDisplayAppTheme {
        LogItemView(log = log)
    }
}

@Preview(name = "Log Item View - Failure")
@Composable
private fun PreviewLogItemViewFailure() {
    val log =
        TrmnlRefreshLog(
            timestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30),
            success = false,
            imageUrl = null,
            imageName = "preview-image.bmp",
            refreshIntervalSeconds = DEFAULT_REFRESH_INTERVAL_SEC,
            imageRefreshWorkType = RefreshWorkType.ONE_TIME.name,
            error = "Network timeout while fetching image.",
        )
    TrmnlDisplayAppTheme {
        LogItemView(log = log)
    }
}
