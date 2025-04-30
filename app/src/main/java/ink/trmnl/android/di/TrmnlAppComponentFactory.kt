package ink.trmnl.android.di

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.annotation.Keep
import androidx.core.app.AppComponentFactory
import ink.trmnl.android.TrmnlDisplayMirrorApp
import javax.inject.Provider

/**
 * Custom implementation of [AppComponentFactory] used to inject Android components
 * (specifically Activities) via Dagger using constructor injection. This factory
 * allows the Android system to delegate activity instantiation to Dagger's dependency
 * graph, enabling constructor injection instead of field injection.
 *
 * This class is referenced in the `AndroidManifest` within the `<application>` tag.
 *
 * Usage:
 * Add the following to your AndroidManifest.xml:
 *
 * ```xml
 * <application
 *     android:appComponentFactory=".di.ComposeAppComponentFactory"
 *     ... />
 * ```
 */
@Keep
class TrmnlAppComponentFactory : AppComponentFactory() {
    /**
     * Retrieves an instance of the specified class (typically an Activity) from the provided
     * Dagger providers map. If a provider exists for the class, it uses that provider to
     * obtain the instance; otherwise, it returns null.
     *
     * @param T The type of the class being retrieved.
     * @param classLoader The ClassLoader used to load the class.
     * @param className The fully qualified name of the class to be instantiated.
     * @param providers A map containing Dagger providers for the available classes.
     * @return The instance of the class if found in the providers map, or null if not.
     */
    private inline fun <reified T> getInstance(
        classLoader: ClassLoader,
        className: String,
        providers: Map<Class<out T>, @JvmSuppressWildcards Provider<T>>,
    ): T? {
        // Load the class using the provided ClassLoader and attempt to retrieve the instance.
        val clazz = Class.forName(className, false, classLoader).asSubclass(T::class.java)
        val modelProvider = providers[clazz] ?: return null
        return modelProvider.get() as T
    }

    /**
     * Called by the Android system to instantiate an activity. This method checks if the
     * activity can be provided by the Dagger component. If the Dagger component can provide
     * the activity, it returns the injected instance; otherwise, it falls back to the
     * default system behavior.
     *
     * @param classLoader The ClassLoader used to load the activity class.
     * @param className The fully qualified name of the activity to be instantiated.
     * @param intent The intent that was used to start the activity.
     * @return The activity instance, either from Dagger or from the system.
     */
    override fun instantiateActivityCompat(
        classLoader: ClassLoader,
        className: String,
        intent: Intent?,
    ): Activity =
        getInstance(classLoader, className, activityProviders)
            ?: super.instantiateActivityCompat(classLoader, className, intent)

    /**
     * Called by the Android system to instantiate the Application class. This method
     * initializes the Dagger component and retrieves the map of activity providers,
     * which are used later for activity injection.
     *
     * @param classLoader The ClassLoader used to load the Application class.
     * @param className The fully qualified name of the Application class.
     * @return The Application instance.
     */
    override fun instantiateApplicationCompat(
        classLoader: ClassLoader,
        className: String,
    ): Application {
        val app = super.instantiateApplicationCompat(classLoader, className)
        // Retrieve the Dagger app component and the activity providers from it
        activityProviders = (app as TrmnlDisplayMirrorApp).appComponent().activityProviders
        return app
    }

    /**
     * Companion object to store activity providers. This object holds the Dagger-provided
     * map of activity classes to their corresponding providers. It's used to inject activities
     * upon instantiation.
     *
     * This map is initialized when the application is created via the Dagger component.
     */
    companion object {
        private lateinit var activityProviders: Map<Class<out Activity>, Provider<Activity>>
    }
}
