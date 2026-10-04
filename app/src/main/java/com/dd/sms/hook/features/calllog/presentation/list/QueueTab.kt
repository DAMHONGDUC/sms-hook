package com.dd.sms.hook.features.calllog.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.dd.sms.hook.R
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import com.dd.sms.hook.features.calllog.presentation.components.TriggerPill
import com.dd.sms.hook.features.dispatch.domain.model.QueueState
import com.dd.sms.hook.features.dispatch.domain.model.QueuedCallItem
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.theme.tabularNumbers
import com.dd.sms.hook.shared.presentation.ui.EmptyState
import com.dd.sms.hook.shared.presentation.ui.IconBadge
import com.dd.sms.hook.shared.presentation.ui.currentLocale
import java.util.Locale

/** Read-only queue: rows are not clickable because the queue sends them by itself. */
@Composable
fun QueueList(items: List<QueuedCallItem>) {
    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Inbox,
            title = stringResource(R.string.queue_empty_title),
            message = stringResource(R.string.queue_empty_message),
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Dimens.listItemGap)) {
        item { QueueNotice() }
        items(items, key = { it.call.workId }) { item -> QueueRow(item) }
    }
}

@Composable
private fun QueueNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenGutter, vertical = Dimens.inlineGap),
        horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(R.string.queue_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QueueRow(item: QueuedCallItem) {
    val locale: Locale = currentLocale()
    val running: Boolean = item.call.state == QueueState.RUNNING
    val nextRunAt: Long? = item.call.nextRunAt?.takeIf { it > TimeUtils.now() }
    val stateLabel: String = when {
        running -> stringResource(R.string.queue_state_running)
        nextRunAt != null -> stringResource(R.string.queue_retry_at, TimeUtils.formatTime(nextRunAt, locale))
        else -> stringResource(R.string.queue_state_waiting)
    }
    val meta: String = listOfNotNull(
        stateLabel,
        if (item.call.attempt > 1) stringResource(R.string.history_attempt, item.call.attempt) else null,
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenGutter, vertical = Dimens.inlineGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.cardPadding),
    ) {
        IconBadge(icon = if (running) Icons.Filled.CloudUpload else Icons.Filled.Schedule)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)) {
            Text(
                text = item.configName ?: stringResource(R.string.queue_unknown_api),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.sms?.let { sms ->
                Text(
                    text = stringResource(R.string.history_row_sms, sms.sender, sms.body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall.tabularNumbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.call.trigger != CallTrigger.SMS) TriggerPill(item.call.trigger)
    }
}

/** Shown above the history of one API when some of its calls ended failed. */
@Composable
fun RetryFailedBanner(count: Int, onRetryAll: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.screenGutter, end = Dimens.screenGutter, top = Dimens.inlineGap),
    ) {
        Row(
            modifier = Modifier.padding(start = Dimens.screenGutter, end = Dimens.inlineGap, top = Dimens.smallGap, bottom = Dimens.smallGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text(
                text = pluralStringResource(R.plurals.history_retry_failed_count, count, count),
                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetryAll) {
                Text(text = stringResource(R.string.history_retry_failed_action), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
