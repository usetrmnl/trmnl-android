package ink.trmnl.android.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds

/**
 * Dagger module that provides dependencies for the Circuit framework.
 */
@ContributesTo(AppScope::class)
@Module
interface CircuitModule {
    /**
     * Dagger multi-binding method that provides a set of Presenter.Factory instances.
     */
    @Multibinds fun presenterFactories(): Set<Presenter.Factory>

    /**
     * Dagger multi-binding method that provides a set of Ui.Factory instances.
     */
    @Multibinds fun viewFactories(): Set<Ui.Factory>

    companion object {
        /**
         * Provides a singleton instance of Circuit with presenter and ui configured.
         */
        @SingleIn(AppScope::class)
        @Provides
        fun provideCircuit(
            presenterFactories: @JvmSuppressWildcards Set<Presenter.Factory>,
            uiFactories: @JvmSuppressWildcards Set<Ui.Factory>,
        ): Circuit =
            Circuit
                .Builder()
                .addPresenterFactories(presenterFactories)
                .addUiFactories(uiFactories)
                .build()
    }
}
