package ke.tahadhari.ui

/**
 * Texts of the "Counties I watch" list, grouped so the `Strings` constructor stays under the
 * JVM's 255-parameter limit (see `Strings.watch`).
 */
data class WatchStrings(
    val title: String,
    val hint: String,
    val add: String,
    val addTitle: String,
    val addHint: String,
    val cancelAdd: String,
    val limitReached: String,
    val remove: (county: String) -> String,
    val loadingRow: String,
    val failedRow: String,
    val savedRow: String,
    val backToMine: String,
    val notifyNote: String,
    /** Description of a ready row. [saved] is true for a saved (offline) forecast. */
    val rowReady: (county: String, level: String, reason: String, updated: String, saved: Boolean) -> String,
    val rowLoadingDescription: (county: String) -> String,
    val rowFailedDescription: (county: String) -> String,
) {
    companion object {
        val english = WatchStrings(
            title = "Counties I watch",
            hint = "Follow family or friends in up to 3 other counties.",
            add = "Add a county",
            addTitle = "Add a county to watch",
            addHint = "Pick a county to follow. It will appear on your Alerts screen.",
            cancelAdd = "Cancel",
            limitReached = "You are watching 3 counties. Remove one to add another.",
            remove = { n -> "Stop watching $n" },
            loadingRow = "Loading",
            failedRow = "Couldn't load this county. No risk level is shown.",
            savedRow = "Saved forecast",
            backToMine = "Back to my county",
            notifyNote = "Risk alerts are on: you will also be told when a county you watch rises to High or Severe.",
            rowReady = { c, level, reason, updated, saved ->
                "$c. $level. $reason. " + (if (saved) "Saved forecast. " else "") + "$updated. Opens the forecast."
            },
            rowLoadingDescription = { c -> "$c. Loading." },
            rowFailedDescription = { c -> "$c. Couldn't load. No risk level is shown." },
        )

        val swahili = WatchStrings(
            title = "Kaunti ninazofuatilia",
            hint = "Fuatilia familia au marafiki katika kaunti nyingine hadi 3.",
            add = "Ongeza kaunti",
            addTitle = "Ongeza kaunti ya kufuatilia",
            addHint = "Chagua kaunti ya kufuatilia. Itaonekana kwenye skrini yako ya Tahadhari.",
            cancelAdd = "Ghairi",
            limitReached = "Unafuatilia kaunti 3. Ondoa moja ili kuongeza nyingine.",
            remove = { n -> "Acha kufuatilia $n" },
            loadingRow = "Inapakia",
            failedRow = "Imeshindwa kupakia kaunti hii. Kiwango cha hatari hakionyeshwi.",
            savedRow = "Utabiri uliohifadhiwa",
            backToMine = "Rudi kwenye kaunti yangu",
            notifyNote = "Arifa za hatari zimewashwa: pia utaarifiwa hatari ikipanda hadi Kubwa au Kubwa sana katika kaunti unazofuatilia.",
            rowReady = { c, level, reason, updated, saved ->
                "$c. $level. $reason. " + (if (saved) "Utabiri uliohifadhiwa. " else "") + "$updated. Hufungua utabiri."
            },
            rowLoadingDescription = { c -> "$c. Inapakia." },
            rowFailedDescription = { c -> "$c. Imeshindwa kupakia. Kiwango cha hatari hakionyeshwi." },
        )
    }
}
