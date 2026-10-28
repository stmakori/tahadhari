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

        val swahili = WatchStrings( // TODO(review): all Swahili below needs a fluent speaker's check.
            title = "Kaunti ninazofuatilia", // TODO(review)
            hint = "Fuatilia familia au marafiki katika kaunti nyingine hadi 3.", // TODO(review)
            add = "Ongeza kaunti", // TODO(review)
            addTitle = "Ongeza kaunti ya kufuatilia", // TODO(review)
            addHint = "Chagua kaunti ya kufuatilia. Itaonekana kwenye skrini yako ya Tahadhari.", // TODO(review)
            cancelAdd = "Ghairi", // TODO(review)
            limitReached = "Unafuatilia kaunti 3. Ondoa moja ili kuongeza nyingine.", // TODO(review)
            remove = { n -> "Acha kufuatilia $n" }, // TODO(review)
            loadingRow = "Inapakia", // TODO(review)
            failedRow = "Imeshindwa kupakia kaunti hii. Kiwango cha hatari hakionyeshwi.", // TODO(review)
            savedRow = "Utabiri uliohifadhiwa", // TODO(review)
            backToMine = "Rudi kwenye kaunti yangu", // TODO(review)
            notifyNote = "Arifa za hatari zimewashwa: pia utaarifiwa hatari ikipanda hadi Kubwa au Kubwa sana katika kaunti unazofuatilia.", // TODO(review)
            rowReady = { c, level, reason, updated, saved ->
                "$c. $level. $reason. " + (if (saved) "Utabiri uliohifadhiwa. " else "") + "$updated. Hufungua utabiri."
            }, // TODO(review)
            rowLoadingDescription = { c -> "$c. Inapakia." }, // TODO(review)
            rowFailedDescription = { c -> "$c. Imeshindwa kupakia. Kiwango cha hatari hakionyeshwi." }, // TODO(review)
        )
    }
}
