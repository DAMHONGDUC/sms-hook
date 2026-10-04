package com.dd.sms.hook.features.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.AppThemeExtras
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.presentation.theme.tabularNumbers
import com.dd.sms.hook.shared.presentation.ui.AppCardDefaults
import com.dd.sms.hook.shared.presentation.ui.IconBadge
import com.dd.sms.hook.features.calllog.domain.model.ApiCallBreakdown

private val SEGMENT_GAP = 2.dp

/** A headline number with its icon and label. */
@Composable
fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = AppCardDefaults.border(),
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(Dimens.inlineGap)) {
            IconBadge(icon = icon, size = Dimens.badgeSmall)
            Text(text = value, style = MaterialTheme.typography.headlineSmall.tabularNumbers())
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One API's calls, its bar length relative to the busiest API and split into success and failed. */
@Composable
fun ApiBreakdownRow(item: ApiCallBreakdown, maxTotal: Int, fallbackName: String) {
    val colors = AppThemeExtras.statusColors
    val failed: Int = item.total - item.success
    val fill: Float = item.total.toFloat() / maxTotal.coerceAtLeast(1)
    val summary: String = stringResource(R.string.breakdown_value, item.success, item.total)

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.smallGap)) {
        Row {
            Text(
                text = item.configName.ifBlank { fallbackName },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = summary, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(fill.coerceIn(0.02f, 1f))
                .height(Dimens.progressHeight)
                .clearAndSetSemantics { contentDescription = summary },
        ) {
            if (item.success > 0) {
                Box(
                    modifier = Modifier
                        .weight(item.success.toFloat())
                        .height(Dimens.progressHeight)
                        .background(colors.success, RoundedCornerShape(Dimens.barCorner)),
                )
            }
            if (item.success > 0 && failed > 0) Spacer(modifier = Modifier.width(SEGMENT_GAP))
            if (failed > 0) {
                Box(
                    modifier = Modifier
                        .weight(failed.toFloat())
                        .height(Dimens.progressHeight)
                        .background(colors.failure, RoundedCornerShape(Dimens.barCorner)),
                )
            }
        }
    }
}
