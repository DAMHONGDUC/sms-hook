package com.dd.sms.hook.features.calllog.domain.model

import com.dd.sms.hook.features.apiconfig.domain.model.HeaderEntry

enum class CallStatus { SUCCESS, FAILED }

/** What started the call; TEST calls are kept in history but excluded from analytics. */
enum class CallTrigger { SMS, TEST, RETRY }

/** One HTTP attempt. Config and SMS fields are snapshots taken at call time. */
data class CallLog(
    val id: Long,
    val configId: Long?,
    val configName: String,
    val smsId: Long?,
    val smsSender: String,
    val smsBody: String,
    val url: String,
    val method: String,
    val requestHeaders: List<HeaderEntry>,
    val requestBody: String,
    val responseCode: Int?,
    val responseBody: String?,
    val errorMessage: String?,
    val durationMs: Long,
    val attempt: Int,
    val status: CallStatus,
    val trigger: CallTrigger,
    val createdAt: Long,
) {
    /** Only a failed real call can be sent again; tests and successes are final. */
    val canRetry: Boolean
        get() = status == CallStatus.FAILED && trigger != CallTrigger.TEST && configId != null && smsId != null
}

data class CallLogFilter(
    val status: CallStatus?,
    val query: String,
    val configId: Long?,
) {
    companion object {
        val ALL: CallLogFilter = CallLogFilter(status = null, query = "", configId = null)
    }
}
