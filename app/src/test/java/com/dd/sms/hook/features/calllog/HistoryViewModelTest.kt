package com.dd.sms.hook.features.calllog

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import com.dd.sms.hook.navigation.HistoryRoute
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.features.apiconfig.domain.usecase.ObserveApiConfigsUseCase
import com.dd.sms.hook.features.calllog.domain.model.CallLogFilter
import com.dd.sms.hook.features.calllog.domain.model.CallStatus
import com.dd.sms.hook.features.calllog.domain.usecase.ClearCallLogsUseCase
import com.dd.sms.hook.features.calllog.domain.usecase.ObserveCallLogsUseCase
import com.dd.sms.hook.features.calllog.presentation.list.HistoryState
import com.dd.sms.hook.features.calllog.presentation.list.HistoryViewModel
import com.dd.sms.hook.testing.FakeApiConfigRepository
import com.dd.sms.hook.testing.FakeCallLogRepository
import com.dd.sms.hook.testing.Fixtures
import com.dd.sms.hook.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

private const val PAST_DEBOUNCE_MILLIS = 300L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val now: Long = TimeUtils.now()
    private val logs = FakeCallLogRepository(
        listOf(
            Fixtures.log(id = 1L, configId = 1L, createdAt = now, status = CallStatus.SUCCESS),
            Fixtures.log(id = 2L, configId = 2L, createdAt = now - 1000, status = CallStatus.FAILED, configName = "Backup"),
            Fixtures.log(id = 3L, configId = 1L, createdAt = now - 2 * TimeUtils.MILLIS_PER_DAY),
        )
    )

    private fun viewModel(configId: Long = HistoryRoute.ALL_CONFIGS): HistoryViewModel = HistoryViewModel(
        SavedStateHandle(route = HistoryRoute(configId)),
        ObserveCallLogsUseCase(logs),
        ObserveApiConfigsUseCase(FakeApiConfigRepository(listOf(Fixtures.config(id = 1L), Fixtures.config(id = 2L)))),
        ClearCallLogsUseCase(logs),
    )

    private fun TestScope.loaded(vm: HistoryViewModel): HistoryState.Loaded {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        advanceTimeBy(PAST_DEBOUNCE_MILLIS)
        return vm.state.value as HistoryState.Loaded
    }

    @Test
    fun `logs are grouped by local day, newest day first`() = runTest {
        val state: HistoryState.Loaded = loaded(viewModel())

        assertEquals(listOf(LocalDate.now(), LocalDate.now().minusDays(2)), state.days.map { it.day })
        assertEquals(listOf(1L, 2L), state.days.first().logs.map { it.id })
    }

    @Test
    fun `status and api filters narrow the list`() = runTest {
        val vm: HistoryViewModel = viewModel()

        vm.onStatusFilter(CallStatus.FAILED)
        assertEquals(listOf(2L), loaded(vm).days.flatMap { it.logs }.map { it.id })
        vm.onStatusFilter(null)
        vm.onApiFilter(1L)
        advanceTimeBy(PAST_DEBOUNCE_MILLIS)
        assertEquals(listOf(1L, 3L), (vm.state.value as HistoryState.Loaded).days.flatMap { it.logs }.map { it.id })
    }

    @Test
    fun `route config id preselects the api filter and clear resets everything`() = runTest {
        val vm: HistoryViewModel = viewModel(configId = 2L)

        assertTrue(vm.isScopedToApi)
        assertEquals(listOf(2L), loaded(vm).days.flatMap { it.logs }.map { it.id })
        vm.onQueryChange("zzz")
        vm.onClearFilters()
        assertEquals(CallLogFilter.ALL, vm.filter.value)
    }

    @Test
    fun `clear all empties the history`() = runTest {
        val vm: HistoryViewModel = viewModel()

        vm.onClearAll()

        assertTrue(loaded(vm).isEmpty)
    }
}
