package com.dd.sms.hook.features.calllog.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.AppThemeExtras
import com.dd.sms.hook.shared.presentation.theme.CodeFontFamily
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.shared.presentation.ui.CodeBlock
import com.dd.sms.hook.shared.presentation.ui.ConfirmDialog
import com.dd.sms.hook.shared.presentation.ui.EmptyState
import com.dd.sms.hook.shared.presentation.ui.IconBadge
import com.dd.sms.hook.shared.presentation.ui.currentLocale
import com.dd.sms.hook.shared.presentation.ui.KeyValueRow
import com.dd.sms.hook.shared.presentation.ui.LoadingState
import com.dd.sms.hook.shared.presentation.ui.MethodTag
import com.dd.sms.hook.shared.presentation.ui.ScreenLevel
import com.dd.sms.hook.shared.presentation.ui.ScreenScaffold
import com.dd.sms.hook.shared.presentation.ui.SectionCard
import com.dd.sms.hook.shared.presentation.ui.UiFormat
import com.dd.sms.hook.shared.presentation.ui.rememberCopyAction
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.calllog.presentation.components.TriggerPill

private val BOTTOM_BAR_ELEVATION = 3.dp

@Composable
fun CallDetailScreen(
    onNavigateUp: () -> Unit,
    onEditApi: (Long) -> Unit,
    viewModel: CallDetailViewModel = hiltViewModel(),
) {
    val state: CallDetailState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    var confirmDelete: Boolean by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CallDetailEvent.Deleted -> onNavigateUp()
                is CallDetailEvent.Message -> snackbarHostState.showSnackbar(resources.getString(event.message.res))
            }
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.detail_title),
        level = ScreenLevel.DETAIL,
        onNavigateUp = onNavigateUp,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            val loaded: CallDetailState.Loaded? = state as? CallDetailState.Loaded
            if (loaded != null) CallActionBar(log = loaded.log, onRetry = viewModel::onRetry, onEditApi = onEditApi)
        },
        actions = {
            if (state is CallDetailState.Loaded) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        },
    ) { padding ->
        when (val current: CallDetailState = state) {
            CallDetailState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            CallDetailState.NotFound -> EmptyState(
                icon = Icons.Filled.SearchOff,
                title = stringResource(R.string.detail_not_found),
                message = stringResource(R.string.detail_not_found_message),
                modifier = Modifier.padding(padding),
            )
            is CallDetailState.Loaded -> CallDetailContent(
                log = current.log,
                attempts = current.attempts,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.detail_delete_title),
            message = stringResource(R.string.detail_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun CallDetailContent(log: CallLog, attempts: List<CallLog>, modifier: Modifier) {
    val copy: (String) -> Unit = rememberCopyAction()
    val success: Boolean = log.status == CallStatus.SUCCESS
    val statusColor: Color = if (success) AppThemeExtras.statusColors.success else AppThemeExtras.statusColors.failure

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap),
    ) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.cardPadding)) {
                IconBadge(
                    icon = if (success) Icons.Filled.Check else Icons.Filled.PriorityHigh,
                    size = Dimens.badgeLarge,
                    containerColor = statusColor,
                    contentColor = MaterialTheme.colorScheme.surface,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)) {
                    Text(
                        text = stringResource(if (success) R.string.status_success else R.string.status_failed) +
                            (log.responseCode?.let { " · HTTP $it" } ?: ""),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(text = log.configName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TriggerPill(log.trigger)
            }
            KeyValueRow(stringResource(R.string.detail_time), TimeUtils.formatDateTime(log.createdAt, currentLocale()))
            KeyValueRow(stringResource(R.string.detail_duration), UiFormat.duration(log.durationMs))
            KeyValueRow(stringResource(R.string.detail_attempt), log.attempt.toString())
            if (log.errorMessage != null) KeyValueRow(stringResource(R.string.detail_error), log.errorMessage)
        }
        if (attempts.size > 1) DeliveryTimeline(attempts = attempts, currentId = log.id)
        SectionCard(title = stringResource(R.string.detail_sms), icon = Icons.Filled.Sms) {
            KeyValueRow(stringResource(R.string.detail_sender), log.smsSender)
            CodeBlock(label = stringResource(R.string.detail_message), text = log.smsBody, onCopy = copy)
        }
        SectionCard(title = stringResource(R.string.detail_request), icon = Icons.Filled.Upload) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap)) {
                MethodTag(log.method)
                SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                    Text(text = log.url, style = MaterialTheme.typography.bodySmall.copy(fontFamily = CodeFontFamily))
                }
            }
            if (log.requestHeaders.isNotEmpty()) {
                CodeBlock(
                    label = stringResource(R.string.detail_request_headers),
                    text = log.requestHeaders.joinToString("\n") { "${it.name}: ${it.value}" },
                    onCopy = copy,
                )
            }
            CodeBlock(label = stringResource(R.string.detail_request_body), text = log.requestBody, onCopy = copy)
        }
        SectionCard(title = stringResource(R.string.detail_response), icon = Icons.Filled.Download) {
            CodeBlock(
                label = stringResource(R.string.detail_response_body),
                text = log.responseBody ?: log.errorMessage.orEmpty(),
                onCopy = copy,
            )
        }
    }
}

/** Next steps in the thumb zone: fix the API or send the call again. */
@Composable
private fun CallActionBar(log: CallLog, onRetry: () -> Unit, onEditApi: (Long) -> Unit) {
    val configId: Long? = log.configId
    val canRetry: Boolean = log.trigger != CallTrigger.TEST && configId != null && log.smsId != null

    if (configId == null) return
    Surface(tonalElevation = BOTTOM_BAR_ELEVATION, shadowElevation = BOTTOM_BAR_ELEVATION) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screenGutter, vertical = Dimens.listItemGap),
            horizontalArrangement = Arrangement.spacedBy(Dimens.listItemGap),
        ) {
            OutlinedButton(onClick = { onEditApi(configId) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Text(
                    text = stringResource(R.string.detail_edit_api),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
            if (canRetry) {
                Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text(
                        text = stringResource(R.string.detail_retry),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                    )
                }
            }
        }
    }
}
