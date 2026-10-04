package com.dd.sms.hook.features.calllog.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.AppThemeExtras
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.shared.presentation.ui.IconBadge
import com.dd.sms.hook.shared.presentation.ui.PillTone
import com.dd.sms.hook.shared.presentation.ui.StatusPill
import com.dd.sms.hook.shared.presentation.ui.UiFormat
import com.dd.sms.hook.shared.presentation.ui.currentLocale
import com.dd.sms.hook.features.calllog.domain.model.CallLog
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.model.CallTrigger
import java.util.Locale

private const val BADGE_ALPHA = 0.16f

/** What the row's timestamp shows: the full date in mixed lists, only the time under a day header. */
enum class RowTimestamp { DATE_TIME, TIME }

/** Public row for a call; also used by the dashboard's recent list. */
@Composable
fun CallLogRow(
    log: CallLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = Dimens.screenGutter,
    timestamp: RowTimestamp = RowTimestamp.DATE_TIME,
) {
    val success: Boolean = log.status == CallStatus.SUCCESS
    val statusColor: Color = if (success) AppThemeExtras.statusColors.success else AppThemeExtras.statusColors.failure
    val locale: Locale = currentLocale()
    val time: String = when (timestamp) {
        RowTimestamp.DATE_TIME -> TimeUtils.formatDateTime(log.createdAt, locale)
        RowTimestamp.TIME -> TimeUtils.formatTime(log.createdAt, locale)
    }
    val meta: String = listOfNotNull(
        time,
        UiFormat.duration(log.durationMs),
        if (log.attempt > 1) stringResource(R.string.history_attempt, log.attempt) else null,
    ).joinToString(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = Dimens.inlineGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.cardPadding),
    ) {
        IconBadge(
            icon = if (success) Icons.Filled.Check else Icons.Filled.PriorityHigh,
            containerColor = statusColor.copy(alpha = BADGE_ALPHA),
            contentColor = statusColor,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)) {
            Text(text = log.configName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(R.string.history_row_sms, log.smsSender, log.smsBody),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)) {
            Text(text = statusLabel(log), style = MaterialTheme.typography.labelLarge)
            if (log.trigger != CallTrigger.SMS) TriggerPill(log.trigger)
        }
    }
}

@Composable
fun statusLabel(log: CallLog): String =
    log.responseCode?.toString() ?: stringResource(
        if (log.status == CallStatus.SUCCESS) R.string.status_success else R.string.status_error
    )

@Composable
fun TriggerPill(trigger: CallTrigger) {
    val icon: ImageVector = when (trigger) {
        CallTrigger.SMS -> Icons.Filled.Sms
        CallTrigger.TEST -> Icons.Filled.Science
        CallTrigger.RETRY -> Icons.Filled.Refresh
    }

    StatusPill(label = triggerLabel(trigger), tone = PillTone.NEUTRAL, icon = icon)
}

@Composable
private fun triggerLabel(trigger: CallTrigger): String = stringResource(
    when (trigger) {
        CallTrigger.SMS -> R.string.trigger_sms
        CallTrigger.TEST -> R.string.trigger_test
        CallTrigger.RETRY -> R.string.trigger_retry
    }
)
