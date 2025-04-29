package dev.hossain.trmnl.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides
import dev.hossain.trmnl.data.log.TrmnlRefreshLogSerializer
import dev.hossain.trmnl.data.log.TrmnlRefreshLogs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@ContributesTo(AppScope::class)
object DataStoreModule {
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
