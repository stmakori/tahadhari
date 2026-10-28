package ke.tahadhari

import ke.tahadhari.data.Preferences
import ke.tahadhari.data.UserPreferences
import ke.tahadhari.data.store.InMemoryKeyValueStore
import ke.tahadhari.domain.Language
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingPreferencesTest {
    @Test fun introIsNotDoneByDefault() = runTest {
        assertFalse(Preferences(InMemoryKeyValueStore()).load().onboardingDone)
        assertFalse(UserPreferences().onboardingDone)
    }

    @Test fun introDoneIsSavedAndKeepsOtherChoices() = runTest {
        val prefs = Preferences(InMemoryKeyValueStore())
        prefs.setCounty(33)
        prefs.setLanguage(Language.SWAHILI)
        prefs.setOnboardingDone(true)
        assertEquals(UserPreferences(33, Language.SWAHILI, onboardingDone = true), prefs.load())
        prefs.setCounty(4) // later changes must not reset the flag
        assertTrue(prefs.load().onboardingDone)
    }

    /** Preferences saved by an older version have no onboardingDone field. */
    @Test fun oldSavedPreferencesStillLoad() = runTest {
        val store = InMemoryKeyValueStore()
        store.put("preferences", """{"countyCode":33,"language":"SWAHILI"}""")
        assertEquals(UserPreferences(33, Language.SWAHILI, onboardingDone = false), Preferences(store).load())
    }

    @Test fun corruptValueMeansIntroNotDone() = runTest {
        val store = InMemoryKeyValueStore()
        store.put("preferences", """{"countyCode":33,"onboardingDone":"maybe"}""")
        assertFalse(Preferences(store).load().onboardingDone)
    }
}
