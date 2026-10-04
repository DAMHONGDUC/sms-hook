package com.dd.sms.hook.features.apiconfig.presentation.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.domain.constants.HttpConstants
import com.dd.sms.hook.shared.presentation.theme.CodeFontFamily
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.ui.SectionCard
import com.dd.sms.hook.shared.presentation.ui.SwitchRow
import com.dd.sms.hook.features.apiconfig.domain.model.HeaderEntry
import com.dd.sms.hook.features.apiconfig.domain.model.HttpMethod
import com.dd.sms.hook.features.apiconfig.domain.model.MatchMode
import com.dd.sms.hook.features.apiconfig.domain.service.ApiConfigError
import com.dd.sms.hook.features.dispatch.domain.service.TemplateVariables

private const val BODY_MIN_LINES = 6

@Composable
internal fun GeneralSection(state: ApiEditorState, viewModel: ApiEditorViewModel) {
    SectionCard(title = stringResource(R.string.editor_section_general), icon = Icons.Filled.Tune) {
        EditorField(
            value = state.draft.name,
            onValueChange = viewModel::onNameChange,
            label = R.string.editor_name,
            error = ApiConfigError.NAME_EMPTY.takeIf { state.visibleError(it) },
        )
        SwitchRow(title = stringResource(R.string.editor_enabled), checked = state.draft.enabled, onChange = viewModel::onEnabledChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RequestSection(state: ApiEditorState, viewModel: ApiEditorViewModel) {
    SectionCard(title = stringResource(R.string.editor_section_request), icon = Icons.Filled.Http) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            HttpMethod.entries.forEachIndexed { index, method ->
                SegmentedButton(
                    selected = state.draft.method == method,
                    onClick = { viewModel.onMethodChange(method) },
                    shape = SegmentedButtonDefaults.itemShape(index, HttpMethod.entries.size),
                    icon = {},
                ) {
                    Text(text = method.name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        EditorField(
            value = state.draft.url,
            onValueChange = viewModel::onUrlChange,
            label = R.string.editor_url,
            supporting = R.string.editor_url_hint,
            error = ApiConfigError.URL_INVALID.takeIf { state.visibleError(it) },
            keyboardType = KeyboardType.Uri,
            monospace = true,
        )
        HeadersEditor(state, viewModel)
        if (state.draft.method.allowsBody) {
            OutlinedTextField(
                value = state.draft.bodyTemplate,
                onValueChange = viewModel::onBodyChange,
                label = { Text(text = stringResource(R.string.editor_body), style = MaterialTheme.typography.bodySmall) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = CodeFontFamily),
                minLines = BODY_MIN_LINES,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = stringResource(R.string.editor_placeholders_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap)) {
            TemplateVariables.ALL.forEach { name ->
                val token: String = TemplateVariables.token(name)
                AssistChip(
                    onClick = { viewModel.onInsertToken(token) },
                    enabled = state.draft.method.allowsBody,
                    label = {
                        Text(text = token, style = MaterialTheme.typography.labelSmall.copy(fontFamily = CodeFontFamily))
                    },
                )
            }
        }
    }
}

@Composable
private fun HeadersEditor(state: ApiEditorState, viewModel: ApiEditorViewModel) {
    Text(text = stringResource(R.string.editor_headers), style = MaterialTheme.typography.labelLarge)
    state.draft.headers.forEachIndexed { index, header ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.smallGap)) {
            OutlinedTextField(
                value = header.name,
                onValueChange = { viewModel.onHeaderChange(index, header.copy(name = it)) },
                placeholder = { Text(text = stringResource(R.string.editor_header_name), style = MaterialTheme.typography.bodySmall) },
                isError = state.showErrors && header.name.isBlank(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = header.value,
                onValueChange = { viewModel.onHeaderChange(index, header.copy(value = it)) },
                placeholder = { Text(text = stringResource(R.string.editor_header_value), style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.onRemoveHeader(index) }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.editor_header_remove))
            }
        }
    }
    TextButton(onClick = viewModel::onAddHeader) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Text(text = stringResource(R.string.editor_header_add), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun TriggerSection(state: ApiEditorState, viewModel: ApiEditorViewModel) {
    val regex: Boolean = state.draft.filter.mode == MatchMode.REGEX

    SectionCard(title = stringResource(R.string.editor_section_trigger), icon = Icons.Filled.FilterAlt) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            MatchMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.draft.filter.mode == mode,
                    onClick = { viewModel.onMatchModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, MatchMode.entries.size),
                ) {
                    Text(
                        text = stringResource(if (mode == MatchMode.REGEX) R.string.editor_mode_regex else R.string.editor_mode_contains),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        EditorField(
            value = state.draft.filter.senders,
            onValueChange = viewModel::onSendersChange,
            label = R.string.editor_senders,
            supporting = if (regex) R.string.editor_senders_hint_regex else R.string.editor_senders_hint,
            error = ApiConfigError.SENDER_REGEX_INVALID.takeIf { state.visibleError(it) },
            monospace = regex,
        )
        EditorField(
            value = state.draft.filter.keyword,
            onValueChange = viewModel::onKeywordChange,
            label = R.string.editor_keyword,
            supporting = if (regex) R.string.editor_keyword_hint_regex else R.string.editor_keyword_hint,
            error = ApiConfigError.KEYWORD_REGEX_INVALID.takeIf { state.visibleError(it) },
            monospace = regex,
        )
    }
}

@Composable
internal fun DeliverySection(state: ApiEditorState, viewModel: ApiEditorViewModel) {
    SectionCard(title = stringResource(R.string.editor_section_delivery), icon = Icons.Filled.Schedule) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap)) {
            EditorField(
                value = state.timeoutText,
                onValueChange = viewModel::onTimeoutChange,
                label = R.string.editor_timeout,
                supportingText = stringResource(R.string.editor_timeout_hint, HttpConstants.MAX_TIMEOUT_SECONDS),
                error = ApiConfigError.TIMEOUT_OUT_OF_RANGE.takeIf { state.visibleError(it) },
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            EditorField(
                value = state.retriesText,
                onValueChange = viewModel::onRetriesChange,
                label = R.string.editor_retries,
                supportingText = stringResource(R.string.editor_retries_hint, HttpConstants.MAX_RETRIES_LIMIT),
                error = ApiConfigError.RETRIES_OUT_OF_RANGE.takeIf { state.visibleError(it) },
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    modifier: Modifier = Modifier,
    @StringRes supporting: Int? = null,
    supportingText: String? = supporting?.let { stringResource(it) },
    error: ApiConfigError? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    monospace: Boolean = false,
) {
    val message: String? = error?.let { stringResource(errorText(it)) } ?: supportingText

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(label), style = MaterialTheme.typography.bodySmall) },
        supportingText = message?.let { { Text(text = it, style = MaterialTheme.typography.bodySmall) } },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = if (monospace) {
            MaterialTheme.typography.bodyMedium.copy(fontFamily = CodeFontFamily)
        } else {
            MaterialTheme.typography.bodyLarge
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@StringRes
private fun errorText(error: ApiConfigError): Int = when (error) {
    ApiConfigError.NAME_EMPTY -> R.string.editor_error_name
    ApiConfigError.URL_INVALID -> R.string.editor_error_url
    ApiConfigError.SENDER_REGEX_INVALID, ApiConfigError.KEYWORD_REGEX_INVALID -> R.string.editor_error_regex
    ApiConfigError.HEADER_NAME_EMPTY -> R.string.editor_error_header
    ApiConfigError.TIMEOUT_OUT_OF_RANGE -> R.string.editor_error_timeout
    ApiConfigError.RETRIES_OUT_OF_RANGE -> R.string.editor_error_retries
}
