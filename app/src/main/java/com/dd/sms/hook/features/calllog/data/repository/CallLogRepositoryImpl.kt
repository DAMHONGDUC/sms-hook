package com.dd.sms.hook.features.calllog.data.repository

import com.dd.sms.hook.features.calllog.data.local.CallLogDao
import com.dd.sms.hook.features.calllog.data.local.CallLogMapper
import com.dd.sms.hook.features.calllog.domain.model.ApiCallBreakdown
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallLogFilter
import com.dd.sms.hook.features.calllog.domain.model.CallPoint
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallSummary
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.calllog.domain.repository.CallLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CallLogRepositoryImpl @Inject constructor(
    private val dao: CallLogDao,
) : CallLogRepository {
    private val success: String = CallStatus.SUCCESS.name
    private val excluded: String = CallTrigger.TEST.name

    override fun observe(filter: CallLogFilter, limit: Int): Flow<List<CallLog>> =
        dao.observe(filter.status?.name, filter.configId, filter.query.trim(), limit)
            .map { list -> list.map(CallLogMapper::toDomain) }

    override fun observeById(id: Long): Flow<CallLog?> =
        dao.observeById(id).map { it?.let(CallLogMapper::toDomain) }

    override fun observeAttempts(smsId: Long, configId: Long): Flow<List<CallLog>> =
        dao.observeAttempts(smsId, configId).map { list -> list.map(CallLogMapper::toDomain) }

    override fun observeFailedSmsIds(configId: Long): Flow<List<Long>> =
        dao.observeFailedSmsIds(configId, CallStatus.FAILED.name, excluded)

    override fun observeHasSuccess(): Flow<Boolean> = dao.observeHasSuccess(success, excluded)

    override suspend fun getById(id: Long): CallLog? = dao.getById(id)?.let(CallLogMapper::toDomain)

    override suspend fun insert(log: CallLog): Long = dao.insert(CallLogMapper.toEntity(log))

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    override suspend fun deleteOlderThan(epochMillis: Long): Int = dao.deleteOlderThan(epochMillis)

    override suspend fun deleteByConfig(configId: Long): Int = dao.deleteByConfig(configId)

    override fun observeSummary(fromMillis: Long): Flow<CallSummary> =
        dao.observeSummary(fromMillis, success, excluded).map { row ->
            CallSummary(total = row.total, success = row.success, avgDurationMs = row.avgDuration?.toLong() ?: 0L)
        }

    override fun observeTimeline(fromMillis: Long): Flow<List<CallPoint>> =
        dao.observeTimeline(fromMillis, excluded).map { rows ->
            rows.map { CallPoint(createdAt = it.createdAt, success = it.status == success) }
        }

    override fun observeBreakdown(fromMillis: Long, limit: Int): Flow<List<ApiCallBreakdown>> =
        dao.observeBreakdown(fromMillis, success, excluded, limit).map { rows ->
            rows.map { ApiCallBreakdown(it.configId, it.configName, it.total, it.success) }
        }
}
