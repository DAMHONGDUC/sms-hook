package com.dd.sms.hook.features.apiconfig.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.shared.presentation.ui.UiMessage
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.usecase.DeleteApiConfigUseCase
import com.dd.sms.hook.features.apiconfig.domain.usecase.DuplicateApiConfigUseCase
import com.dd.sms.hook.features.apiconfig.domain.usecase.ObserveApiConfigsUseCase
import com.dd.sms.hook.features.apiconfig.domain.usecase.SetApiConfigEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ApiListViewModel"
private const val STOP_TIMEOUT_MILLIS = 5_000L

sealed interface ApiListState {
    data object Loading : ApiListState
    data class Loaded(val configs: List<ApiConfig>) : ApiListState
}

@HiltViewModel
class ApiListViewModel @Inject constructor(
    observeApiConfigs: ObserveApiConfigsUseCase,
    private val setEnabled: SetApiConfigEnabledUseCase,
    private val deleteConfig: DeleteApiConfigUseCase,
    private val duplicateConfig: DuplicateApiConfigUseCase,
) : ViewModel() {
    private val messageChannel: Channel<UiMessage> = Channel(Channel.BUFFERED)

    val messages: Flow<UiMessage> = messageChannel.receiveAsFlow()
    val state: StateFlow<ApiListState> = observeApiConfigs()
        .map<List<ApiConfig>, ApiListState> { ApiListState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ApiListState.Loading)

    fun onToggle(config: ApiConfig, enabled: Boolean) {
        AppLogger.i(TAG, "toggle tapped - {id: ${config.id}, enabled: $enabled}")
        viewModelScope.launch {
            try {
                setEnabled(config.id, enabled)
            } catch (e: Exception) {
                AppLogger.e(TAG, "toggle failed - {id: ${config.id}}", e)
                messageChannel.send(UiMessage(R.string.error_generic))
            }
        }
    }

    fun onDuplicate(config: ApiConfig, copySuffix: String) {
        AppLogger.i(TAG, "duplicate tapped - {id: ${config.id}}")
        viewModelScope.launch {
            try {
                duplicateConfig(config, copySuffix)
                messageChannel.send(UiMessage(R.string.api_list_duplicated, listOf(config.name)))
            } catch (e: Exception) {
                AppLogger.e(TAG, "duplicate failed - {id: ${config.id}}", e)
                messageChannel.send(UiMessage(R.string.error_generic))
            }
        }
    }

    fun onDelete(config: ApiConfig) {
        AppLogger.i(TAG, "delete confirmed - {id: ${config.id}}")
        viewModelScope.launch {
            try {
                deleteConfig(config.id)
                messageChannel.send(UiMessage(R.string.api_list_deleted, listOf(config.name)))
            } catch (e: Exception) {
                AppLogger.e(TAG, "delete failed - {id: ${config.id}}", e)
                messageChannel.send(UiMessage(R.string.error_generic))
            }
        }
    }
}
