package ink.trmnl.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ink.trmnl.android.data.log.TrmnlRefreshLogSerializer
import ink.trmnl.android.data.log.TrmnlRefreshLogs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@ContributesTo(AppScope::class)
interface DataStoreModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideActivityLogDataStore(
        @ApplicationContext context: Context,
    ): DataStore<TrmnlRefreshLogs> =
        DataStoreFactory.create(
            serializer = TrmnlRefreshLogSerializer,
            produceFile = { context.dataStoreFile("trmnl_activity_logs.json") },
            corruptionHandler = null,
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        )
}
