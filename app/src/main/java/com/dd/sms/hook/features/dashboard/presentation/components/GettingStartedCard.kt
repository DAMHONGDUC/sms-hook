package com.dd.sms.hook.features.dashboard.presentation.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.AppThemeExtras
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.theme.tabularNumbers
import com.dd.sms.hook.shared.presentation.ui.IconBadge
import com.dd.sms.hook.shared.presentation.ui.SectionCard

/** One onboarding step: done, or the single action that completes it. */
data class SetupStep(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    val done: Boolean,
    @param:StringRes val actionLabel: Int?,
    val onAction: () -> Unit,
)

/** Guided setup for new users; the dashboard hides it once every step is done. */
@Composable
fun GettingStartedCard(steps: List<SetupStep>) {
    val doneCount: Int = steps.count { it.done }
    val progress: Float by animateFloatAsState(targetValue = doneCount.toFloat() / steps.size, label = "setup progress")

    SectionCard(title = stringResource(R.string.getting_started_title), icon = Icons.Filled.RocketLaunch) {
        Text(
            text = stringResource(R.string.getting_started_progress, doneCount, steps.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.progressHeight)
                .clip(RoundedCornerShape(Dimens.progressHeight)),
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
        steps.forEachIndexed { index, step -> StepRow(number = index + 1, step = step) }
    }
}

@Composable
private fun StepRow(number: Int, step: SetupStep) {
    val successColor = AppThemeExtras.statusColors.success

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.rowGap),
    ) {
        if (step.done) {
            IconBadge(icon = Icons.Filled.Check, size = Dimens.badgeSmall, containerColor = successColor, contentColor = MaterialTheme.colorScheme.surface)
        } else {
            NumberBadge(number)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)) {
            Text(
                text = stringResource(step.title),
                style = MaterialTheme.typography.titleSmall,
                color = if (step.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (!step.done) {
                Text(
                    text = stringResource(step.description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val action: Int? = step.actionLabel
        if (!step.done && action != null) {
            FilledTonalButton(onClick = step.onAction) {
                Text(text = stringResource(action), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** A pending step shows its number instead of a check. */
@Composable
private fun NumberBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(Dimens.badgeSmall)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
