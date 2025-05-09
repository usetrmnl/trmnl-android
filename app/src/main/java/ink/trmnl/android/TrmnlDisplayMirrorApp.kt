package ink.trmnl.android

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import ink.trmnl.android.di.AppComponent
import ink.trmnl.android.work.TrmnlWorkerFactory
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
        } else {
            // Enable logging in release builds during early development
            // This will help us debug issues in production builds
            // ℹ️ In future we will remove this and install crashlytics tree
            Timber.plant(Timber.DebugTree())
        }
    }
}
