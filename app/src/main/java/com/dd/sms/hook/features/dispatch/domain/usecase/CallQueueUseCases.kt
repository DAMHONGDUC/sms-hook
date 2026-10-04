package com.dd.sms.hook.features.dispatch.domain.usecase

import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.repository.ApiConfigRepository
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.calllog.domain.repository.CallLogRepository
import com.dd.sms.hook.features.dispatch.domain.model.QueueState
import com.dd.sms.hook.features.dispatch.domain.model.QueuedCall
import com.dd.sms.hook.features.dispatch.domain.model.QueuedCallItem
import com.dd.sms.hook.features.dispatch.domain.repository.ReceivedSmsRepository
import com.dd.sms.hook.features.dispatch.domain.service.CallScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

private const val TAG = "CallQueueUseCases"

/** Read-only view of the delivery queue, optionally for one API: running first, then by next run time. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveCallQueueUseCase @Inject constructor(
    private val scheduler: CallScheduler,
    private val configRepository: ApiConfigRepository,
    private val smsRepository: ReceivedSmsRepository,
) {
    operator fun invoke(configId: Long?): Flow<List<QueuedCallItem>> =
        combine(scheduler.observeQueue(), configRepository.observeAll()) { queue, configs -> queue to configs }
            .mapLatest { (queue, configs) ->
                val names: Map<Long, String> = configs.associate { config: ApiConfig -> config.id to config.name }

                queue.filter { configId == null || it.configId == configId }
                    .sortedWith(compareBy<QueuedCall> { it.state != QueueState.RUNNING }.thenBy { it.nextRunAt ?: 0L })
                    .map { call ->
                        QueuedCallItem(
                            call = call,
                            configName = call.configId?.let { names[it] },
                            sms = call.smsId?.let { smsRepository.getById(it) },
                        )
                    }
            }
}

/** SMS ids whose latest call to this API failed and that are not already back in the queue. */
class ObserveRetryableFailuresUseCase @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val scheduler: CallScheduler,
) {
    operator fun invoke(configId: Long): Flow<List<Long>> =
        combine(callLogRepository.observeFailedSmsIds(configId), scheduler.observeQueue()) { failed, queue ->
            val queued: Set<Long> = queue.filter { it.configId == configId }.mapNotNull { it.smsId }.toSet()

            failed.filterNot { it in queued }
        }
}

/** Queues every retryable failure of one API again, oldest first; returns how many were queued. */
class RetryFailedCallsUseCase @Inject constructor(
    private val observeRetryableFailures: ObserveRetryableFailuresUseCase,
    private val configRepository: ApiConfigRepository,
    private val scheduler: CallScheduler,
) {
    suspend operator fun invoke(configId: Long): Int {
        if (configRepository.getById(configId) == null) {
            AppLogger.i(TAG, "retry all refused, config deleted - {configId: $configId}")
            return 0
        }
        val smsIds: List<Long> = observeRetryableFailures(configId).first()

        smsIds.forEach { scheduler.enqueue(configId, it, CallTrigger.RETRY) }
        AppLogger.i(TAG, "retry all queued - {configId: $configId, smsIds: $smsIds}")

        return smsIds.size
    }
}
