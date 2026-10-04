package com.dd.sms.hook.features.apiconfig.presentation.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.domain.logging.AppLogger
import com.dd.sms.hook.navigation.ApiEditorRoute
import com.dd.sms.hook.shared.presentation.ui.UiMessage
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfigDefaults
import com.dd.sms.hook.features.apiconfig.domain.model.HeaderEntry
import com.dd.sms.hook.features.apiconfig.domain.model.HttpMethod
import com.dd.sms.hook.features.apiconfig.domain.model.MatchMode
import com.dd.sms.hook.features.apiconfig.domain.service.ApiConfigError
import com.dd.sms.hook.features.apiconfig.domain.service.ApiConfigValidator
import com.dd.sms.hook.features.apiconfig.domain.usecase.GetApiConfigUseCase
import com.dd.sms.hook.features.apiconfig.domain.usecase.SaveApiConfigResult
import com.dd.sms.hook.features.apiconfig.domain.usecase.SaveApiConfigUseCase
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.dispatch.domain.usecase.TestApiCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ApiEditorViewModel"
private const val INVALID_NUMBER = -1

sealed interface TestCallState {
    data object Idle : TestCallState
    data object Running : TestCallState
    data class Done(val log: CallLog) : TestCallState
}

data class ApiEditorState(
    val loading: Boolean = true,
    val draft: ApiConfig = ApiConfigDefaults.newConfig(),
    val timeoutText: String = draft.timeoutSeconds.toString(),
    val retriesText: String = draft.maxRetries.toString(),
    val errors: Set<ApiConfigError> = emptySet(),
    val showErrors: Boolean = false,
    val saving: Boolean = false,
    val testCall: TestCallState = TestCallState.Idle,
) {
    fun visibleError(error: ApiConfigError): Boolean = showErrors && error in errors
}

sealed interface ApiEditorEvent {
    data class Saved(val name: String) : ApiEditorEvent
    data class Message(val message: UiMessage) : ApiEditorEvent
}

@HiltViewModel
class ApiEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getApiConfig: GetApiConfigUseCase,
    private val saveApiConfig: SaveApiConfigUseCase,
    private val testApiCall: TestApiCallUseCase,
    private val validator: ApiConfigValidator,
) : ViewModel() {
    private val configId: Long = savedStateHandle.toRoute<ApiEditorRoute>().id
    private val mutableState: MutableStateFlow<ApiEditorState> = MutableStateFlow(ApiEditorState())
    private val eventChannel: Channel<ApiEditorEvent> = Channel(Channel.BUFFERED)

    val state: StateFlow<ApiEditorState> = mutableState.asStateFlow()
    val events: Flow<ApiEditorEvent> = eventChannel.receiveAsFlow()
    val isNew: Boolean = configId == ApiEditorRoute.NEW

    init {
        load()
    }

    fun onNameChange(value: String) = edit { it.copy(name = value) }

    fun onUrlChange(value: String) = edit { it.copy(url = value) }

    fun onMethodChange(value: HttpMethod) = edit { it.copy(method = value) }

    fun onBodyChange(value: String) = edit { it.copy(bodyTemplate = value) }

    fun onInsertToken(token: String) = edit { it.copy(bodyTemplate = it.bodyTemplate + token) }

    fun onEnabledChange(value: Boolean) = edit { it.copy(enabled = value) }

    fun onSendersChange(value: String) = edit { it.copy(filter = it.filter.copy(senders = value)) }

    fun onKeywordChange(value: String) = edit { it.copy(filter = it.filter.copy(keyword = value)) }

    fun onMatchModeChange(value: MatchMode) = edit { it.copy(filter = it.filter.copy(mode = value)) }

    fun onAddHeader() = edit { it.copy(headers = it.headers + HeaderEntry("", "")) }

    fun onHeaderChange(index: Int, header: HeaderEntry) =
        edit { config -> config.copy(headers = config.headers.mapIndexed { i, h -> if (i == index) header else h }) }

    fun onRemoveHeader(index: Int) =
        edit { config -> config.copy(headers = config.headers.filterIndexed { i, _ -> i != index }) }

    fun onTimeoutChange(text: String) {
        mutableState.update { it.copy(timeoutText = text) }
        edit { it.copy(timeoutSeconds = text.toIntOrNull() ?: INVALID_NUMBER) }
    }

    fun onRetriesChange(text: String) {
        mutableState.update { it.copy(retriesText = text) }
        edit { it.copy(maxRetries = text.toIntOrNull() ?: INVALID_NUMBER) }
    }

    fun onSave() {
        val draft: ApiConfig = mutableState.value.draft

        AppLogger.i(TAG, "save tapped - {id: ${draft.id}, name: ${draft.name}}")
        if (mutableState.value.saving) return
        mutableState.update { it.copy(saving = true, showErrors = true) }
        viewModelScope.launch {
            try {
                when (val result: SaveApiConfigResult = saveApiConfig(draft)) {
                    is SaveApiConfigResult.Saved -> eventChannel.send(ApiEditorEvent.Saved(draft.name.trim()))
                    is SaveApiConfigResult.Invalid -> {
                        mutableState.update { it.copy(errors = result.errors) }
                        eventChannel.send(ApiEditorEvent.Message(UiMessage(R.string.editor_fix_errors)))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "save failed - {id: ${draft.id}}", e)
                eventChannel.send(ApiEditorEvent.Message(UiMessage(R.string.error_generic)))
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun onRunTest(sampleSender: String, sampleBody: String) {
        val draft: ApiConfig = mutableState.value.draft
        val errors: Set<ApiConfigError> = validator.validate(draft.copy(name = draft.name.ifBlank { "test" }))

        AppLogger.i(TAG, "test tapped - {id: ${draft.id}, url: ${draft.url}, sender: $sampleSender}")
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(errors = errors, showErrors = true) }
            viewModelScope.launch { eventChannel.send(ApiEditorEvent.Message(UiMessage(R.string.editor_fix_errors))) }
            return
        }
        mutableState.update { it.copy(testCall = TestCallState.Running) }
        viewModelScope.launch {
            try {
                val log: CallLog = testApiCall(draft, sampleSender, sampleBody)
                mutableState.update { it.copy(testCall = TestCallState.Done(log)) }
            } catch (e: Exception) {
                AppLogger.e(TAG, "test call failed - {id: ${draft.id}}", e)
                mutableState.update { it.copy(testCall = TestCallState.Idle) }
                eventChannel.send(ApiEditorEvent.Message(UiMessage(R.string.error_generic)))
            }
        }
    }

    fun onDismissTest() {
        mutableState.update { it.copy(testCall = TestCallState.Idle) }
    }

    private fun load() {
        if (isNew) {
            mutableState.update { it.copy(loading = false) }
            return
        }
        viewModelScope.launch {
            try {
                val config: ApiConfig? = getApiConfig(configId)
                if (config == null) {
                    AppLogger.w(TAG, "config not found - {id: $configId}")
                    eventChannel.send(ApiEditorEvent.Message(UiMessage(R.string.editor_not_found)))
                    mutableState.update { it.copy(loading = false) }
                    return@launch
                }
                mutableState.update {
                    ApiEditorState(
                        loading = false,
                        draft = config,
                        timeoutText = config.timeoutSeconds.toString(),
                        retriesText = config.maxRetries.toString(),
                    )
                }
                AppLogger.i(TAG, "loaded - {id: $configId, name: ${config.name}}")
            } catch (e: Exception) {
                AppLogger.e(TAG, "load failed - {id: $configId}", e)
                mutableState.update { it.copy(loading = false) }
                eventChannel.send(ApiEditorEvent.Message(UiMessage(R.string.error_generic)))
            }
        }
    }

    private fun edit(transform: (ApiConfig) -> ApiConfig) {
        mutableState.update { state ->
            val draft: ApiConfig = transform(state.draft)
            state.copy(draft = draft, errors = validator.validate(draft))
        }
    }
}
