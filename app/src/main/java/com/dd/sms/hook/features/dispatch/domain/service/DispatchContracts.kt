package com.dd.sms.hook.features.dispatch.domain.service

import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.dispatch.domain.model.HttpRequestSpec
import com.dd.sms.hook.features.dispatch.domain.model.HttpResult
import com.dd.sms.hook.features.dispatch.domain.model.QueuedCall
import kotlinx.coroutines.flow.Flow

/** Sends a rendered request. Never throws: transport errors come back as [HttpResult.Failure]. */
interface HttpExecutor {
    suspend fun execute(request: HttpRequestSpec, timeoutSeconds: Int): HttpResult
}

/** Queues a durable, retrying API call for one config and one received SMS. */
interface CallScheduler {
    fun enqueue(configId: Long, smsId: Long, trigger: CallTrigger)

    /** Calls not finished yet: waiting for network, their turn or a retry, or running now. */
    fun observeQueue(): Flow<List<QueuedCall>>
}

interface FailureNotifier {
    fun notifyFailure(log: CallLog)
}

/** Starts or stops the foreground service that keeps the process alive. */
interface KeepAliveController {
    fun start()

    fun stop()
}
