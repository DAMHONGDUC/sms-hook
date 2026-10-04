package com.dd.sms.hook.features.calllog.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.navigation.CallDetailRoute
import com.dd.sms.hook.shared.presentation.ui.UiMessage
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.usecase.DeleteCallLogUseCase
import com.dd.sms.hook.features.calllog.domain.usecase.ObserveCallAttemptsUseCase
import com.dd.sms.hook.features.calllog.domain.usecase.ObserveCallLogUseCase
import com.dd.sms.hook.features.dispatch.domain.usecase.RetryCallUseCase
import com.dd.sms.hook.features.dispatch.domain.usecase.RetryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CallDetailViewModel"
private const val STOP_TIMEOUT_MILLIS = 5_000L

sealed interface CallDetailState {
    data object Loading : CallDetailState
    data object NotFound : CallDetailState
    /** [attempts] is every try for the same SMS and API, oldest first; empty for test calls. */
    data class Loaded(val log: CallLog, val attempts: List<CallLog>) : CallDetailState
}

sealed interface CallDetailEvent {
    data object Deleted : CallDetailEvent
    data class Message(val message: UiMessage) : CallDetailEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CallDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeCallLog: ObserveCallLogUseCase,
    observeAttempts: ObserveCallAttemptsUseCase,
    private val retryCall: RetryCallUseCase,
    private val deleteCallLog: DeleteCallLogUseCase,
) : ViewModel() {
    private val logId: Long = savedStateHandle.toRoute<CallDetailRoute>().id
    private val eventChannel: Channel<CallDetailEvent> = Channel(Channel.BUFFERED)

    val events: Flow<CallDetailEvent> = eventChannel.receiveAsFlow()
    val state: StateFlow<CallDetailState> = observeCallLog(logId)
        .flatMapLatest { log ->
            val smsId: Long? = log?.smsId
            val configId: Long? = log?.configId
            when {
                log == null -> flowOf(CallDetailState.NotFound)
                smsId == null || configId == null -> flowOf(CallDetailState.Loaded(log, emptyList()))
                else -> observeAttempts(smsId, configId).map { CallDetailState.Loaded(log, it) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CallDetailState.Loading)

    fun onRetry() {
        AppLogger.i(TAG, "retry tapped - {log: $logId}")
        viewModelScope.launch {
            val message: Int = try {
                when (retryCall(logId)) {
                    RetryResult.QUEUED -> R.string.detail_retry_queued
                    RetryResult.CONFIG_DELETED -> R.string.detail_retry_config_deleted
                    RetryResult.NOT_RETRYABLE -> R.string.detail_retry_not_possible
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "retry failed - {log: $logId}", e)
                R.string.error_generic
            }
            eventChannel.send(CallDetailEvent.Message(UiMessage(message)))
        }
    }

    fun onDelete() {
        AppLogger.i(TAG, "delete confirmed - {log: $logId}")
        viewModelScope.launch {
            try {
                deleteCallLog(logId)
                eventChannel.send(CallDetailEvent.Deleted)
            } catch (e: Exception) {
                AppLogger.e(TAG, "delete failed - {log: $logId}", e)
                eventChannel.send(CallDetailEvent.Message(UiMessage(R.string.error_generic)))
            }
        }
    }
}
