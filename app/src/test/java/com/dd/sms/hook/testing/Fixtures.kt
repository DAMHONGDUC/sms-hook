package com.dd.sms.hook.testing

import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfigDefaults
import com.dd.sms.hook.features.apiconfig.domain.model.MatchMode
import com.dd.sms.hook.features.apiconfig.domain.model.SmsFilter
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.dispatch.domain.model.QueueState
import com.dd.sms.hook.features.dispatch.domain.model.QueuedCall
import com.dd.sms.hook.features.dispatch.domain.model.ReceivedSms

/** Shared builders for unit tests. */
object Fixtures {
    const val RECEIVED_AT: Long = 1_700_000_000_000L

    fun config(
        id: Long = 7L,
        url: String = "https://example.com/hook",
        filter: SmsFilter = SmsFilter.ANY,
        maxRetries: Int = 2,
    ): ApiConfig = ApiConfigDefaults.newConfig().copy(
        id = id,
        name = "Bank hook",
        url = url,
        filter = filter,
        maxRetries = maxRetries,
    )

    fun filter(senders: String = "", keyword: String = "", mode: MatchMode = MatchMode.CONTAINS): SmsFilter =
        SmsFilter(senders = senders, keyword = keyword, mode = mode)

    fun sms(id: Long = 3L, sender: String = "+84 901 234 567", body: String = "Your OTP is 123456"): ReceivedSms =
        ReceivedSms(id, sender, body, RECEIVED_AT, subscriptionId = 1, matchedCount = 0)

    fun queued(
        configId: Long? = 7L,
        smsId: Long? = 3L,
        state: QueueState = QueueState.WAITING,
        nextRunAt: Long? = null,
        attempt: Int = 1,
    ): QueuedCall = QueuedCall(
        workId = "work-$configId-$smsId",
        configId = configId,
        smsId = smsId,
        trigger = CallTrigger.SMS,
        state = state,
        attempt = attempt,
        nextRunAt = nextRunAt,
    )

    fun log(
        id: Long = 0L,
        configId: Long? = 7L,
        smsId: Long? = 3L,
        status: CallStatus = CallStatus.SUCCESS,
        trigger: CallTrigger = CallTrigger.SMS,
        createdAt: Long = RECEIVED_AT,
        attempt: Int = 1,
        configName: String = "Bank hook",
    ): CallLog = CallLog(
        id = id,
        configId = configId,
        configName = configName,
        smsId = smsId,
        smsSender = "VCB",
        smsBody = "OTP 123456",
        url = "https://example.com/hook",
        method = "POST",
        requestHeaders = emptyList(),
        requestBody = "{}",
        responseCode = if (status == CallStatus.SUCCESS) 200 else 500,
        responseBody = "ok",
        errorMessage = null,
        durationMs = 100L,
        attempt = attempt,
        status = status,
        trigger = trigger,
        createdAt = createdAt,
    )
}
