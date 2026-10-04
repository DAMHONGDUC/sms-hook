package com.dd.sms.hook.features.dispatch

import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.dispatch.domain.model.QueueState
import com.dd.sms.hook.features.dispatch.domain.model.QueuedCallItem
import com.dd.sms.hook.features.dispatch.domain.usecase.ObserveCallQueueUseCase
import com.dd.sms.hook.features.dispatch.domain.usecase.ObserveRetryableFailuresUseCase
import com.dd.sms.hook.features.dispatch.domain.usecase.RetryFailedCallsUseCase
import com.dd.sms.hook.testing.FakeApiConfigRepository
import com.dd.sms.hook.testing.FakeCallLogRepository
import com.dd.sms.hook.testing.FakeCallScheduler
import com.dd.sms.hook.testing.FakeReceivedSmsRepository
import com.dd.sms.hook.testing.Fixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallQueueUseCasesTest {
    private val scheduler = FakeCallScheduler()
    private val configs = FakeApiConfigRepository(listOf(Fixtures.config(id = 7L), Fixtures.config(id = 8L).copy(name = "Shop")))
    private val sms = FakeReceivedSmsRepository()
    private val logs = FakeCallLogRepository()
    private val observeFailures = ObserveRetryableFailuresUseCase(logs, scheduler)

    @Test
    fun `queue joins config name and sms, running first then by next run time`() = runTest {
        val smsId: Long = sms.insert(Fixtures.sms())
        scheduler.queue.value = listOf(
            Fixtures.queued(configId = 7L, smsId = smsId, nextRunAt = 300L, attempt = 2),
            Fixtures.queued(configId = 8L, smsId = smsId, state = QueueState.RUNNING),
            Fixtures.queued(configId = 7L, smsId = smsId, nextRunAt = 100L, attempt = 3),
        )

        val items: List<QueuedCallItem> = ObserveCallQueueUseCase(scheduler, configs, sms)(configId = null).first()

        assertEquals(listOf("Shop", "Bank hook", "Bank hook"), items.map { it.configName })
        assertEquals(listOf(1, 3, 2), items.map { it.call.attempt })
        assertEquals(Fixtures.sms().sender, items.first().sms?.sender)
    }

    @Test
    fun `queue for one API hides other APIs and keeps untagged jobs readable`() = runTest {
        scheduler.queue.value = listOf(Fixtures.queued(configId = 8L), Fixtures.queued(configId = null, smsId = null))

        assertEquals(emptyList<QueuedCallItem>(), ObserveCallQueueUseCase(scheduler, configs, sms)(configId = 7L).first())
        val untagged: QueuedCallItem = ObserveCallQueueUseCase(scheduler, configs, sms)(configId = null).first()
            .single { it.call.configId == null }
        assertNull(untagged.configName)
        assertNull(untagged.sms)
    }

    @Test
    fun `retryable failures are SMS whose latest real attempt failed and that are not queued`() = runTest {
        logs.logs.value = listOf(
            Fixtures.log(id = 1L, smsId = 10L, status = CallStatus.FAILED),
            Fixtures.log(id = 2L, smsId = 10L, status = CallStatus.SUCCESS),
            Fixtures.log(id = 3L, smsId = 11L, status = CallStatus.FAILED),
            Fixtures.log(id = 4L, smsId = 12L, status = CallStatus.FAILED),
            Fixtures.log(id = 5L, smsId = 13L, status = CallStatus.FAILED, trigger = CallTrigger.TEST),
            Fixtures.log(id = 6L, smsId = 14L, status = CallStatus.FAILED, configId = 8L),
        )
        scheduler.queue.value = listOf(Fixtures.queued(configId = 7L, smsId = 12L))

        assertEquals(listOf(11L), observeFailures(7L).first())
    }

    @Test
    fun `retry all queues each failure oldest first as RETRY`() = runTest {
        logs.logs.value = listOf(
            Fixtures.log(id = 1L, smsId = 20L, status = CallStatus.FAILED),
            Fixtures.log(id = 2L, smsId = 21L, status = CallStatus.FAILED),
        )

        val queued: Int = RetryFailedCallsUseCase(observeFailures, configs, scheduler)(7L)

        assertEquals(2, queued)
        assertEquals(listOf(Triple(7L, 20L, CallTrigger.RETRY), Triple(7L, 21L, CallTrigger.RETRY)), scheduler.enqueued)
    }

    @Test
    fun `retry all does nothing for a deleted API`() = runTest {
        logs.logs.value = listOf(Fixtures.log(id = 1L, configId = 99L, smsId = 20L, status = CallStatus.FAILED))

        assertEquals(0, RetryFailedCallsUseCase(observeFailures, configs, scheduler)(99L))
        assertTrue(scheduler.enqueued.isEmpty())
    }
}
