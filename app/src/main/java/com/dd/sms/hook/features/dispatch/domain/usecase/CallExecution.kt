package com.dd.sms.hook.features.dispatch.domain.usecase

import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.calllog.domain.repository.CallLogRepository
import com.dd.sms.hook.features.dispatch.domain.model.HttpRequestSpec
import com.dd.sms.hook.features.dispatch.domain.model.HttpResult
import com.dd.sms.hook.features.dispatch.domain.model.ReceivedSms
import com.dd.sms.hook.features.dispatch.domain.service.HttpExecutor
import com.dd.sms.hook.features.dispatch.domain.service.RequestFactory
import javax.inject.Inject

private const val TAG = "CallExecution"

/** Status codes worth retrying; any other 4xx will fail the same way again. */
private val RETRYABLE_CODES: Set<Int> = setOf(408, 425, 429)

/** Renders, sends and records one attempt. Shared by queued calls and editor test calls. */
class CallExecution @Inject constructor(
    private val requestFactory: RequestFactory,
    private val httpExecutor: HttpExecutor,
    private val callLogRepository: CallLogRepository,
) {
    data class Result(val log: CallLog, val retryable: Boolean)

    suspend fun run(config: ApiConfig, sms: ReceivedSms, attempt: Int, trigger: CallTrigger): Result {
        val request: HttpRequestSpec = requestFactory.build(config, sms)

        AppLogger.i(
            TAG,
            "request - {config: ${config.id}, attempt: $attempt, trigger: $trigger, method: ${request.method}, " +
                "url: ${request.url}, headers: ${request.headers.map { it.name }}, body: ${request.body}}"
        )
        val result: HttpResult = httpExecutor.execute(request, config.timeoutSeconds)
        val success: Boolean = result is HttpResult.Response && result.isSuccessful
        val log: CallLog = CallLog(
            id = 0L,
            configId = config.id.takeIf { !config.isNew },
            configName = config.name,
            smsId = sms.id.takeIf { it != ReceivedSms.NEW_ID },
            smsSender = sms.sender,
            smsBody = sms.body,
            url = request.url,
            method = request.method.name,
            requestHeaders = request.headers,
            requestBody = request.body.orEmpty(),
            responseCode = (result as? HttpResult.Response)?.code,
            responseBody = (result as? HttpResult.Response)?.body,
            errorMessage = (result as? HttpResult.Failure)?.message,
            durationMs = result.durationMs,
            attempt = attempt,
            status = if (success) CallStatus.SUCCESS else CallStatus.FAILED,
            trigger = trigger,
            createdAt = TimeUtils.now(),
        )
        val id: Long = callLogRepository.insert(log)

        AppLogger.i(
            TAG,
            "response - {log: $id, config: ${config.id}, status: ${log.status}, code: ${log.responseCode}, " +
                "durationMs: ${log.durationMs}, error: ${log.errorMessage}, body: ${log.responseBody}}"
        )
        return Result(log.copy(id = id), retryable = !success && isRetryable(result))
    }

    private fun isRetryable(result: HttpResult): Boolean = when (result) {
        is HttpResult.Failure -> true
        is HttpResult.Response -> result.code >= 500 || result.code in RETRYABLE_CODES
    }
}
