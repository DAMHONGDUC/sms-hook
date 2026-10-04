package com.dd.sms.hook.features.devtools.domain.service

import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfigDefaults
import com.dd.sms.hook.features.apiconfig.domain.model.HttpMethod
import com.dd.sms.hook.features.apiconfig.domain.model.MatchMode
import com.dd.sms.hook.features.apiconfig.domain.model.SmsFilter
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.devtools.domain.model.DemoAttempt
import com.dd.sms.hook.features.devtools.domain.model.DemoCall
import com.dd.sms.hook.features.devtools.domain.model.DemoConstants
import com.dd.sms.hook.features.devtools.domain.model.DemoMessage
import com.dd.sms.hook.features.dispatch.domain.model.ReceivedSms
import com.dd.sms.hook.shared.domain.time.TimeUtils
import javax.inject.Inject
import kotlin.random.Random

private const val OUTCOME_SUCCESS = 75
private const val OUTCOME_RECOVERED = 87
private const val OUTCOME_GAVE_UP = 96

/**
 * Builds demo APIs and two weeks of SMS with realistic outcomes. Same [random] seed, same data.
 * - 75% succeed first time, 12% succeed after retries, 9% fail every retry, 4% fail with a 404.
 */
class DemoDataFactory @Inject constructor() {
    private val samples: List<List<Pair<String, String>>> = listOf(
        listOf(
            "VCB" to "TK 0123xxx789 +2,500,000VND luc 09:15. SD: 15,320,000VND. ND: CHUYEN TIEN",
            "TPBank" to "TK 0456xxx321 -350,000VND luc 08:02. SD: 4,120,000VND",
            "MBBank" to "TK 0789xxx654 +12,000,000VND. ND: LUONG THANG 9",
        ),
        listOf(
            "Shopee" to "Your OTP is 482913. Valid for 5 minutes.",
            "Grab" to "Grab OTP 771204. Do not share this code.",
            "Google" to "G-559310 is your Google verification code. OTP expires in 10 minutes.",
        ),
        listOf(
            "+84 901 234 567" to "Hi, I will be there at 7pm",
            "+84 912 888 001" to "Your parcel arrives today between 2 and 4pm",
            "+84 938 456 120" to "Dinner at grandma's on Sunday, don't forget",
        ),
    )

    /** Disabled so real SMS never reach the fake endpoints; manual retries still run. */
    fun configs(now: Long): List<ApiConfig> {
        val base: ApiConfig = ApiConfigDefaults.newConfig().copy(enabled = false, createdAt = now, updatedAt = now)

        return listOf(
            base.copy(
                name = DemoConstants.NAME_PREFIX + "Bank alerts",
                url = "https://example.com/demo/bank",
                filter = SmsFilter(senders = "VCB, TPBank, MBBank", keyword = "", mode = MatchMode.CONTAINS),
            ),
            base.copy(
                name = DemoConstants.NAME_PREFIX + "OTP relay",
                url = "https://example.com/demo/otp",
                filter = SmsFilter(senders = "", keyword = "OTP", mode = MatchMode.CONTAINS),
            ),
            base.copy(
                name = DemoConstants.NAME_PREFIX + "Family chat",
                url = "https://example.com/demo/chat?text={{body}}",
                method = HttpMethod.GET,
                filter = SmsFilter(senders = "^\\+84", keyword = "", mode = MatchMode.REGEX),
            ),
        )
    }

    fun messages(configs: List<ApiConfig>, now: Long, random: Random = Random(DemoConstants.RANDOM_SEED)): List<DemoMessage> {
        val start: Long = TimeUtils.startOfRange(DemoConstants.DAYS, now)
        val messages: MutableList<DemoMessage> = mutableListOf()

        for (day: Int in 0 until DemoConstants.DAYS) {
            val count: Int = random.nextInt(DemoConstants.MIN_SMS_PER_DAY, DemoConstants.MAX_SMS_PER_DAY + 1)
            repeat(count) {
                val configIndex: Int = random.nextInt(configs.size)
                val (sender: String, body: String) = samples[configIndex % samples.size].random(random)
                val dayStart: Long = start + day * TimeUtils.MILLIS_PER_DAY
                // Today only runs until now, so its SMS spread over the hours already passed.
                val receivedAt: Long = dayStart + random.nextLong((now - dayStart).coerceIn(1L, TimeUtils.MILLIS_PER_DAY))
                val sms: ReceivedSms = ReceivedSms(ReceivedSms.NEW_ID, sender, body, receivedAt, DemoConstants.SUBSCRIPTION_ID, matchedCount = 1)

                messages += DemoMessage(sms, listOf(DemoCall(configIndex, attempts(configs[configIndex].maxRetries, random))))
            }
        }
        return messages.sortedBy { it.sms.receivedAt }
    }

    private fun attempts(maxRetries: Int, random: Random): List<DemoAttempt> {
        val outcome: Int = random.nextInt(100)
        val retries: Int = if (maxRetries == 0) 0 else random.nextInt(1, maxRetries + 1)

        return when {
            outcome < OUTCOME_SUCCESS -> listOf(success(0L, random))
            outcome < OUTCOME_RECOVERED -> List(retries) { retryableFailure(it, random) } + success(backoffOffset(retries), random)
            outcome < OUTCOME_GAVE_UP -> List(maxRetries + 1) { retryableFailure(it, random) }
            else -> listOf(DemoAttempt(CallStatus.FAILED, 404, "Not Found", null, duration(random), 0L))
        }
    }

    private fun success(offsetMs: Long, random: Random): DemoAttempt =
        DemoAttempt(CallStatus.SUCCESS, 200, "{\"ok\":true}", null, duration(random), offsetMs)

    /** Alternates server errors and timeouts so both show up in history. */
    private fun retryableFailure(index: Int, random: Random): DemoAttempt =
        if (random.nextBoolean()) {
            DemoAttempt(CallStatus.FAILED, 503, "Service Unavailable", null, duration(random), backoffOffset(index))
        } else {
            DemoAttempt(CallStatus.FAILED, null, null, "timeout", DemoConstants.TIMEOUT_DURATION_MS, backoffOffset(index))
        }

    /** Same exponential backoff the queue uses: 0, 15 s, 45 s, 105 s… after the SMS. */
    private fun backoffOffset(index: Int): Long =
        (0 until index).sumOf { DemoConstants.FIRST_BACKOFF_MS shl it }

    private fun duration(random: Random): Long =
        random.nextLong(DemoConstants.MIN_DURATION_MS, DemoConstants.MAX_DURATION_MS)
}
