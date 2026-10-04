package com.dd.sms.hook.features.calllog.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.theme.tabularNumbers
import com.dd.sms.hook.shared.presentation.ui.ConfirmDialog
import com.dd.sms.hook.shared.presentation.ui.EmptyState
import com.dd.sms.hook.shared.presentation.ui.LoadingState
import com.dd.sms.hook.shared.presentation.ui.MessageEffect
import com.dd.sms.hook.shared.presentation.ui.ScreenLevel
import com.dd.sms.hook.shared.presentation.ui.ScreenScaffold
import com.dd.sms.hook.shared.presentation.ui.currentLocale
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.apiconfig.domain.model.ApiConfig
import com.dd.sms.hook.features.calllog.domain.model.CallLogFilter
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.presentation.components.CallLogRow
import com.dd.sms.hook.features.calllog.presentation.components.RowTimestamp
import com.dd.sms.hook.features.dispatch.domain.model.QueuedCallItem
import java.time.LocalDate
import java.util.Locale

@Composable
fun HistoryScreen(
    onOpenCall: (Long) -> Unit,
    onOpenApis: () -> Unit,
    onNavigateUp: (() -> Unit)?,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state: HistoryState by viewModel.state.collectAsStateWithLifecycle()
    val filter: CallLogFilter by viewModel.filter.collectAsStateWithLifecycle()
    val apis: List<ApiConfig> by viewModel.apis.collectAsStateWithLifecycle()
    val queue: List<QueuedCallItem> by viewModel.queue.collectAsStateWithLifecycle()
    val retryableFailures: Int by viewModel.retryableFailures.collectAsStateWithLifecycle()
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    var confirmClear: Boolean by rememberSaveable { mutableStateOf(false) }
    var confirmRetryAll: Boolean by rememberSaveable { mutableStateOf(false) }
    var tab: HistoryTab by rememberSaveable { mutableStateOf(HistoryTab.SENT) }

    MessageEffect(viewModel.messages, snackbarHostState)

    ScreenScaffold(
        title = stringResource(if (viewModel.isScopedToApi) R.string.history_title_api else R.string.history_title),
        level = ScreenLevel.TOP,
        onNavigateUp = onNavigateUp,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = { confirmClear = true }) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.history_clear))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            HistoryTabs(selected = tab, queued = queue.size, onSelect = { tab = it })
            if (tab == HistoryTab.QUEUE) {
                ApiChips(filter, apis, viewModel, modifier = Modifier.padding(top = Dimens.inlineGap))
                QueueList(queue)
            } else {
                HistoryFilters(filter, apis, viewModel)
                if (filter.configId != null && retryableFailures > 0) {
                    RetryFailedBanner(count = retryableFailures, onRetryAll = { confirmRetryAll = true })
                }
                SentContent(state, filter, viewModel, onOpenCall, onOpenApis)
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear),
            message = stringResource(R.string.history_clear_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::onClearAll,
            onDismiss = { confirmClear = false },
        )
    }
    if (confirmRetryAll) {
        val apiName: String = apis.firstOrNull { it.id == filter.configId }?.name.orEmpty()

        ConfirmDialog(
            title = stringResource(R.string.history_retry_failed_confirm_title),
            message = pluralStringResource(R.plurals.history_retry_failed_confirm_message, retryableFailures, retryableFailures, apiName),
            confirmLabel = stringResource(R.string.history_retry_failed_action),
            onConfirm = viewModel::onRetryAllFailed,
            onDismiss = { confirmRetryAll = false },
            destructive = false,
        )
    }
}

@Composable
private fun SentContent(
    state: HistoryState,
    filter: CallLogFilter,
    viewModel: HistoryViewModel,
    onOpenCall: (Long) -> Unit,
    onOpenApis: () -> Unit,
) {
    when (state) {
        HistoryState.Loading -> LoadingState()
        is HistoryState.Loaded -> if (state.isEmpty && filter != CallLogFilter.ALL) {
            EmptyState(
                icon = Icons.Filled.FilterAltOff,
                title = stringResource(R.string.history_empty_filtered_title),
                message = stringResource(R.string.history_empty_filtered_message),
                actionLabel = stringResource(R.string.history_clear_filters),
                onAction = viewModel::onClearFilters,
            )
        } else if (state.isEmpty) {
            EmptyState(
                icon = Icons.Filled.History,
                title = stringResource(R.string.history_empty_title),
                message = stringResource(R.string.history_empty_message),
                actionLabel = stringResource(R.string.history_empty_cta),
                onAction = onOpenApis,
            )
        } else {
            HistoryList(days = state.days, onOpenCall = onOpenCall)
        }
    }
}

private enum class HistoryTab { SENT, QUEUE }

@Composable
private fun HistoryTabs(selected: HistoryTab, queued: Int, onSelect: (HistoryTab) -> Unit) {
    PrimaryTabRow(selectedTabIndex = selected.ordinal) {
        Tab(
            selected = selected == HistoryTab.SENT,
            onClick = { onSelect(HistoryTab.SENT) },
            text = { Text(text = stringResource(R.string.history_tab_sent), style = MaterialTheme.typography.labelLarge) },
        )
        Tab(
            selected = selected == HistoryTab.QUEUE,
            onClick = { onSelect(HistoryTab.QUEUE) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.history_tab_queue), style = MaterialTheme.typography.labelLarge)
                    if (queued > 0) {
                        Badge { Text(text = queued.toString(), style = MaterialTheme.typography.labelSmall.tabularNumbers()) }
                    }
                }
            },
        )
    }
}

@Composable
private fun HistoryList(days: List<HistoryDay>, onOpenCall: (Long) -> Unit) {
    val locale: Locale = currentLocale()
    val today: LocalDate = LocalDate.now()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Dimens.listItemGap)) {
        days.forEach { group ->
            stickyHeader(key = group.day.toEpochDay()) {
                Text(
                    text = when (group.day) {
                        today -> stringResource(R.string.history_today)
                        today.minusDays(1) -> stringResource(R.string.history_yesterday)
                        else -> TimeUtils.formatDate(group.day, locale)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = Dimens.screenGutter, vertical = Dimens.inlineGap),
                )
            }
            items(group.logs, key = { it.id }) { log ->
                CallLogRow(log = log, onClick = { onOpenCall(log.id) }, timestamp = RowTimestamp.TIME)
            }
        }
    }
}

@Composable
private fun HistoryFilters(filter: CallLogFilter, apis: List<ApiConfig>, viewModel: HistoryViewModel) {
    Column(modifier = Modifier.padding(top = Dimens.inlineGap), verticalArrangement = Arrangement.spacedBy(Dimens.inlineGap)) {
        OutlinedTextField(
            value = filter.query,
            onValueChange = viewModel::onQueryChange,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(text = stringResource(R.string.history_search), style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            shape = RoundedCornerShape(Dimens.searchRadius),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedBorderColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenGutter),
        )
        Row(modifier = Modifier.padding(horizontal = Dimens.screenGutter), horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap)) {
            StatusChip(R.string.history_filter_all, filter.status == null) { viewModel.onStatusFilter(null) }
            StatusChip(R.string.status_success, filter.status == CallStatus.SUCCESS) {
                viewModel.onStatusFilter(CallStatus.SUCCESS)
            }
            StatusChip(R.string.status_failed, filter.status == CallStatus.FAILED) {
                viewModel.onStatusFilter(CallStatus.FAILED)
            }
        }
        ApiChips(filter, apis, viewModel)
    }
}

/** Tappable API filters, shared by both tabs so the queue can be narrowed the same way. */
@Composable
private fun ApiChips(filter: CallLogFilter, apis: List<ApiConfig>, viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    if (apis.size <= 1) return
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Dimens.screenGutter),
        horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap),
    ) {
        item {
            FilterChip(
                selected = filter.configId == null,
                onClick = { viewModel.onApiFilter(null) },
                label = { Text(text = stringResource(R.string.history_filter_all_apis), style = MaterialTheme.typography.labelLarge) },
            )
        }
        items(apis, key = { it.id }) { api ->
            FilterChip(
                selected = filter.configId == api.id,
                onClick = { viewModel.onApiFilter(api.id) },
                leadingIcon = { Icon(Icons.Filled.Api, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
                label = { Text(text = api.name, style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}

@Composable
private fun StatusChip(label: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = stringResource(label), style = MaterialTheme.typography.labelLarge) },
    )
}
