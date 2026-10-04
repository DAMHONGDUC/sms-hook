package com.dd.sms.hook.shared.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.dd.sms.hook.BuildConfig
import com.dd.sms.hook.R
import com.dd.sms.hook.shared.presentation.theme.Dimens

/** Where a screen sits decides who owns the bottom inset: the bottom bar (top level) or this scaffold. */
enum class ScreenLevel { TOP, DETAIL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    level: ScreenLevel,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val surface: Color = MaterialTheme.colorScheme.surface

    Scaffold(
        modifier = modifier,
        contentWindowInsets = if (level == ScreenLevel.TOP) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            // One compact bar for every screen: the title sits in it, tabs just use a larger style.
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap)) {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = if (level == ScreenLevel.TOP) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (BuildConfig.IS_DEV) EnvironmentTag()
                    }
                },
                navigationIcon = {
                    if (onNavigateUp != null) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = actions,
                // Same colour scrolled or not: no tint overlay when content moves under the bar.
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surface, scrolledContainerColor = surface),
            )
        },
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(snackbarHostState) },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        content = content,
    )
}

/** Marks dev builds on every screen so they are never mistaken for prod. */
@Composable
private fun EnvironmentTag() {
    Text(
        text = BuildConfig.ENV.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(Dimens.chipRadius))
            .padding(horizontal = Dimens.inlineGap, vertical = Dimens.smallGap),
    )
}
