package ink.trmnl.android.di

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides
import ink.trmnl.android.work.TrmnlWorkerFactory

@Module
@ContributesTo(AppScope::class)
object WorkerModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideWorkManager(
        @ApplicationContext context: Context,
        factory: TrmnlWorkerFactory,
    ): WorkManager {
        val config =
            Configuration
                .Builder()
                .setWorkerFactory(factory)
                .build()

        WorkManager.initialize(context, config)
        return WorkManager.getInstance(context)
    }
}
