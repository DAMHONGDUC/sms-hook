package com.dd.sms.hook.features.calllog.domain.repository

import com.dd.sms.hook.features.calllog.domain.model.ApiCallBreakdown
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallLogFilter
import com.dd.sms.hook.features.calllog.domain.model.CallPoint
import com.dd.sms.hook.features.calllog.domain.model.CallSummary
import kotlinx.coroutines.flow.Flow

interface CallLogRepository {
    fun observe(filter: CallLogFilter, limit: Int): Flow<List<CallLog>>

    fun observeById(id: Long): Flow<CallLog?>

    /** Every attempt (first call and retries) for one SMS sent to one API, oldest first. */
    fun observeAttempts(smsId: Long, configId: Long): Flow<List<CallLog>>

    /** SMS ids, oldest first, whose latest real (non-test) attempt for this API failed. */
    fun observeFailedSmsIds(configId: Long): Flow<List<Long>>

    /** True once any real (non-test) call has succeeded. */
    fun observeHasSuccess(): Flow<Boolean>

    suspend fun getById(id: Long): CallLog?

    suspend fun insert(log: CallLog): Long

    suspend fun delete(id: Long)

    suspend fun clearAll()

    suspend fun deleteOlderThan(epochMillis: Long): Int

    /** Analytics below exclude TEST calls. */
    fun observeSummary(fromMillis: Long): Flow<CallSummary>

    fun observeTimeline(fromMillis: Long): Flow<List<CallPoint>>

    fun observeBreakdown(fromMillis: Long, limit: Int): Flow<List<ApiCallBreakdown>>
}
