package com.dd.sms.hook

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dd.sms.hook.navigation.AppRoot
import com.dd.sms.hook.shared.presentation.theme.AppTheme
import com.dd.sms.hook.features.settings.domain.model.AppSettings
import com.dd.sms.hook.features.settings.domain.model.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

/** AppCompat so the in-app language picker works on Android 12 and below. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    /** A call log opened from a failure notification, consumed once by the nav host. */
    private val pendingCallLogId: MutableStateFlow<Long?> = MutableStateFlow(null)
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        readDeepLink(intent)
        setContent {
            val settings: AppSettings? by viewModel.settings.collectAsStateWithLifecycle()
            val darkTheme: Boolean = isDark(settings?.themeMode ?: ThemeMode.SYSTEM)

            SystemBarsEffect(darkTheme)
            AppTheme(darkTheme = darkTheme, dynamicColor = settings?.dynamicColor ?: false) {
                AppRoot(pendingCallLogId = pendingCallLogId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readDeepLink(intent)
    }

    /** Status and navigation bar icons follow the app theme, not only the system one. */
    @Composable
    private fun SystemBarsEffect(darkTheme: Boolean) {
        DisposableEffect(darkTheme) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            )
            onDispose { }
        }
    }

    @Composable
    private fun isDark(mode: ThemeMode): Boolean = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    private fun readDeepLink(intent: Intent?) {
        val id: Long = intent?.getLongExtra(EXTRA_CALL_LOG_ID, NO_ID) ?: NO_ID

        if (id != NO_ID) pendingCallLogId.value = id
    }

    companion object {
        const val EXTRA_CALL_LOG_ID = "extra_call_log_id"
        private const val NO_ID = -1L
    }
}
