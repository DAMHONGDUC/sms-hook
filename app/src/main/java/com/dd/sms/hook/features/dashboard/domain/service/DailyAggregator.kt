package com.dd.sms.hook.features.dashboard.domain.service

import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.calllog.domain.model.CallPoint
import com.dd.sms.hook.features.calllog.domain.model.DailyCallCount
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Buckets calls into local days, emitting a zero entry for days without calls. */
class DailyAggregator @Inject constructor() {
    fun aggregate(
        points: List<CallPoint>,
        days: Int,
        nowMillis: Long = TimeUtils.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DailyCallCount> {
        val byDay: Map<LocalDate, List<CallPoint>> = points.groupBy { TimeUtils.toLocalDate(it.createdAt, zone) }

        return TimeUtils.daysOfRange(days, nowMillis, zone).map { day ->
            val dayPoints: List<CallPoint> = byDay[day].orEmpty()
            val success: Int = dayPoints.count { it.success }
            DailyCallCount(day = day, success = success, failed = dayPoints.size - success)
        }
    }
}
