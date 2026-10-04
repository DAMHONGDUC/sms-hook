package com.dd.sms.hook.features.dashboard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.ui.IconBadge
import com.dd.sms.hook.features.dashboard.domain.model.DashboardData

private const val TAG_ALPHA = 0.12f
private const val GLOW_ALPHA = 0.35f

/** Is forwarding live, and what to do if not. Active uses the primary container, paused a neutral one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusHeroCard(data: DashboardData, onOpenSettings: () -> Unit) {
    val active: Boolean = data.forwardingEnabled && data.enabledApis > 0
    val container: Color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content: Color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(modifier = Modifier.padding(Dimens.heroPadding), verticalArrangement = Arrangement.spacedBy(Dimens.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.cardPadding)) {
                Box(contentAlignment = Alignment.Center) {
                    // A static, soft glow marks "live"; no pulsing so the screen stays calm.
                    if (active) {
                        val glow: Color = MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .size(Dimens.glow)
                                .drawBehind { drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = GLOW_ALPHA), Color.Transparent))) },
                        )
                    }
                    IconBadge(
                        icon = if (active) Icons.Filled.Sensors else Icons.Filled.PauseCircle,
                        size = Dimens.badgeLarge,
                        containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)) {
                    Text(
                        text = stringResource(if (active) R.string.dashboard_status_active else R.string.dashboard_status_paused),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = when {
                            !data.forwardingEnabled -> stringResource(R.string.dashboard_status_forwarding_off)
                            data.enabledApis == 0 -> stringResource(R.string.dashboard_status_no_apis)
                            else -> stringResource(R.string.dashboard_status_listening)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap), verticalArrangement = Arrangement.spacedBy(Dimens.inlineGap)) {
                HeroTag(Icons.Filled.Api, pluralStringResource(R.plurals.dashboard_status_detail, data.enabledApis, data.enabledApis), content)
                HeroTag(
                    Icons.Filled.Bolt,
                    stringResource(if (data.keepAliveEnabled) R.string.dashboard_keep_alive_on else R.string.dashboard_keep_alive_off),
                    content,
                )
            }
            if (!data.forwardingEnabled) {
                HeroButton(Icons.Filled.Settings, stringResource(R.string.dashboard_open_settings), onOpenSettings)
            }
        }
    }
}

@Composable
private fun HeroTag(icon: ImageVector, label: String, color: Color) {
    Surface(shape = RoundedCornerShape(Dimens.chipRadius), color = color.copy(alpha = TAG_ALPHA), contentColor = color) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.inlineGap, vertical = Dimens.smallGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.smallGap),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.iconSmall))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun HeroButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Button(onClick = onClick, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
    }
}
