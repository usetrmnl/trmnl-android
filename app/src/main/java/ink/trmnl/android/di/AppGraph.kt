package ink.trmnl.android.di

import android.content.Context
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import ink.trmnl.android.work.TrmnlWorkerFactory

/**
 * Metro dependency graph for the application.
 *
 * - [MetroAppComponentProviders] enables constructor injection of Activities via
 *   [dev.zacsweers.metrox.android.MetroAppComponentFactory].
 * - [TrmnlWorkerFactory] is exposed so [ink.trmnl.android.TrmnlDisplayMirrorApp] can
 *   configure WorkManager.
 */
@DependencyGraph(AppScope::class)
interface AppGraph : MetroAppComponentProviders {
    val workerFactory: TrmnlWorkerFactory

    /**
     * Re-exposes the raw [Context] with the [ApplicationContext] qualifier so that
     * module contributors can inject it by qualifier.
     */
    @Provides
    @ApplicationContext
    fun provideApplicationContext(context: Context): Context = context

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides context: Context,
        ): AppGraph
    }
}
