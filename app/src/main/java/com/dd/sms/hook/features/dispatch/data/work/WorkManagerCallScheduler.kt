package com.dd.sms.hook.features.dispatch.data.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dd.sms.hook.shared.domain.constants.WorkConstants
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.dispatch.domain.service.CallScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CallScheduler"

@Singleton
class WorkManagerCallScheduler @Inject constructor(
    private val workManager: WorkManager,
) : CallScheduler {
    private val networkConstraint: Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun enqueue(configId: Long, smsId: Long, trigger: CallTrigger) {
        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<ApiCallWorker>()
            .setInputData(
                workDataOf(
                    WorkConstants.KEY_CONFIG_ID to configId,
                    WorkConstants.KEY_SMS_ID to smsId,
                    WorkConstants.KEY_TRIGGER to trigger.name,
                )
            )
            .setConstraints(networkConstraint)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkConstants.BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(WorkConstants.TAG_API_CALL)
            .build()

        workManager.enqueue(request)
        AppLogger.i(TAG, "enqueued - {work: ${request.id}, configId: $configId, smsId: $smsId, trigger: $trigger}")
    }

    fun schedulePeriodicCleanup() {
        val request: PeriodicWorkRequest = PeriodicWorkRequestBuilder<LogCleanupWorker>(
            WorkConstants.CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(WorkConstants.UNIQUE_LOG_CLEANUP, ExistingPeriodicWorkPolicy.KEEP, request)
        AppLogger.i(TAG, "cleanup scheduled - {everyHours: ${WorkConstants.CLEANUP_INTERVAL_HOURS}}")
    }
}
