package com.dd.sms.hook.features.devtools.domain.usecase

import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.repository.ApiConfigRepository
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.calllog.domain.repository.CallLogRepository
import com.dd.sms.hook.features.devtools.domain.model.DemoConstants
import com.dd.sms.hook.features.devtools.domain.model.DemoMessage
import com.dd.sms.hook.features.devtools.domain.model.DemoSeedResult
import com.dd.sms.hook.features.devtools.domain.service.DemoDataFactory
import com.dd.sms.hook.features.dispatch.domain.model.HttpRequestSpec
import com.dd.sms.hook.features.dispatch.domain.model.ReceivedSms
import com.dd.sms.hook.features.dispatch.domain.repository.ReceivedSmsRepository
import com.dd.sms.hook.features.dispatch.domain.service.RequestFactory
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.shared.domain.time.TimeUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val TAG = "DemoDataUseCases"

/** Removes demo APIs with their history and demo SMS; real data is never touched. Returns removed APIs. */
class ClearDemoDataUseCase @Inject constructor(
    private val configRepository: ApiConfigRepository,
    private val callLogRepository: CallLogRepository,
    private val smsRepository: ReceivedSmsRepository,
) {
    suspend operator fun invoke(): Int {
        val demoConfigs: List<ApiConfig> = configRepository.observeAll().first()
            .filter { it.name.startsWith(DemoConstants.NAME_PREFIX) }
        var removedCalls = 0

        demoConfigs.forEach { config ->
            removedCalls += callLogRepository.deleteByConfig(config.id)
            configRepository.delete(config.id)
        }
        val removedSms: Int = smsRepository.deleteBySubscription(DemoConstants.SUBSCRIPTION_ID)
        AppLogger.i(TAG, "demo data cleared - {apis: ${demoConfigs.map { it.id }}, calls: $removedCalls, sms: $removedSms}")

        return demoConfigs.size
    }
}

/** Replaces any previous demo data with fresh APIs, SMS and call history ending now. */
class SeedDemoDataUseCase @Inject constructor(
    private val clearDemoData: ClearDemoDataUseCase,
    private val factory: DemoDataFactory,
    private val requestFactory: RequestFactory,
    private val configRepository: ApiConfigRepository,
    private val callLogRepository: CallLogRepository,
    private val smsRepository: ReceivedSmsRepository,
) {
    suspend operator fun invoke(): DemoSeedResult {
        val now: Long = TimeUtils.now()
        var calls = 0

        clearDemoData()
        val configs: List<ApiConfig> = factory.configs(now).map { it.copy(id = configRepository.save(it)) }
        val messages: List<DemoMessage> = factory.messages(configs, now)
        messages.forEach { message ->
            val sms: ReceivedSms = message.sms.copy(id = smsRepository.insert(message.sms))
            message.calls.forEach { call ->
                val config: ApiConfig = configs[call.configIndex]
                val request: HttpRequestSpec = requestFactory.build(config, sms)
                call.attempts.forEachIndexed { index, attempt ->
                    callLogRepository.insert(
                        CallLog(
                            id = 0L,
                            configId = config.id,
                            configName = config.name,
                            smsId = sms.id,
                            smsSender = sms.sender,
                            smsBody = sms.body,
                            url = request.url,
                            method = request.method.name,
                            requestHeaders = request.headers,
                            requestBody = request.body.orEmpty(),
                            responseCode = attempt.responseCode,
                            responseBody = attempt.responseBody,
                            errorMessage = attempt.errorMessage,
                            durationMs = attempt.durationMs,
                            attempt = index + 1,
                            status = attempt.status,
                            trigger = CallTrigger.SMS,
                            createdAt = (sms.receivedAt + attempt.offsetMs).coerceAtMost(now),
                        )
                    )
                    calls++
                }
            }
        }
        AppLogger.i(TAG, "demo data seeded - {apis: ${configs.map { it.id }}, sms: ${messages.size}, calls: $calls}")

        return DemoSeedResult(apis = configs.size, sms = messages.size, calls = calls)
    }
}
