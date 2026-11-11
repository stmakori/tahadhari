package ke.tahadhari

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end smoke test of the phone app on an emulator: intro, county search, county screen
 * (risk card or error, since the network may be down on CI), Safety guide, Emergency numbers, Back.
 *
 * It does not depend on leftover state: if a previous run saved the intro as done or saved a
 * county, the test adapts (skips the intro, goes Back to the county picker first).
 */
class SmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun exists(tag: String) =
        rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun pressBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun introCountySafetyEmergencyAndBack() {
        // 1. First-launch intro: tap Skip (absent when preferences were saved by an earlier run).
        rule.waitUntil(15_000) { exists("onboarding_skip") || exists("county_search") || exists("tab_alerts") }
        if (exists("onboarding_skip")) rule.onNodeWithTag("onboarding_skip").performClick()

        // A saved county from an earlier run opens straight to the county screen: Back returns to the picker.
        rule.waitUntil(15_000) { exists("county_search") || exists("tab_alerts") }
        if (!exists("county_search")) pressBack()
        rule.waitUntil(10_000) { exists("county_search") }

        // 2. Search for Tana River and open it.
        rule.onNodeWithTag("county_search").performTextInput("Tana")
        rule.waitUntil(10_000) { exists("county_row_4") }
        rule.onNodeWithTag("county_row_4").performClick()

        // 3. The county screen settles on a risk card or on the error state (CI may be offline).
        rule.waitUntil(60_000) { exists("risk_card") || exists("error_state") }
        if (exists("error_state")) {
            // Fail safe: never "Low" next to a failed load.
            check(rule.onAllNodesWithText("Low", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
                "\"Low\" is shown together with the error state"
            }
            check(!exists("risk_card")) { "risk card and error state are shown together" }
        }

        // 4. Safety tab: open a guide, then Back returns to the guide list.
        rule.onNodeWithTag("tab_safety").performClick()
        rule.waitUntil(10_000) { exists("guide_row_floods") }
        rule.onNodeWithTag("guide_row_floods").performClick()
        rule.waitUntil(10_000) { exists("guide_detail") }
        pressBack()
        rule.waitUntil(10_000) { exists("guide_row_floods") && !exists("guide_detail") }

        // 5. Emergency tab: the national numbers are shown.
        rule.onNodeWithTag("tab_emergency").performClick()
        rule.waitUntil(10_000) { exists("emergency_screen") }
        rule.onNodeWithText("999", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("1199", useUnmergedTree = true).assertExists()

        // 6. Back: Emergency -> Alerts (county screen, not the picker) -> county picker.
        pressBack()
        rule.waitUntil(10_000) { !exists("emergency_screen") }
        check(!exists("county_search")) { "Back from Emergency should return to the county screen, not the picker" }
        check(exists("risk_card") || exists("error_state") || exists("tab_alerts")) { "county screen expected after Back" }
        pressBack()
        rule.waitUntil(10_000) { exists("county_search") }
    }
}
