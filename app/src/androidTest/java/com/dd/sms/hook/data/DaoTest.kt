package com.dd.sms.hook.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dd.sms.hook.shared.data.db.AppDatabase
import com.dd.sms.hook.features.apiconfig.data.local.ApiConfigDao
import com.dd.sms.hook.features.apiconfig.data.local.ApiConfigEntity
import com.dd.sms.hook.features.calllog.data.local.CallLogDao
import com.dd.sms.hook.features.calllog.data.local.CallLogEntity
import com.dd.sms.hook.features.dispatch.data.local.ReceivedSmsDao
import com.dd.sms.hook.features.dispatch.data.local.ReceivedSmsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val SUCCESS = "SUCCESS"
private const val FAILED = "FAILED"
private const val TEST = "TEST"
private const val SMS = "SMS"

/** Runs every query against real SQLite, including the ones the unit tests only fake. */
@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: AppDatabase
    private lateinit var configs: ApiConfigDao
    private lateinit var logs: CallLogDao
    private lateinit var sms: ReceivedSmsDao

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        configs = db.apiConfigDao()
        logs = db.callLogDao()
        sms = db.receivedSmsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun config(name: String, enabled: Boolean = true, createdAt: Long = 1L) = ApiConfigEntity(
        0, name, "https://x.io", "POST", "[]", "{}", "", "", "CONTAINS", enabled, 15, 3, createdAt, createdAt,
    )

    private fun log(
        configId: Long? = 1L,
        smsId: Long? = 1L,
        status: String = SUCCESS,
        trigger: String = SMS,
        createdAt: Long = 1_000L,
        name: String = "Shop",
        body: String = "OTP 1",
        duration: Long = 100L,
    ) = CallLogEntity(
        0, configId, name, smsId, "VCB", body, "https://x.io", "POST", "[]", "{}",
        if (status == SUCCESS) 200 else 500, "", null, duration, 1, status, trigger, createdAt,
    )

    @Test
    fun apiConfigs_orderEnabledCountToggleDelete() = runTest {
        val a: Long = configs.insert(config("A", createdAt = 1L))
        val b: Long = configs.insert(config("B", enabled = false, createdAt = 2L))

        assertEquals(listOf("B", "A"), configs.observeAll().first().map { it.name })
        assertEquals(1, configs.observeEnabledCount().first())
        assertEquals(listOf("A"), configs.getEnabled().map { it.name })
        configs.setEnabled(b, true, 3L)
        assertEquals(2, configs.observeEnabledCount().first())
        configs.delete(a)
        assertNull(configs.getById(a))
    }

    @Test
    fun callLogs_filterByStatusConfigAndQuery_newestFirstWithLimit() = runTest {
        logs.insert(log(configId = 1L, status = SUCCESS, createdAt = 1L, name = "Shop"))
        logs.insert(log(configId = 2L, status = FAILED, createdAt = 2L, name = "Backup", body = "Balance 100"))
        logs.insert(log(configId = 1L, status = FAILED, createdAt = 3L, name = "Shop"))

        assertEquals(listOf(3L, 2L, 1L), logs.observe(null, null, "", 10).first().map { it.createdAt })
        assertEquals(listOf(3L, 2L), logs.observe(FAILED, null, "", 10).first().map { it.createdAt })
        assertEquals(listOf(3L, 1L), logs.observe(null, 1L, "", 10).first().map { it.createdAt })
        assertEquals(listOf(2L), logs.observe(null, null, "balance", 10).first().map { it.createdAt })
        assertEquals(listOf(2L), logs.observe(null, null, "backup", 10).first().map { it.createdAt })
        assertEquals(1, logs.observe(null, null, "", 1).first().size)
    }

    @Test
    fun callLogs_statsExcludeTestCallsAndOldRows() = runTest {
        logs.insert(log(status = SUCCESS, createdAt = 100L, duration = 100L))
        logs.insert(log(status = FAILED, createdAt = 200L, duration = 300L, configId = 2L, name = "Backup"))
        logs.insert(log(status = FAILED, createdAt = 200L, duration = 300L, configId = 2L, name = "Backup"))
        logs.insert(log(status = SUCCESS, trigger = TEST, createdAt = 300L))
        logs.insert(log(status = SUCCESS, createdAt = 10L))

        val summary = logs.observeSummary(50L, SUCCESS, TEST).first()
        assertEquals(3, summary.total)
        assertEquals(1, summary.success)
        assertEquals(700.0 / 3, summary.avgDuration!!, 0.01)
        assertEquals(3, logs.observeTimeline(50L, TEST).first().size)
        val breakdown = logs.observeBreakdown(50L, SUCCESS, TEST, 5).first()
        assertEquals(listOf("Backup", "Shop"), breakdown.map { it.configName })
        assertEquals(listOf(2, 1), breakdown.map { it.total })
    }

    @Test
    fun callLogs_emptyRangeSummaryIsZero() = runTest {
        val summary = logs.observeSummary(0L, SUCCESS, TEST).first()

        assertEquals(0, summary.total)
        assertEquals(0, summary.success)
        assertNull(summary.avgDuration)
    }

    @Test
    fun callLogs_attemptsHasSuccessAndCleanup() = runTest {
        logs.insert(log(smsId = 5L, configId = 1L, status = FAILED, createdAt = 2L))
        logs.insert(log(smsId = 5L, configId = 1L, status = FAILED, createdAt = 1L))
        logs.insert(log(smsId = 5L, configId = 2L, createdAt = 3L))

        assertEquals(listOf(1L, 2L), logs.observeAttempts(5L, 1L).first().map { it.createdAt })
        assertTrue(logs.observeHasSuccess(SUCCESS, TEST).first())
        assertEquals(2, logs.deleteOlderThan(3L))
        logs.clearAll()
        assertFalse(logs.observeHasSuccess(SUCCESS, TEST).first())
    }

    @Test
    fun callLogs_testSuccessDoesNotCountAsForwarded() = runTest {
        logs.insert(log(trigger = TEST))

        assertFalse(logs.observeHasSuccess(SUCCESS, TEST).first())
    }

    @Test
    fun receivedSms_countMatchedAndCleanup() = runTest {
        val id: Long = sms.insert(ReceivedSmsEntity(0, "VCB", "x", 100L, 1, 0))
        sms.insert(ReceivedSmsEntity(0, "VCB", "y", 10L, 1, 0))

        sms.setMatchedCount(id, 2)
        assertEquals(2, sms.getById(id)!!.matchedCount)
        assertEquals(1, sms.observeCountSince(50L).first())
        assertEquals(1, sms.deleteOlderThan(50L))
    }
}
