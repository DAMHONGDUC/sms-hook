package com.dd.sms.hook.features.dispatch.domain.model

import com.dd.sms.hook.features.apiconfig.domain.model.HeaderEntry
import com.dd.sms.hook.features.apiconfig.domain.model.HttpMethod
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger

/** An SMS as received by the device; multipart messages are already joined. */
data class ReceivedSms(
    val id: Long,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val subscriptionId: Int,
    val matchedCount: Int,
) {
    companion object {
        const val NEW_ID: Long = 0L
    }
}

/** A fully rendered request, ready to send. */
data class HttpRequestSpec(
    val url: String,
    val method: HttpMethod,
    val headers: List<HeaderEntry>,
    val body: String?,
)

sealed interface HttpResult {
    val durationMs: Long

    data class Response(val code: Int, val body: String, override val durationMs: Long) : HttpResult {
        val isSuccessful: Boolean get() = code in 200..299
    }

    data class Failure(val message: String, override val durationMs: Long) : HttpResult
}

sealed interface DispatchOutcome {
    data class Success(val log: CallLog) : DispatchOutcome

    data class Failed(val log: CallLog, val willRetry: Boolean) : DispatchOutcome

    /** Nothing was sent, e.g. the config was deleted or disabled after the SMS was queued. */
    data class Skipped(val reason: String) : DispatchOutcome
}

enum class QueueState { WAITING, RUNNING }

/** A call still in the delivery queue. Ids are null for jobs queued before they were tagged. */
data class QueuedCall(
    val workId: String,
    val configId: Long?,
    val smsId: Long?,
    val trigger: CallTrigger,
    val state: QueueState,
    val attempt: Int,
    /** Earliest run time while waiting for a backoff; null when it only waits for its turn or network. */
    val nextRunAt: Long?,
)

/** A queued call joined with the config and SMS it will send, for display. */
data class QueuedCallItem(
    val call: QueuedCall,
    val configName: String?,
    val sms: ReceivedSms?,
)
