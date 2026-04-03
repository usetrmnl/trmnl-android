package ink.trmnl.android.di

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ink.trmnl.android.work.TrmnlWorkerFactory

@ContributesTo(AppScope::class)
interface WorkerModule {
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
