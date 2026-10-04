package com.dd.sms.hook.e2e

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.work.WorkInfo
import com.dd.sms.hook.MainActivity
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfigDefaults
import com.dd.sms.hook.features.apiconfig.domain.repository.ApiConfigRepository
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallLogFilter
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.calllog.domain.repository.CallLogRepository
import com.dd.sms.hook.features.dispatch.domain.usecase.HandleIncomingSmsUseCase
import com.dd.sms.hook.features.settings.domain.repository.SettingsRepository
import com.dd.sms.hook.testing.ComposeRobot
import com.dd.sms.hook.testing.WorkManagerTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

private const val REQUEST_WAIT_SECONDS = 10L
private const val POLL_MILLIS = 100L

/** Drives the real app: UI, Room, WorkManager and HTTP, with only the network endpoint faked. */
@HiltAndroidTest
class AppFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val workRule = WorkManagerTestRule(hiltRule)

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var configRepository: ApiConfigRepository

    @Inject
    lateinit var callLogRepository: CallLogRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    /** Built lazily: it needs WorkManager, which the rule starts after injection. */
    @Inject
    lateinit var handleIncomingSms: Provider<HandleIncomingSmsUseCase>

    private val server = MockWebServer()
    private val robot by lazy { ComposeRobot(composeRule) }

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun seedConfig(name: String = "Shop", path: String = "/hook", maxRetries: Int = 3): ApiConfig = runBlocking {
        val draft: ApiConfig = ApiConfigDefaults.newConfig().copy(
            name = name,
            url = server.url(path).toString(),
            maxRetries = maxRetries,
            createdAt = TimeUtils.now(),
        )
        draft.copy(id = configRepository.save(draft))
    }

    private fun receiveSms(sender: String, body: String): Int = runBlocking {
        handleIncomingSms.get()(sender, body, TimeUtils.now(), subscriptionId = 0)
    }

    private fun awaitLogs(count: Int): List<CallLog> {
        val deadline: Long = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(REQUEST_WAIT_SECONDS)
        while (System.currentTimeMillis() < deadline) {
            val logs: List<CallLog> = runBlocking { callLogRepository.observe(CallLogFilter.ALL, 100).first() }
            if (logs.size >= count) return logs
            Thread.sleep(POLL_MILLIS)
        }
        error("expected $count call logs")
    }

    private fun takeRequest(): RecordedRequest =
        server.takeRequest(REQUEST_WAIT_SECONDS, TimeUnit.SECONDS) ?: error("no request reached the server")

    @Test
    fun createApiThroughTheEditor_appearsInListWithConfirmation() {
        robot.tab(R.string.tab_apis)
        robot.tap(R.string.api_list_new)
        robot.type(R.string.editor_name, "Shop")
        robot.type(R.string.editor_url, server.url("/hook").toString())
        robot.type(R.string.editor_senders, "VCB")
        robot.tap(R.string.action_save)

        robot.waitFor(robot.string(R.string.editor_saved, "Shop"))
        robot.waitFor(robot.string(R.string.api_filter_from, "VCB"))
        val saved: ApiConfig = runBlocking { configRepository.observeAll().first() }.single()
        assertEquals("VCB", saved.filter.senders)
    }

    @Test
    fun savingAnEmptyApi_showsFieldErrorsAndStaysOnEditor() {
        robot.tab(R.string.tab_apis)
        robot.tap(R.string.api_list_new)
        robot.tap(R.string.action_save)

        robot.waitFor(R.string.editor_error_name)
        robot.waitFor(R.string.editor_error_url)
        assertTrue(runBlocking { configRepository.observeAll().first() }.isEmpty())
    }

    @Test
    fun testRequestFromEditor_celebratesSuccessAndExplainsFailure() {
        seedConfig()
        server.enqueue(MockResponse.Builder().code(200).body("ok").build())
        server.enqueue(MockResponse.Builder().code(500).body("boom").build())

        robot.tab(R.string.tab_apis)
        robot.clickable("Shop").performClick()
        robot.tap(R.string.editor_test_button)
        robot.tap(R.string.test_send)
        robot.waitFor(R.string.test_success_title)
        assertTrue(takeRequest().body!!.utf8().contains("\"sender\""))

        robot.tap(R.string.test_send)
        robot.waitFor(R.string.test_failure_title)
        robot.waitFor(robot.string(R.string.test_failure_message_code, 500))
    }

    @Test
    fun incomingSms_isForwardedWithRenderedBody_andShowsInHistory() {
        seedConfig()
        server.enqueue(MockResponse.Builder().code(200).body("{\"ok\":true}").build())

        assertEquals(1, receiveSms("VCB", "Your OTP is 777 \"quoted\""))
        workRule.runQueuedCalls()
        val request: RecordedRequest = takeRequest()
        val body: String = request.body!!.utf8()

        assertEquals("POST", request.method)
        assertTrue(body, body.contains("\"sender\": \"VCB\""))
        assertTrue(body, body.contains("\"message\": \"Your OTP is 777 \\\"quoted\\\"\""))
        assertEquals(CallStatus.SUCCESS, awaitLogs(1).single().status)

        robot.tab(R.string.tab_history)
        robot.waitFor(robot.string(R.string.history_row_sms, "VCB", "Your OTP is 777 \"quoted\""))
        robot.waitFor("200")
    }

    @Test
    fun failedCall_isRetriedAutomatically_andTimelineShowsEveryAttempt() {
        seedConfig()
        server.enqueue(MockResponse.Builder().code(503).body("busy").build())
        server.enqueue(MockResponse.Builder().code(200).body("ok").build())

        receiveSms("VCB", "retry me")
        workRule.runQueuedCalls()
        takeRequest()
        awaitLogs(1)
        assertTrue(workRule.callStates().contains(WorkInfo.State.ENQUEUED))
        workRule.runQueuedCalls()
        takeRequest()
        val logs: List<CallLog> = awaitLogs(2)
        assertEquals(listOf(CallStatus.SUCCESS, CallStatus.FAILED), logs.map { it.status })

        robot.tab(R.string.tab_history)
        robot.clickable("200").performClick()
        robot.waitFor(R.string.detail_timeline)
        robot.waitFor(robot.string(R.string.detail_timeline_attempt, 1) + " · 503")
        robot.waitFor(robot.string(R.string.detail_timeline_attempt, 2) + " · 200")
    }

    @Test
    fun nonMatchingSms_andForwardingOff_neverCallTheApi() {
        seedConfig().let { runBlocking { configRepository.save(it.copy(filter = it.filter.copy(senders = "VCB"))) } }

        assertEquals(0, receiveSms("TCB", "not for us"))
        runBlocking { settingsRepository.setForwardingEnabled(false) }
        assertEquals(0, receiveSms("VCB", "forwarding off"))

        assertTrue(workRule.callStates().isEmpty())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun historyFilters_narrowByStatusAndApi() {
        runBlocking {
            callLogRepository.insert(log(configId = 1L, name = "Shop", status = CallStatus.SUCCESS, body = "paid order"))
            callLogRepository.insert(log(configId = 2L, name = "Backup", status = CallStatus.FAILED, body = "backup failed"))
        }

        robot.tab(R.string.tab_history)
        robot.waitFor("paid order", substring = true)
        robot.tap(R.string.status_failed)
        robot.waitFor("backup failed", substring = true)
        robot.assertAbsent(robot.string(R.string.history_row_sms, "VCB", "paid order"))
    }

    @Test
    fun deleteApi_asksForConfirmation_thenShowsEmptyState() {
        seedConfig()

        robot.tab(R.string.tab_apis)
        robot.waitFor("Shop")
        composeRule.onAllNodesWithContentDescription(robot.string(R.string.action_more))[0].performClick()
        robot.tap(R.string.action_delete)
        robot.waitFor(R.string.api_list_delete_title)
        composeRule.onAllNodes(hasText(robot.string(R.string.action_delete)))[0].performClick()

        robot.waitFor(R.string.api_list_empty_title)
        assertTrue(runBlocking { configRepository.observeAll().first() }.isEmpty())
    }

    @Test
    fun settingsSwitch_turnsForwardingOff() {
        robot.tab(R.string.tab_settings)
        robot.clickable(robot.string(R.string.settings_forwarding)).performClick()

        composeRule.waitUntil(REQUEST_WAIT_SECONDS * 1000) { runBlocking { !settingsRepository.current().forwardingEnabled } }
        assertFalse(runBlocking { settingsRepository.current().forwardingEnabled })
    }

    private fun log(configId: Long, name: String, status: CallStatus, body: String): CallLog = CallLog(
        id = 0L,
        configId = configId,
        configName = name,
        smsId = null,
        smsSender = "VCB",
        smsBody = body,
        url = "https://x.io",
        method = "POST",
        requestHeaders = emptyList(),
        requestBody = "{}",
        responseCode = if (status == CallStatus.SUCCESS) 200 else 500,
        responseBody = "",
        errorMessage = null,
        durationMs = 100L,
        attempt = 1,
        status = status,
        trigger = CallTrigger.SMS,
        createdAt = TimeUtils.now(),
    )
}
