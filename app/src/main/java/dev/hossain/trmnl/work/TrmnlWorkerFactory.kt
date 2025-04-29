package dev.hossain.trmnl.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.squareup.anvil.annotations.optional.SingleIn
import dev.hossain.trmnl.di.AppScope
import javax.inject.Inject

@SingleIn(AppScope::class)
class TrmnlWorkerFactory
    @Inject
    constructor(
        private val imageRefreshWorkerFactory: TrmnlImageRefreshWorker.Factory,
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker? =
            when (workerClassName) {
                TrmnlImageRefreshWorker::class.java.name ->
                    imageRefreshWorkerFactory.create(appContext, workerParameters)
                else -> null
            }
    }
