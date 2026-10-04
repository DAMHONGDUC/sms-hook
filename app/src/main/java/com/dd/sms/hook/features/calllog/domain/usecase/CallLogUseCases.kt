package com.dd.sms.hook.features.calllog.domain.usecase

import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallLogFilter
import com.dd.sms.hook.features.calllog.domain.repository.CallLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

private const val TAG = "CallLogUseCases"

object CallLogLimits {
    const val HISTORY_LIMIT = 500
    const val RECENT_LIMIT = 5
}

class ObserveCallLogsUseCase @Inject constructor(private val repository: CallLogRepository) {
    operator fun invoke(filter: CallLogFilter, limit: Int = CallLogLimits.HISTORY_LIMIT): Flow<List<CallLog>> =
        repository.observe(filter, limit)
}

class ObserveCallLogUseCase @Inject constructor(private val repository: CallLogRepository) {
    operator fun invoke(id: Long): Flow<CallLog?> = repository.observeById(id)
}

class ObserveCallAttemptsUseCase @Inject constructor(private val repository: CallLogRepository) {
    operator fun invoke(smsId: Long, configId: Long): Flow<List<CallLog>> = repository.observeAttempts(smsId, configId)
}

class DeleteCallLogUseCase @Inject constructor(private val repository: CallLogRepository) {
    suspend operator fun invoke(id: Long) {
        repository.delete(id)
        AppLogger.i(TAG, "deleted - {id: $id}")
    }
}

class ClearCallLogsUseCase @Inject constructor(private val repository: CallLogRepository) {
    suspend operator fun invoke() {
        repository.clearAll()
        AppLogger.i(TAG, "history cleared")
    }
}
