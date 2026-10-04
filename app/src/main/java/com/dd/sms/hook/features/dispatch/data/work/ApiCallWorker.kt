package com.dd.sms.hook.features.dispatch.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.dd.sms.hook.shared.domain.constants.WorkConstants
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.dispatch.domain.model.DispatchOutcome
import com.dd.sms.hook.features.dispatch.domain.usecase.ExecuteQueuedCallUseCase
import com.dd.sms.hook.features.dispatch.platform.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "ApiCallWorker"

@HiltWorker
class ApiCallWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val executeQueuedCall: ExecuteQueuedCallUseCase,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val configId: Long = inputData.getLong(WorkConstants.KEY_CONFIG_ID, -1L)
        val smsId: Long = inputData.getLong(WorkConstants.KEY_SMS_ID, -1L)
        val trigger: CallTrigger = CallTrigger.entries
            .firstOrNull { it.name == inputData.getString(WorkConstants.KEY_TRIGGER) } ?: CallTrigger.SMS
        val attempt: Int = runAttemptCount + 1

        return try {
            when (val outcome: DispatchOutcome = executeQueuedCall(configId, smsId, attempt, trigger)) {
                is DispatchOutcome.Success -> Result.success()
                is DispatchOutcome.Skipped -> Result.success()
                is DispatchOutcome.Failed -> if (outcome.willRetry) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "worker crashed - {configId: $configId, smsId: $smsId, attempt: $attempt}", e)
            Result.failure()
        }
    }

    /** Needed for expedited work on Android 11 and below, where it runs as a foreground service. */
    override suspend fun getForegroundInfo(): ForegroundInfo = notificationHelper.workForegroundInfo()
}
