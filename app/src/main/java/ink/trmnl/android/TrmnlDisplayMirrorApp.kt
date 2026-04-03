package ink.trmnl.android

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.android.MetroApplication
import ink.trmnl.android.di.AppGraph
import timber.log.Timber

/**
 * Application class for the app with key initializations.
 */
class TrmnlDisplayMirrorApp :
    Application(),
    MetroApplication,
    Configuration.Provider {
    private val appGraph by lazy { createGraphFactory<AppGraph.Factory>().create(this) }

    override val appComponentProviders: MetroAppComponentProviders
        get() = appGraph

    override val workManagerConfiguration: Configuration
        get() {
            Timber.i("Setting up custom WorkManager configuration")
            return Configuration
                .Builder()
                .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.WARN)
                .setWorkerFactory(appGraph.workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        installLoggingTree()
    }

    private fun installLoggingTree() {
        if (BuildConfig.DEBUG) {
            // Plant a debug tree for development builds
            Timber.plant(Timber.DebugTree())
        } else {
            // Enable logging in release builds during early development
            // This will help us debug issues in production builds
            // ℹ️ In future we will remove this and install crashlytics tree
            Timber.plant(Timber.DebugTree())
        }
    }
}
