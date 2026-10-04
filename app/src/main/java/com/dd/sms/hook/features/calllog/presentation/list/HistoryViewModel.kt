package com.dd.sms.hook.features.calllog.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.navigation.HistoryRoute
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.shared.presentation.ui.UiMessage
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.usecase.ObserveApiConfigsUseCase
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallLogFilter
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.usecase.ClearCallLogsUseCase
import com.dd.sms.hook.features.calllog.domain.usecase.ObserveCallLogsUseCase
import com.dd.sms.hook.features.dispatch.domain.model.QueuedCallItem
import com.dd.sms.hook.features.dispatch.domain.usecase.ObserveCallQueueUseCase
import com.dd.sms.hook.features.dispatch.domain.usecase.ObserveRetryableFailuresUseCase
import com.dd.sms.hook.features.dispatch.domain.usecase.RetryFailedCallsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "HistoryViewModel"
private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val SEARCH_DEBOUNCE_MILLIS = 250L

/** Calls of one local day, newest first. */
data class HistoryDay(val day: LocalDate, val logs: List<CallLog>)

sealed interface HistoryState {
    data object Loading : HistoryState
    data class Loaded(val days: List<HistoryDay>) : HistoryState {
        val isEmpty: Boolean get() = days.isEmpty()
    }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeCallLogs: ObserveCallLogsUseCase,
    observeApiConfigs: ObserveApiConfigsUseCase,
    observeCallQueue: ObserveCallQueueUseCase,
    observeRetryableFailures: ObserveRetryableFailuresUseCase,
    private val clearCallLogs: ClearCallLogsUseCase,
    private val retryFailedCalls: RetryFailedCallsUseCase,
) : ViewModel() {
    private val configId: Long? = savedStateHandle.toRoute<HistoryRoute>().configId
        .takeIf { it != HistoryRoute.ALL_CONFIGS }
    private val mutableFilter: MutableStateFlow<CallLogFilter> =
        MutableStateFlow(CallLogFilter.ALL.copy(configId = configId))
    private val messageChannel: Channel<UiMessage> = Channel(Channel.BUFFERED)

    val filter: StateFlow<CallLogFilter> = mutableFilter.asStateFlow()
    val messages: Flow<UiMessage> = messageChannel.receiveAsFlow()
    val isScopedToApi: Boolean = configId != null
    /** Tappable API filters so the user picks instead of typing a name. */
    val apis: StateFlow<List<ApiConfig>> = observeApiConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
    private val selectedApi: Flow<Long?> = mutableFilter.map { it.configId }.distinctUntilChanged()

    /** Calls still waiting or running; read-only, the queue sends them by itself. */
    val queue: StateFlow<List<QueuedCallItem>> = selectedApi
        .flatMapLatest { observeCallQueue(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
    /** Failed calls of the selected API that "retry all" would queue; 0 when no single API is selected. */
    val retryableFailures: StateFlow<Int> = selectedApi
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else observeRetryableFailures(id) }
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)
    val state: StateFlow<HistoryState> = mutableFilter
        .debounce(SEARCH_DEBOUNCE_MILLIS)
        .flatMapLatest { observeCallLogs(it) }
        .map<List<CallLog>, HistoryState> { logs -> HistoryState.Loaded(groupByDay(logs)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryState.Loading)

    fun onStatusFilter(status: CallStatus?) {
        AppLogger.i(TAG, "status filter - {status: $status}")
        mutableFilter.update { it.copy(status = status) }
    }

    fun onApiFilter(configId: Long?) {
        AppLogger.i(TAG, "api filter - {configId: $configId}")
        mutableFilter.update { it.copy(configId = configId) }
    }

    fun onClearFilters() {
        AppLogger.i(TAG, "filters cleared")
        mutableFilter.value = CallLogFilter.ALL
    }

    fun onQueryChange(query: String) {
        mutableFilter.update { it.copy(query = query) }
    }

    fun onClearAll() {
        AppLogger.i(TAG, "clear all confirmed")
        viewModelScope.launch {
            try {
                clearCallLogs()
                messageChannel.send(UiMessage(R.string.history_cleared))
            } catch (e: Exception) {
                AppLogger.e(TAG, "clear failed", e)
                messageChannel.send(UiMessage(R.string.error_generic))
            }
        }
    }

    fun onRetryAllFailed() {
        val configId: Long = mutableFilter.value.configId ?: return

        AppLogger.i(TAG, "retry all failed confirmed - {configId: $configId}")
        viewModelScope.launch {
            try {
                val queued: Int = retryFailedCalls(configId)
                AppLogger.i(TAG, "retry all failed done - {configId: $configId, queued: $queued}")
                messageChannel.send(UiMessage(R.string.history_retry_failed_queued))
            } catch (e: Exception) {
                AppLogger.e(TAG, "retry all failed - {configId: $configId}", e)
                messageChannel.send(UiMessage(R.string.error_generic))
            }
        }
    }

    /** Logs arrive newest first, so grouping keeps both day and row order. */
    private fun groupByDay(logs: List<CallLog>): List<HistoryDay> =
        logs.groupBy { TimeUtils.toLocalDate(it.createdAt) }.map { (day, items) -> HistoryDay(day, items) }
}
