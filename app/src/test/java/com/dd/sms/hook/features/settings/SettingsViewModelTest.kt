package com.dd.sms.hook.features.settings

import app.cash.turbine.test
import com.dd.sms.hook.R
import com.dd.sms.hook.features.devtools.domain.service.DemoDataFactory
import com.dd.sms.hook.features.devtools.domain.usecase.ClearDemoDataUseCase
import com.dd.sms.hook.features.devtools.domain.usecase.SeedDemoDataUseCase
import com.dd.sms.hook.features.dispatch.domain.service.RequestFactory
import com.dd.sms.hook.features.dispatch.domain.service.TemplateRenderer
import com.dd.sms.hook.features.settings.domain.model.RetentionPeriod
import com.dd.sms.hook.features.settings.domain.model.ThemeMode
import com.dd.sms.hook.features.settings.domain.usecase.ObserveSettingsUseCase
import com.dd.sms.hook.features.settings.domain.usecase.UpdateSettingsUseCase
import com.dd.sms.hook.features.settings.presentation.SettingsViewModel
import com.dd.sms.hook.testing.FakeApiConfigRepository
import com.dd.sms.hook.testing.FakeCallLogRepository
import com.dd.sms.hook.testing.FakeKeepAliveController
import com.dd.sms.hook.testing.FakeReceivedSmsRepository
import com.dd.sms.hook.testing.FakeSettingsRepository
import com.dd.sms.hook.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository()
    private val keepAlive = FakeKeepAliveController()
    private val configs = FakeApiConfigRepository()
    private val logs = FakeCallLogRepository()
    private val sms = FakeReceivedSmsRepository()
    private val vm by lazy {
        val clear = ClearDemoDataUseCase(configs, logs, sms)
        val seed = SeedDemoDataUseCase(clear, DemoDataFactory(), RequestFactory(TemplateRenderer()), configs, logs, sms)

        SettingsViewModel(ObserveSettingsUseCase(repository), UpdateSettingsUseCase(repository), keepAlive, seed, clear)
    }

    @Test
    fun `keep alive switch persists and starts or stops the service`() = runTest {
        vm.onKeepAliveChange(true)
        assertTrue(repository.current().keepAliveEnabled)
        assertTrue(keepAlive.running)

        vm.onKeepAliveChange(false)
        assertFalse(repository.current().keepAliveEnabled)
        assertFalse(keepAlive.running)
    }

    @Test
    fun `other switches are stored`() = runTest {
        vm.onForwardingChange(false)
        vm.onNotifyOnFailureChange(false)
        vm.onRetentionChange(RetentionPeriod.WEEK)
        vm.onThemeModeChange(ThemeMode.LIGHT)
        vm.onDynamicColorChange(true)

        val settings = repository.current()
        assertFalse(settings.forwardingEnabled)
        assertFalse(settings.notifyOnFailure)
        assertEquals(RetentionPeriod.WEEK, settings.retention)
        assertEquals(ThemeMode.LIGHT, settings.themeMode)
        assertTrue(settings.dynamicColor)
    }

    @Test
    fun `demo data is added then removed and the screen is told`() = runTest {
        vm.messages.test {
            vm.onSeedDemoData()
            assertEquals(R.string.settings_demo_added, awaitItem().res)
            assertTrue(configs.configs.value.isNotEmpty())
            assertTrue(logs.logs.value.isNotEmpty())

            vm.onClearDemoData()
            assertEquals(R.string.settings_demo_removed, awaitItem().res)
            assertTrue(configs.configs.value.isEmpty())
            assertTrue(sms.sms.value.isEmpty())
        }
        assertFalse(vm.demoBusy.value)
    }
}
