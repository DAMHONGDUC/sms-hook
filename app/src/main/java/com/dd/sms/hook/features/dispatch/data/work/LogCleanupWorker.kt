package com.dd.sms.hook.features.dispatch.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.dispatch.domain.usecase.PruneHistoryUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "LogCleanupWorker"

@HiltWorker
class LogCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pruneHistory: PruneHistoryUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        try {
            pruneHistory()
            Result.success()
        } catch (e: Exception) {
            AppLogger.e(TAG, "cleanup failed", e)
            Result.retry()
        }
}
