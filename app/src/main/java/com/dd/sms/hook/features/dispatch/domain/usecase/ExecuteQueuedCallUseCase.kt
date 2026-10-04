package com.dd.sms.hook.features.dispatch.domain.usecase

import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.repository.ApiConfigRepository
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.dispatch.domain.model.DispatchOutcome
import com.dd.sms.hook.features.dispatch.domain.model.ReceivedSms
import com.dd.sms.hook.features.dispatch.domain.repository.ReceivedSmsRepository
import com.dd.sms.hook.features.dispatch.domain.service.FailureNotifier
import com.dd.sms.hook.features.settings.domain.repository.SettingsRepository
import javax.inject.Inject

private const val TAG = "ExecuteQueuedCall"

/** Runs one attempt of a queued call and decides whether the queue should retry it. */
class ExecuteQueuedCallUseCase @Inject constructor(
    private val configRepository: ApiConfigRepository,
    private val smsRepository: ReceivedSmsRepository,
    private val settingsRepository: SettingsRepository,
    private val execution: CallExecution,
    private val failureNotifier: FailureNotifier,
) {
    suspend operator fun invoke(configId: Long, smsId: Long, attempt: Int, trigger: CallTrigger): DispatchOutcome {
        val config: ApiConfig? = configRepository.getById(configId)
        val sms: ReceivedSms? = smsRepository.getById(smsId)

        if (config == null || sms == null) {
            AppLogger.w(TAG, "skipped - {configId: $configId, smsId: $smsId, configFound: ${config != null}, smsFound: ${sms != null}}")
            return DispatchOutcome.Skipped("config or sms no longer exists")
        }
        // A manual retry is an explicit user action, so it runs even for a disabled config.
        if (!config.enabled && trigger == CallTrigger.SMS) {
            AppLogger.i(TAG, "skipped disabled config - {configId: $configId, smsId: $smsId}")
            return DispatchOutcome.Skipped("config disabled")
        }
        val result: CallExecution.Result = execution.run(config, sms, attempt, trigger)

        if (result.log.status == CallStatus.SUCCESS) {
            return DispatchOutcome.Success(result.log)
        }
        val willRetry: Boolean = result.retryable && attempt <= config.maxRetries

        if (!willRetry && settingsRepository.current().notifyOnFailure) {
            failureNotifier.notifyFailure(result.log)
        }
        AppLogger.w(TAG, "attempt failed - {log: ${result.log.id}, attempt: $attempt, maxRetries: ${config.maxRetries}, willRetry: $willRetry}")

        return DispatchOutcome.Failed(result.log, willRetry)
    }
}
