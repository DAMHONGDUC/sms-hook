package com.dd.sms.hook.features.dashboard

import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.dashboard.domain.model.DashboardData
import com.dd.sms.hook.features.dashboard.domain.model.DashboardRange
import com.dd.sms.hook.features.dashboard.domain.service.DailyAggregator
import com.dd.sms.hook.features.dashboard.domain.usecase.ObserveDashboardUseCase
import com.dd.sms.hook.features.dispatch.domain.model.ReceivedSms
import com.dd.sms.hook.testing.FakeApiConfigRepository
import com.dd.sms.hook.testing.FakeCallLogRepository
import com.dd.sms.hook.testing.FakeReceivedSmsRepository
import com.dd.sms.hook.testing.FakeSettingsRepository
import com.dd.sms.hook.testing.Fixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveDashboardUseCaseTest {
    private val now: Long = TimeUtils.now()
    private val logs = FakeCallLogRepository(
        listOf(
            Fixtures.log(id = 1L, createdAt = now, status = CallStatus.SUCCESS),
            Fixtures.log(id = 2L, createdAt = now, status = CallStatus.FAILED),
            Fixtures.log(id = 3L, createdAt = now, trigger = CallTrigger.TEST),
            Fixtures.log(id = 4L, createdAt = now - 20 * TimeUtils.MILLIS_PER_DAY),
        )
    )
    private val sms = FakeReceivedSmsRepository()
    private val useCase = ObserveDashboardUseCase(
        logs,
        sms,
        FakeApiConfigRepository(listOf(Fixtures.config(id = 1L), Fixtures.config(id = 2L).copy(enabled = false))),
        FakeSettingsRepository(),
        DailyAggregator(),
    )

    @Test
    fun `week range counts only real calls inside the range`() = runTest {
        sms.insert(ReceivedSms(0, "VCB", "x", now, 0, 0))

        val data: DashboardData = useCase(DashboardRange.WEEK).first()

        assertEquals(2, data.summary.total)
        assertEquals(1, data.summary.success)
        assertEquals(1, data.smsReceived)
        assertEquals(7, data.daily.size)
        assertEquals(2, data.daily.last().total)
        assertEquals(1, data.enabledApis)
        assertTrue(data.hasForwardedSms)
    }

    @Test
    fun `month range includes the older call`() = runTest {
        assertEquals(3, useCase(DashboardRange.MONTH).first().summary.total)
    }

    @Test
    fun `only test calls do not count as a forwarded sms`() = runTest {
        logs.logs.value = listOf(Fixtures.log(id = 1L, createdAt = now, trigger = CallTrigger.TEST))

        assertFalse(useCase(DashboardRange.WEEK).first().hasForwardedSms)
    }
}
