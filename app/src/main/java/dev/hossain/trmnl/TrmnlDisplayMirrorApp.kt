package dev.hossain.trmnl

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import dev.hossain.trmnl.di.AppComponent
import dev.hossain.trmnl.work.TrmnlWorkerFactory
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for the app with key initializations.
 */
class TrmnlDisplayMirrorApp :
    Application(),
    Configuration.Provider {
    private val appComponent: AppComponent by lazy { AppComponent.create(this) }

    fun appComponent(): AppComponent = appComponent

    @Inject
    lateinit var workerFactory: TrmnlWorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            Timber.i("Setting up custom WorkManager configuration")
            return Configuration
                .Builder()
                .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.WARN)
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        installLoggingTree()
        appComponent.inject(this)
    }

    private fun installLoggingTree() {
        if (BuildConfig.DEBUG) {
            // Plant a debug tree for development builds
            Timber.plant(Timber.DebugTree())
        }
    }
}
