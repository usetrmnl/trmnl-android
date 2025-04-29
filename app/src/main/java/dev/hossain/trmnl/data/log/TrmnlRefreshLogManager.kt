package dev.hossain.trmnl.data.log

import android.content.Context
import androidx.datastore.core.DataStore
import com.squareup.anvil.annotations.optional.SingleIn
import dev.hossain.trmnl.di.AppScope
import dev.hossain.trmnl.di.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

@SingleIn(AppScope::class)
class TrmnlRefreshLogManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataStore: DataStore<TrmnlRefreshLogs>,
    ) {
        companion object {
            const val MAX_LOG_ENTRIES = 100
        }

        val logsFlow: Flow<List<TrmnlRefreshLog>> =
            dataStore.data
                .catch { e ->
                    Timber.e(e, "Error reading logs")
                    emit(TrmnlRefreshLogs())
                }.map { it.logs }

        suspend fun addSuccessLog(
            imageUrl: String,
            imageName: String,
            refreshIntervalSeconds: Long?,
            imageRefreshWorkType: String?,
        ) {
            addLog(TrmnlRefreshLog.createSuccess(imageUrl, imageName, refreshIntervalSeconds, imageRefreshWorkType))
        }

        suspend fun addFailureLog(error: String) {
            addLog(TrmnlRefreshLog.createFailure(error))
        }

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

        suspend fun clearLogs() {
            dataStore.updateData {
                TrmnlRefreshLogs(emptyList())
            }
        }
    }
