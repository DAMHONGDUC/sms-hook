package com.dd.sms.hook.shared.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.dd.sms.hook.shared.presentation.theme.Dimens

private const val BORDER_ALPHA = 0.08f

/** Cards stay flat; a faint primary-tinted border gives depth that works in light and dark. */
object AppCardDefaults {
    @Composable
    fun border(): BorderStroke = BorderStroke(Dimens.cardBorder, MaterialTheme.colorScheme.primary.copy(alpha = BORDER_ALPHA))
}
