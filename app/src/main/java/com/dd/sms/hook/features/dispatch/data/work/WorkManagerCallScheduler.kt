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
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dd.sms.hook.shared.domain.constants.WorkConstants
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.dispatch.domain.model.QueueState
import com.dd.sms.hook.features.dispatch.domain.model.QueuedCall
import com.dd.sms.hook.features.dispatch.domain.service.CallScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
            .addTag(WorkConstants.TAG_PREFIX_CONFIG + configId)
            .addTag(WorkConstants.TAG_PREFIX_SMS + smsId)
            .addTag(WorkConstants.TAG_PREFIX_TRIGGER + trigger.name)
            .build()

        workManager.enqueue(request)
        AppLogger.i(TAG, "enqueued - {work: ${request.id}, configId: $configId, smsId: $smsId, trigger: $trigger}")
    }

    override fun observeQueue(): Flow<List<QueuedCall>> =
        workManager.getWorkInfosByTagFlow(WorkConstants.TAG_API_CALL).map { infos ->
            infos.filterNot { it.state.isFinished }.map { it.toQueuedCall() }
        }

    fun schedulePeriodicCleanup() {
        val request: PeriodicWorkRequest = PeriodicWorkRequestBuilder<LogCleanupWorker>(
            WorkConstants.CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(WorkConstants.UNIQUE_LOG_CLEANUP, ExistingPeriodicWorkPolicy.KEEP, request)
        AppLogger.i(TAG, "cleanup scheduled - {everyHours: ${WorkConstants.CLEANUP_INTERVAL_HOURS}}")
    }
}

private fun WorkInfo.toQueuedCall(): QueuedCall {
    val trigger: CallTrigger = CallTrigger.entries
        .firstOrNull { it.name == tagValue(WorkConstants.TAG_PREFIX_TRIGGER) } ?: CallTrigger.SMS

    return QueuedCall(
        workId = id.toString(),
        configId = tagValue(WorkConstants.TAG_PREFIX_CONFIG)?.toLongOrNull(),
        smsId = tagValue(WorkConstants.TAG_PREFIX_SMS)?.toLongOrNull(),
        trigger = trigger,
        state = if (state == WorkInfo.State.RUNNING) QueueState.RUNNING else QueueState.WAITING,
        attempt = runAttemptCount + 1,
        nextRunAt = nextScheduleTimeMillis.takeIf { state == WorkInfo.State.ENQUEUED && runAttemptCount > 0 && it != Long.MAX_VALUE },
    )
}

private fun WorkInfo.tagValue(prefix: String): String? =
    tags.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)
