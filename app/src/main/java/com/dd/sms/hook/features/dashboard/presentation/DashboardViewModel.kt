package com.dd.sms.hook.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.features.dashboard.domain.model.DashboardData
import com.dd.sms.hook.features.dashboard.domain.model.DashboardRange
import com.dd.sms.hook.features.dashboard.domain.usecase.ObserveDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val TAG = "DashboardViewModel"
private const val STOP_TIMEOUT_MILLIS = 5_000L

sealed interface DashboardState {
    data object Loading : DashboardState
    data object Error : DashboardState
    data class Loaded(val data: DashboardData) : DashboardState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
) : ViewModel() {
    private val mutableRange: MutableStateFlow<DashboardRange> = MutableStateFlow(DashboardRange.WEEK)

    val range: StateFlow<DashboardRange> = mutableRange.asStateFlow()
    val state: StateFlow<DashboardState> = mutableRange
        .flatMapLatest { observeDashboard(it) }
        .map<DashboardData, DashboardState> { DashboardState.Loaded(it) }
        .catch { error ->
            AppLogger.e(TAG, "dashboard stream failed - {range: ${mutableRange.value}}", error)
            emit(DashboardState.Error)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DashboardState.Loading)

    fun onRangeChange(range: DashboardRange) {
        AppLogger.i(TAG, "range changed - {range: $range}")
        mutableRange.value = range
    }
}
