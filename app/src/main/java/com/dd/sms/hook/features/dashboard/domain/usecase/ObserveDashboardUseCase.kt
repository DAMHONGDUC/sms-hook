package com.dd.sms.hook.features.dashboard.domain.usecase

import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.apiconfig.domain.repository.ApiConfigRepository
import com.dd.sms.hook.features.calllog.domain.model.ApiCallBreakdown
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallLogFilter
import com.dd.sms.hook.features.calllog.domain.model.CallPoint
import com.dd.sms.hook.features.calllog.domain.model.CallSummary
import com.dd.sms.hook.features.calllog.domain.repository.CallLogRepository
import com.dd.sms.hook.features.calllog.domain.usecase.CallLogLimits
import com.dd.sms.hook.features.dashboard.domain.model.DashboardData
import com.dd.sms.hook.features.dashboard.domain.model.DashboardRange
import com.dd.sms.hook.features.dashboard.domain.service.DailyAggregator
import com.dd.sms.hook.features.dispatch.domain.repository.ReceivedSmsRepository
import com.dd.sms.hook.features.settings.domain.model.AppSettings
import com.dd.sms.hook.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

private const val TOP_API_LIMIT = 5

private data class DashboardStatus(
    val smsReceived: Int,
    val enabledApis: Int,
    val settings: AppSettings,
    val hasForwardedSms: Boolean,
)

class ObserveDashboardUseCase @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val smsRepository: ReceivedSmsRepository,
    private val configRepository: ApiConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val aggregator: DailyAggregator,
) {
    operator fun invoke(range: DashboardRange): Flow<DashboardData> {
        val from: Long = TimeUtils.startOfRange(range.days)
        val summary: Flow<CallSummary> = callLogRepository.observeSummary(from)
        val timeline: Flow<List<CallPoint>> = callLogRepository.observeTimeline(from)
        val breakdown: Flow<List<ApiCallBreakdown>> = callLogRepository.observeBreakdown(from, TOP_API_LIMIT)
        val recent: Flow<List<CallLog>> = callLogRepository.observe(CallLogFilter.ALL, CallLogLimits.RECENT_LIMIT)
        val status: Flow<DashboardStatus> = combine(
            smsRepository.observeCountSince(from),
            configRepository.observeEnabledCount(),
            settingsRepository.settings,
            callLogRepository.observeHasSuccess(),
        ) { sms, enabled, settings, hasSuccess -> DashboardStatus(sms, enabled, settings, hasSuccess) }

        return combine(summary, timeline, breakdown, recent, status) { s, points, top, latest, (sms, enabled, settings, hasSuccess) ->
            DashboardData(
                range = range,
                summary = s,
                smsReceived = sms,
                daily = aggregator.aggregate(points, range.days),
                topApis = top,
                recent = latest,
                enabledApis = enabled,
                forwardingEnabled = settings.forwardingEnabled,
                keepAliveEnabled = settings.keepAliveEnabled,
                hasForwardedSms = hasSuccess,
            )
        }
    }
}
