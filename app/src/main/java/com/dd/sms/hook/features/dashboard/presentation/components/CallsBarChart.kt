package com.dd.sms.hook.features.dashboard.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.AppThemeExtras
import com.dd.sms.hook.shared.presentation.theme.Dimens
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.shared.presentation.ui.currentLocale
import com.dd.sms.hook.features.calllog.domain.model.DailyCallCount
import java.util.Locale

private const val GRID_LINES = 3
private const val NO_SELECTION = -1
private const val GRID_ALPHA = 0.5f
private const val DIMMED_ALPHA = 0.35f
private val SEGMENT_GAP = 2.dp
private val LEGEND_SWATCH = 10.dp

/** Stacked daily bars: success at the baseline, failed on top. Tap a bar to read its values. */
@Composable
fun CallsBarChart(days: List<DailyCallCount>, modifier: Modifier = Modifier) {
    val colors = AppThemeExtras.statusColors
    val gridColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = GRID_ALPHA)
    val maxTotal: Int = (days.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
    val summary: String = stringResource(
        R.string.chart_summary,
        days.size,
        days.sumOf { it.success },
        days.sumOf { it.failed },
    )
    var selected: Int by rememberSaveable(days.size) { mutableIntStateOf(NO_SELECTION) }
    val locale: Locale = currentLocale()
    // Days run oldest to newest in reading direction, so bars mirror in RTL to match the axis labels.
    val rtl: Boolean = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.inlineGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sectionGap)) {
            LegendItem(colors.success, stringResource(R.string.status_success))
            LegendItem(colors.failure, stringResource(R.string.status_failed))
        }
        Text(
            text = days.getOrNull(selected)?.let {
                stringResource(R.string.chart_tooltip, TimeUtils.formatDayLabel(it.day, locale), it.success, it.failed)
            } ?: pluralStringResource(R.plurals.chart_max, maxTotal, maxTotal),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.chartHeight)
                .clearAndSetSemantics { contentDescription = summary }
                .pointerInput(days) {
                    detectTapGestures { offset ->
                        val slotIndex: Int = (offset.x / (size.width / days.size.coerceAtLeast(1))).toInt()
                        val index: Int = if (rtl) days.lastIndex - slotIndex else slotIndex
                        selected = if (index == selected) NO_SELECTION else index.coerceIn(0, days.lastIndex)
                    }
                },
        ) {
            drawGrid(gridColor)
            val slot: Float = size.width / days.size.coerceAtLeast(1)
            val barWidth: Float = slot * (1f - Dimens.barGapRatio)

            days.forEachIndexed { index, day ->
                val alpha: Float = if (selected == NO_SELECTION || selected == index) 1f else DIMMED_ALPHA
                val slotIndex: Int = if (rtl) days.lastIndex - index else index
                val left: Float = slotIndex * slot + (slot - barWidth) / 2f
                drawDayBar(day, left, barWidth, maxTotal, colors.success.copy(alpha = alpha), colors.failure.copy(alpha = alpha))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.firstOrNull()?.let { AxisLabel(TimeUtils.formatDayLabel(it.day, locale)) }
            days.lastOrNull()?.let { AxisLabel(TimeUtils.formatDayLabel(it.day, locale)) }
        }
    }
}

private fun DrawScope.drawGrid(color: Color) {
    for (line in 0..GRID_LINES) {
        val y: Float = size.height * line / GRID_LINES
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
    }
}

private fun DrawScope.drawDayBar(
    day: DailyCallCount,
    left: Float,
    width: Float,
    maxTotal: Int,
    successColor: Color,
    failureColor: Color,
) {
    if (day.total == 0) return
    val gap: Float = SEGMENT_GAP.toPx()
    val successHeight: Float = size.height * day.success / maxTotal
    val failedHeight: Float = size.height * day.failed / maxTotal
    val bothPresent: Boolean = day.success > 0 && day.failed > 0

    if (day.success > 0) {
        drawSegment(successColor, left, size.height - successHeight, width, successHeight, roundedTop = day.failed == 0)
    }
    if (day.failed > 0) {
        val top: Float = size.height - successHeight - failedHeight
        val height: Float = failedHeight - if (bothPresent) gap else 0f
        drawSegment(failureColor, left, top, width, height.coerceAtLeast(1f), roundedTop = true)
    }
}

/** Only the segment at the data end is rounded; the baseline stays square. */
private fun DrawScope.drawSegment(color: Color, left: Float, top: Float, width: Float, height: Float, roundedTop: Boolean) {
    val radius: CornerRadius = if (roundedTop) CornerRadius(Dimens.barCorner.toPx()) else CornerRadius.Zero
    val path: Path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = androidx.compose.ui.geometry.Rect(Offset(left, top), Size(width, height)),
                topLeft = radius,
                topRight = radius,
                bottomRight = CornerRadius.Zero,
                bottomLeft = CornerRadius.Zero,
            )
        )
    }
    drawPath(path, color)
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.smallGap)) {
        Box(modifier = Modifier.size(LEGEND_SWATCH).background(color, RoundedCornerShape(Dimens.barCorner)))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun AxisLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
