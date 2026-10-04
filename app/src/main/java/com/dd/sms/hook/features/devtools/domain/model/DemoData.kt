package com.dd.sms.hook.features.devtools.domain.model

import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.dispatch.domain.model.ReceivedSms

/** Values that mark and shape demo data; dev builds only. */
object DemoConstants {
    /** Demo APIs are found again by this name prefix. */
    const val NAME_PREFIX = "Demo · "
    /** Demo SMS use this SIM id; real ones are >= 0, or -1 when unknown. */
    const val SUBSCRIPTION_ID = -2
    const val DAYS = 14
    const val MIN_SMS_PER_DAY = 2
    const val MAX_SMS_PER_DAY = 6
    const val RANDOM_SEED = 42
    const val MIN_DURATION_MS = 40L
    const val MAX_DURATION_MS = 900L
    const val TIMEOUT_DURATION_MS = 15_000L
    const val FIRST_BACKOFF_MS = 15_000L
}

/** One demo SMS and what each matched demo API did with it. */
data class DemoMessage(val sms: ReceivedSms, val calls: List<DemoCall>)

/** All attempts of one SMS sent to the demo API at [configIndex], oldest first. */
data class DemoCall(val configIndex: Int, val attempts: List<DemoAttempt>)

data class DemoAttempt(
    val status: CallStatus,
    val responseCode: Int?,
    val responseBody: String?,
    val errorMessage: String?,
    val durationMs: Long,
    /** Time after the SMS arrived. */
    val offsetMs: Long,
)

data class DemoSeedResult(val apis: Int, val sms: Int, val calls: Int)
