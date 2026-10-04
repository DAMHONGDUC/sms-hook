package com.dd.sms.hook.shared.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dd.sms.hook.shared.presentation.theme.AppThemeExtras
import com.dd.sms.hook.shared.presentation.theme.CodeFontFamily
import com.dd.sms.hook.shared.presentation.theme.Dimens

private const val PILL_BACKGROUND_ALPHA = 0.14f

/** Pill colour role. Pills always pair an icon with a label so colour is never the only signal. */
enum class PillTone { SUCCESS, FAILURE, NEUTRAL }

@Composable
fun StatusPill(label: String, tone: PillTone, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val colors = AppThemeExtras.statusColors
    val color: Color = when (tone) {
        PillTone.SUCCESS -> colors.success
        PillTone.FAILURE -> colors.failure
        PillTone.NEUTRAL -> MaterialTheme.colorScheme.tertiary
    }
    val resolvedIcon: ImageVector = icon ?: when (tone) {
        PillTone.SUCCESS -> Icons.Filled.CheckCircle
        PillTone.FAILURE -> Icons.Filled.Error
        PillTone.NEUTRAL -> Icons.Filled.Science
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = PILL_BACKGROUND_ALPHA), RoundedCornerShape(Dimens.chipRadius))
            .padding(horizontal = Dimens.inlineGap, vertical = Dimens.smallGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.smallGap),
    ) {
        Icon(resolvedIcon, contentDescription = null, tint = color, modifier = Modifier.size(Dimens.iconSmall))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun MethodTag(method: String, modifier: Modifier = Modifier) {
    Text(
        text = method,
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(Dimens.chipRadius))
            .padding(horizontal = Dimens.inlineGap, vertical = Dimens.smallGap),
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = CodeFontFamily),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}
