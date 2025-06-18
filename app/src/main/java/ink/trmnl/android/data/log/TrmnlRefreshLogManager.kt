package ink.trmnl.android.data.log

import android.content.Context
import androidx.datastore.core.DataStore
import com.squareup.anvil.annotations.optional.SingleIn
import ink.trmnl.android.data.AppConfig.MAX_LOG_ENTRIES
import ink.trmnl.android.data.HttpResponseMetadata
import ink.trmnl.android.di.AppScope
import ink.trmnl.android.di.ApplicationContext
import ink.trmnl.android.model.TrmnlDeviceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Manages logs related to terminal refresh operations.
 * Provides functionality to add successful and failed refresh logs,
 * access log data through Flow, and clear logs when needed.
 */
@SingleIn(AppScope::class)
class TrmnlRefreshLogManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataStore: DataStore<TrmnlRefreshLogs>,
    ) {
        /**
         * Flow of terminal refresh logs ordered from newest to oldest.
         * Handles errors by emitting an empty log list and logging the exception.
         */
        val logsFlow: Flow<List<TrmnlRefreshLog>> =
            dataStore.data
                .catch { e ->
                    Timber.e(e, "Error reading logs")
                    emit(TrmnlRefreshLogs())
                }.map { it.logs }

        /**
         * Records a successful image refresh operation.
         */
        suspend fun addSuccessLog(
            trmnlDeviceType: TrmnlDeviceType,
            imageUrl: String,
            imageName: String,
            refreshIntervalSeconds: Long?,
            imageRefreshWorkType: String?,
            httpResponseMetadata: HttpResponseMetadata? = null,
        ) {
            addLog(
                TrmnlRefreshLog.createSuccess(
                    trmnlDeviceType = trmnlDeviceType,
                    imageUrl = imageUrl,
                    imageName = imageName,
                    refreshIntervalSeconds = refreshIntervalSeconds,
                    imageRefreshWorkType = imageRefreshWorkType,
                    httpResponseMetadata = httpResponseMetadata,
                ),
            )
        }

        /**
         * Records a failed image refresh operation.
         */
        suspend fun addFailureLog(
            error: String,
            httpResponseMetadata: HttpResponseMetadata? = null,
        ) {
            addLog(TrmnlRefreshLog.createFailure(error, httpResponseMetadata))
        }

        /**
         * Adds a log entry to the beginning of the log list and trims older entries if necessary.
         * Maintains a maximum number of log entries defined by [MAX_LOG_ENTRIES].
         */
        internal suspend fun addLog(log: TrmnlRefreshLog) {
            dataStore.updateData { currentLogs ->
                val updatedLogs =
                    currentLogs.logs.toMutableList().apply {
                        add(0, log) // Add to the beginning for descending order
                        if (size > MAX_LOG_ENTRIES) {
                            // Keep only the most recent logs
                            removeAll(subList(MAX_LOG_ENTRIES, size))
                        }
                    }
                TrmnlRefreshLogs(updatedLogs)
            }
        }

        /**
         * Removes all logs from storage.
         */
        suspend fun clearLogs() {
            dataStore.updateData {
                TrmnlRefreshLogs(emptyList())
            }
        }
    }
