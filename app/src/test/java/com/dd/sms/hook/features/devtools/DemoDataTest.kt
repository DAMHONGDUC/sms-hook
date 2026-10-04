package com.dd.sms.hook.features.devtools

import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.devtools.domain.model.DemoConstants
import com.dd.sms.hook.features.devtools.domain.model.DemoMessage
import com.dd.sms.hook.features.devtools.domain.model.DemoSeedResult
import com.dd.sms.hook.features.devtools.domain.service.DemoDataFactory
import com.dd.sms.hook.features.devtools.domain.usecase.ClearDemoDataUseCase
import com.dd.sms.hook.features.devtools.domain.usecase.SeedDemoDataUseCase
import com.dd.sms.hook.features.dispatch.domain.service.RequestFactory
import com.dd.sms.hook.features.dispatch.domain.service.TemplateRenderer
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.testing.FakeApiConfigRepository
import com.dd.sms.hook.testing.FakeCallLogRepository
import com.dd.sms.hook.testing.FakeReceivedSmsRepository
import com.dd.sms.hook.testing.Fixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DemoDataTest {
    private val now: Long = TimeUtils.now()
    private val factory = DemoDataFactory()
    private val configs = FakeApiConfigRepository(listOf(Fixtures.config(id = 1L)))
    private val logs = FakeCallLogRepository(listOf(Fixtures.log(id = 1L, configId = 1L, smsId = 1L)))
    private val sms = FakeReceivedSmsRepository()
    private val clear = ClearDemoDataUseCase(configs, logs, sms)
    private val seed = SeedDemoDataUseCase(clear, factory, RequestFactory(TemplateRenderer()), configs, logs, sms)

    private fun demoConfigs(): List<ApiConfig> = factory.configs(now).mapIndexed { index, config -> config.copy(id = index + 1L) }

    @Test
    fun `same seed gives the same messages inside the demo window`() {
        val first: List<DemoMessage> = factory.messages(demoConfigs(), now, Random(1))
        val second: List<DemoMessage> = factory.messages(demoConfigs(), now, Random(1))

        assertEquals(first, second)
        assertTrue(first.size >= DemoConstants.DAYS * DemoConstants.MIN_SMS_PER_DAY)
        assertTrue(first.all { it.sms.receivedAt in TimeUtils.startOfRange(DemoConstants.DAYS, now)..now })
        assertTrue(first.all { it.sms.subscriptionId == DemoConstants.SUBSCRIPTION_ID })
    }

    @Test
    fun `attempts follow the retry policy and cover success, recovery and give-up`() {
        val messages: List<DemoMessage> = factory.messages(demoConfigs(), now)
        val chains = messages.flatMap { it.calls }

        chains.forEach { call ->
            assertTrue(call.attempts.size <= demoConfigs()[call.configIndex].maxRetries + 1)
            assertTrue(call.attempts.dropLast(1).all { it.status == CallStatus.FAILED })
            assertEquals(call.attempts.map { it.offsetMs }.sorted(), call.attempts.map { it.offsetMs })
        }
        assertTrue(chains.any { it.attempts.size == 1 && it.attempts.single().status == CallStatus.SUCCESS })
        assertTrue(chains.any { it.attempts.size > 1 && it.attempts.last().status == CallStatus.SUCCESS })
        assertTrue(chains.any { it.attempts.size > 1 && it.attempts.last().status == CallStatus.FAILED })
    }

    @Test
    fun `seed stores disabled demo apis and history linked to stored sms`() = runTest {
        val result: DemoSeedResult = seed()

        val demo: List<ApiConfig> = configs.configs.value.filter { it.name.startsWith(DemoConstants.NAME_PREFIX) }
        val demoLogs: List<CallLog> = logs.logs.value.filter { it.configName.startsWith(DemoConstants.NAME_PREFIX) }
        assertEquals(3, result.apis)
        assertEquals(demo.size, result.apis)
        assertTrue(demo.none { it.enabled })
        assertEquals(result.sms, sms.sms.value.size)
        assertEquals(result.calls, demoLogs.size)
        assertTrue(demoLogs.all { log -> sms.sms.value.any { it.id == log.smsId } && demo.any { it.id == log.configId } })
        assertTrue(demoLogs.all { it.createdAt <= TimeUtils.now() })
    }

    @Test
    fun `seeding twice replaces demo data and clear keeps real data`() = runTest {
        seed()
        val second: DemoSeedResult = seed()

        assertEquals(second.apis, configs.configs.value.count { it.name.startsWith(DemoConstants.NAME_PREFIX) })
        assertEquals(second.sms, sms.sms.value.size)
        assertEquals(3, clear())
        assertEquals(listOf(1L), configs.configs.value.map { it.id })
        assertEquals(listOf(1L), logs.logs.value.map { it.id })
        assertFalse(sms.sms.value.any { it.subscriptionId == DemoConstants.SUBSCRIPTION_ID })
    }
}
