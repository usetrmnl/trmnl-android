package dev.hossain.trmnl.di

import android.app.Activity
import android.content.Context
import com.squareup.anvil.annotations.MergeComponent
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.BindsInstance
import dev.hossain.trmnl.TrmnlDisplayMirrorApp
import javax.inject.Provider

@MergeComponent(
    scope = AppScope::class,
    modules = [CircuitModule::class],
)
@SingleIn(AppScope::class)
interface AppComponent {
    val activityProviders: Map<Class<out Activity>, @JvmSuppressWildcards Provider<Activity>>

    /**
     * Injects dependencies into [TrmnlDisplayMirrorApp].
     */
    fun inject(app: TrmnlDisplayMirrorApp)

    @MergeComponent.Factory
    interface Factory {
        fun create(
            @ApplicationContext @BindsInstance context: Context,
        ): AppComponent
    }

    companion object {
        fun create(context: Context): AppComponent = DaggerAppComponent.factory().create(context)
    }
}
