package com.dd.sms.hook.features.dashboard

import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.dashboard.domain.model.DashboardRange
import com.dd.sms.hook.features.dashboard.domain.service.DailyAggregator
import com.dd.sms.hook.features.dashboard.domain.usecase.ObserveDashboardUseCase
import com.dd.sms.hook.features.dashboard.presentation.DashboardState
import com.dd.sms.hook.features.dashboard.presentation.DashboardViewModel
import com.dd.sms.hook.testing.FakeApiConfigRepository
import com.dd.sms.hook.testing.FakeCallLogRepository
import com.dd.sms.hook.testing.FakeReceivedSmsRepository
import com.dd.sms.hook.testing.FakeSettingsRepository
import com.dd.sms.hook.testing.Fixtures
import com.dd.sms.hook.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `switching the range reloads data for that range`() = runTest {
        val logs = FakeCallLogRepository(listOf(Fixtures.log(id = 1L, createdAt = TimeUtils.now() - 10 * TimeUtils.MILLIS_PER_DAY)))
        val vm = DashboardViewModel(
            ObserveDashboardUseCase(logs, FakeReceivedSmsRepository(), FakeApiConfigRepository(), FakeSettingsRepository(), DailyAggregator())
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }

        assertEquals(0, (vm.state.value as DashboardState.Loaded).data.summary.total)
        vm.onRangeChange(DashboardRange.MONTH)
        val data = (vm.state.value as DashboardState.Loaded).data
        assertEquals(DashboardRange.MONTH, data.range)
        assertEquals(30, data.daily.size)
        assertEquals(1, data.summary.total)
    }
}
