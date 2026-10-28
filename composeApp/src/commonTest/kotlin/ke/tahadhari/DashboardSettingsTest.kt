package ke.tahadhari

import ke.tahadhari.data.store.InMemoryKeyValueStore
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.ui.dashboard.DashboardSettings
import ke.tahadhari.ui.dashboard.DashboardSettingsStore
import ke.tahadhari.ui.dashboard.DashboardState
import ke.tahadhari.ui.dashboard.StatusFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DashboardSettingsTest {
    @Test
    fun defaultsWhenNothingSaved() = runTest {
        val loaded = DashboardSettingsStore(InMemoryKeyValueStore()).load()
        assertEquals(DashboardSettings(), loaded)
        assertEquals(Language.ENGLISH, loaded.languageOrDefault())
        assertTrue(loaded.autoRefresh)
    }

    @Test
    fun roundTripsThroughTheStore() = runTest {
        val store = DashboardSettingsStore(InMemoryKeyValueStore())
        store.save(DashboardSettings(Language.SWAHILI.name, 42, "high", autoRefresh = false))
        val loaded = store.load()
        assertEquals(Language.SWAHILI, loaded.languageOrDefault())
        assertEquals(42, loaded.selectedCodeOrNull())
        assertEquals(StatusFilter.Level(RiskLevel.HIGH), loaded.filterOrNull())
        assertEquals(false, loaded.autoRefresh)
    }

    @Test
    fun corruptDataGivesDefaults() = runTest {
        val kv = InMemoryKeyValueStore()
        kv.put(DashboardSettingsStore.KEY, "{not json")
        assertEquals(DashboardSettings(), DashboardSettingsStore(kv).load())
        kv.put(DashboardSettingsStore.KEY, "{\"selectedCode\":\"abc\"}")
        assertEquals(DashboardSettings(), DashboardSettingsStore(kv).load())
    }

    @Test
    fun unknownValuesAreIgnoredSafely() {
        val s = DashboardSettings(language = "KLINGON", selectedCode = 999, filter = "banana")
        assertEquals(Language.ENGLISH, s.languageOrDefault())
        assertNull(s.selectedCodeOrNull())
        assertNull(s.filterOrNull())
    }

    @Test
    fun unknownJsonKeysAreIgnored() = runTest {
        val kv = InMemoryKeyValueStore()
        kv.put(DashboardSettingsStore.KEY, "{\"language\":\"SWAHILI\",\"future\":1}")
        assertEquals(Language.SWAHILI, DashboardSettingsStore(kv).load().languageOrDefault())
    }

    @Test
    fun loadingFilterIsNeverSaved() {
        assertNull(DashboardSettings.filterKey(StatusFilter.Loading))
        assertEquals("failed", DashboardSettings.filterKey(StatusFilter.Failed))
        assertEquals("severe", DashboardSettings.filterKey(StatusFilter.Level(RiskLevel.SEVERE)))
    }

    @Test
    fun stateRestoresAndExportsSettings() {
        val state = DashboardState(CoroutineScope(Dispatchers.Unconfined), loader = { error("unused") }, clock = { 0L })
        state.restore(DashboardSettings(Language.SWAHILI.name, 7, "failed", autoRefresh = false))
        assertEquals(Language.SWAHILI, state.language)
        assertEquals(7, state.selectedCode)
        assertEquals(StatusFilter.Failed, state.filter)
        assertEquals(false, state.autoRefresh)
        assertEquals(DashboardSettings(Language.SWAHILI.name, 7, "failed", false), state.settings)
    }
}
