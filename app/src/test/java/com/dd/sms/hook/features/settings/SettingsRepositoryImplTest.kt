package com.dd.sms.hook.features.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.dd.sms.hook.features.settings.data.repository.SettingsRepositoryImpl
import com.dd.sms.hook.features.settings.domain.model.AppSettings
import com.dd.sms.hook.features.settings.domain.model.RetentionPeriod
import com.dd.sms.hook.features.settings.domain.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun repository(scope: CoroutineScope, file: File = File(folder.root, "settings.preferences_pb")): SettingsRepositoryImpl =
        SettingsRepositoryImpl(PreferenceDataStoreFactory.create(scope = scope) { file })

    @Test
    fun `empty store reads defaults`() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

        assertEquals(AppSettings.DEFAULT, repository(scope).current())
    }

    @Test
    fun `every setting is persisted`() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val repo: SettingsRepositoryImpl = repository(scope)

        repo.setForwardingEnabled(false)
        repo.setKeepAliveEnabled(true)
        repo.setNotifyOnFailure(false)
        repo.setRetention(RetentionPeriod.FOREVER)
        repo.setThemeMode(ThemeMode.DARK)
        repo.setDynamicColor(true)

        assertEquals(
            AppSettings(
                forwardingEnabled = false,
                keepAliveEnabled = true,
                notifyOnFailure = false,
                retention = RetentionPeriod.FOREVER,
                themeMode = ThemeMode.DARK,
                dynamicColor = true,
            ),
            repo.current(),
        )
    }
}
