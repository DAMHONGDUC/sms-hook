package com.dd.sms.hook.testing

import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.repository.ApiConfigRepository
import com.dd.sms.hook.features.calllog.domain.model.ApiCallBreakdown
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallLogFilter
import com.dd.sms.hook.features.calllog.domain.model.CallPoint
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallSummary
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.calllog.domain.repository.CallLogRepository
import com.dd.sms.hook.features.dispatch.domain.model.HttpRequestSpec
import com.dd.sms.hook.features.dispatch.domain.model.HttpResult
import com.dd.sms.hook.features.dispatch.domain.model.QueuedCall
import com.dd.sms.hook.features.dispatch.domain.model.ReceivedSms
import com.dd.sms.hook.features.dispatch.domain.repository.ReceivedSmsRepository
import com.dd.sms.hook.features.dispatch.domain.service.CallScheduler
import com.dd.sms.hook.features.dispatch.domain.service.HttpExecutor
import com.dd.sms.hook.features.dispatch.domain.service.KeepAliveController
import com.dd.sms.hook.features.settings.domain.model.AppSettings
import com.dd.sms.hook.features.settings.domain.model.RetentionPeriod
import com.dd.sms.hook.features.settings.domain.model.ThemeMode
import com.dd.sms.hook.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory repositories and services shared by unit tests. */
class FakeApiConfigRepository(initial: List<ApiConfig> = emptyList()) : ApiConfigRepository {
    val configs: MutableStateFlow<List<ApiConfig>> = MutableStateFlow(initial)
    private var nextId: Long = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeAll(): Flow<List<ApiConfig>> = configs
    override fun observeEnabledCount(): Flow<Int> = configs.map { list -> list.count { it.enabled } }
    override suspend fun getById(id: Long): ApiConfig? = configs.value.firstOrNull { it.id == id }
    override suspend fun getEnabled(): List<ApiConfig> = configs.value.filter { it.enabled }

    override suspend fun save(config: ApiConfig): Long {
        if (config.isNew) {
            val id: Long = nextId++
            configs.update { it + config.copy(id = id) }
            return id
        }
        configs.update { list -> list.map { if (it.id == config.id) config else it } }
        return config.id
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        configs.update { list -> list.map { if (it.id == id) it.copy(enabled = enabled) else it } }
    }

    override suspend fun delete(id: Long) {
        configs.update { list -> list.filterNot { it.id == id } }
    }
}

class FakeCallLogRepository(initial: List<CallLog> = emptyList()) : CallLogRepository {
    val logs: MutableStateFlow<List<CallLog>> = MutableStateFlow(initial)
    var deletedBefore: Long? = null
    private var nextId: Long = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observe(filter: CallLogFilter, limit: Int): Flow<List<CallLog>> = logs.map { list ->
        list.filter { filter.status == null || it.status == filter.status }
            .filter { filter.configId == null || it.configId == filter.configId }
            .filter { filter.query.isBlank() || it.configName.contains(filter.query, ignoreCase = true) }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }

    override fun observeById(id: Long): Flow<CallLog?> = logs.map { list -> list.firstOrNull { it.id == id } }
    override fun observeAttempts(smsId: Long, configId: Long): Flow<List<CallLog>> =
        logs.map { list -> list.filter { it.smsId == smsId && it.configId == configId }.sortedBy { it.createdAt } }
    override fun observeFailedSmsIds(configId: Long): Flow<List<Long>> = logs.map { list ->
        list.filter { it.configId == configId && it.smsId != null && it.trigger != CallTrigger.TEST }
            .groupBy { it.smsId!! }
            .mapValues { (_, attempts) -> attempts.maxBy { it.id } }
            .values.filter { it.status == CallStatus.FAILED }
            .sortedBy { it.id }
            .map { it.smsId!! }
    }

    override fun observeHasSuccess(): Flow<Boolean> =
        logs.map { list -> list.any { it.status == CallStatus.SUCCESS && it.trigger != CallTrigger.TEST } }
    override suspend fun getById(id: Long): CallLog? = logs.value.firstOrNull { it.id == id }

    override suspend fun insert(log: CallLog): Long {
        val id: Long = nextId++
        logs.update { it + log.copy(id = id) }
        return id
    }

    override suspend fun delete(id: Long) = logs.update { list -> list.filterNot { it.id == id } }
    override suspend fun clearAll() = logs.update { emptyList() }

    override suspend fun deleteByConfig(configId: Long): Int {
        val before: Int = logs.value.size
        logs.update { list -> list.filterNot { it.configId == configId } }
        return before - logs.value.size
    }

    override suspend fun deleteOlderThan(epochMillis: Long): Int {
        deletedBefore = epochMillis
        val before: Int = logs.value.size
        logs.update { list -> list.filter { it.createdAt >= epochMillis } }
        return before - logs.value.size
    }

    private fun real(from: Long): List<CallLog> = logs.value.filter { it.createdAt >= from && it.trigger != CallTrigger.TEST }

    override fun observeSummary(fromMillis: Long): Flow<CallSummary> = logs.map {
        val list: List<CallLog> = real(fromMillis)
        CallSummary(list.size, list.count { it.status == CallStatus.SUCCESS }, list.map { it.durationMs }.average().takeIf { !it.isNaN() }?.toLong() ?: 0L)
    }

    override fun observeTimeline(fromMillis: Long): Flow<List<CallPoint>> =
        logs.map { real(fromMillis).map { CallPoint(it.createdAt, it.status == CallStatus.SUCCESS) } }

    override fun observeBreakdown(fromMillis: Long, limit: Int): Flow<List<ApiCallBreakdown>> = logs.map {
        real(fromMillis).groupBy { it.configId }.map { (id, list) ->
            ApiCallBreakdown(id, list.first().configName, list.size, list.count { it.status == CallStatus.SUCCESS })
        }.sortedByDescending { it.total }.take(limit)
    }
}

class FakeReceivedSmsRepository : ReceivedSmsRepository {
    val sms: MutableStateFlow<List<ReceivedSms>> = MutableStateFlow(emptyList())
    var deletedBefore: Long? = null
    private var nextId: Long = 1

    override suspend fun insert(sms: ReceivedSms): Long {
        val id: Long = nextId++
        this.sms.update { it + sms.copy(id = id) }
        return id
    }

    override suspend fun getById(id: Long): ReceivedSms? = sms.value.firstOrNull { it.id == id }
    override suspend fun setMatchedCount(id: Long, count: Int) =
        sms.update { list -> list.map { if (it.id == id) it.copy(matchedCount = count) else it } }
    override fun observeCountSince(fromMillis: Long): Flow<Int> = sms.map { list -> list.count { it.receivedAt >= fromMillis } }

    override suspend fun deleteBySubscription(subscriptionId: Int): Int {
        val before: Int = sms.value.size
        sms.update { list -> list.filterNot { it.subscriptionId == subscriptionId } }
        return before - sms.value.size
    }

    override suspend fun deleteOlderThan(epochMillis: Long): Int {
        deletedBefore = epochMillis
        val before: Int = sms.value.size
        sms.update { list -> list.filter { it.receivedAt >= epochMillis } }
        return before - sms.value.size
    }
}

class FakeSettingsRepository(initial: AppSettings = AppSettings.DEFAULT) : SettingsRepository {
    private val state: MutableStateFlow<AppSettings> = MutableStateFlow(initial)

    override val settings: Flow<AppSettings> = state
    override suspend fun current(): AppSettings = state.value
    override suspend fun setForwardingEnabled(enabled: Boolean) = state.update { it.copy(forwardingEnabled = enabled) }
    override suspend fun setKeepAliveEnabled(enabled: Boolean) = state.update { it.copy(keepAliveEnabled = enabled) }
    override suspend fun setNotifyOnFailure(enabled: Boolean) = state.update { it.copy(notifyOnFailure = enabled) }
    override suspend fun setRetention(retention: RetentionPeriod) = state.update { it.copy(retention = retention) }
    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) = state.update { it.copy(dynamicColor = enabled) }
}

/** Records every enqueue instead of touching WorkManager. */
class FakeCallScheduler : CallScheduler {
    val enqueued: MutableList<Triple<Long, Long, CallTrigger>> = mutableListOf()
    val queue: MutableStateFlow<List<QueuedCall>> = MutableStateFlow(emptyList())

    override fun enqueue(configId: Long, smsId: Long, trigger: CallTrigger) {
        enqueued += Triple(configId, smsId, trigger)
    }

    override fun observeQueue(): Flow<List<QueuedCall>> = queue
}

/** Answers with [result] and remembers the last request. */
class FakeHttpExecutor(var result: HttpResult = HttpResult.Response(200, "ok", 10)) : HttpExecutor {
    val requests: MutableList<HttpRequestSpec> = mutableListOf()

    override suspend fun execute(request: HttpRequestSpec, timeoutSeconds: Int): HttpResult {
        requests += request
        return result
    }
}

class FakeKeepAliveController : KeepAliveController {
    var running: Boolean = false

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }
}
