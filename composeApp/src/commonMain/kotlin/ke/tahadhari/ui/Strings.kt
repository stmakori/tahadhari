package ke.tahadhari.ui

import ke.tahadhari.domain.DataAge
import ke.tahadhari.domain.Dates
import ke.tahadhari.domain.Freshness
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskLevel

/** All UI text in both languages. Keep every user-facing string here. */
data class Strings(
    val tagline: String,
    val searchHint: String,
    val back: String,
    val rainNext7: String,
    val whatToDo: String,
    val loading: String,
    val failed: String,
    val retry: String,
    val disclaimer: String,
    val dashboardTitle: String,
    val selectCounty: String,
    val preparing: String,
    val aiLabel: String,
    val offlineLabel: String,
    val riverPeak: (river: String, m3s: Int, day: String, ratio: String) -> String,
    val loaded: (done: Int, total: Int) -> String,
    val risk: (RiskLevel) -> String,
    // --- Week 2: offline cache / freshness ---
    val updatedJustNow: String,
    val updatedMinutesAgo: (minutes: Int) -> String,
    val updatedHoursAgo: (hours: Int) -> String,
    /** [isoDate] is yyyy-MM-dd; the lambda formats it for its language. */
    val updatedOn: (isoDate: String) -> String,
    /** Shown with LoadState.Ready when fromCache is true. */
    val showingSavedForecast: String,
    /** Shown for LoadState.Failed when staleSinceMillis is set. [isoDate] is yyyy-MM-dd. */
    val savedForecastTooOld: (isoDate: String) -> String,
) {
    companion object {
        val english = Strings(
            tagline = "El Niño flood alerts for Kenya",
            searchHint = "Search county",
            back = "← All counties",
            rainNext7 = "Rainfall, next 7 days (mm)",
            whatToDo = "What to do",
            loading = "Loading forecast…",
            failed = "Couldn't load the forecast. Check your connection.",
            retry = "Try again",
            disclaimer = "Guidance based on Open-Meteo forecast data. This is not an official warning. " +
                "Always follow Kenya Meteorological Service and county government advisories.",
            dashboardTitle = "Kenya flood risk dashboard",
            selectCounty = "Select a county to see details",
            preparing = "Preparing explanation…",
            aiLabel = "✨ AI explanation",
            offlineLabel = "Offline explanation",
            riverPeak = { r, q, d, x -> "$r: forecast peak $q m³/s on $d ($x× recent level)" },
            loaded = { d, t -> "$d of $t counties loaded" },
            risk = { when (it) {
                RiskLevel.LOW -> "Low"; RiskLevel.MODERATE -> "Moderate"
                RiskLevel.HIGH -> "High"; RiskLevel.SEVERE -> "Severe"
            } },
            updatedJustNow = "Updated just now",
            updatedMinutesAgo = { "Updated $it min ago" },
            updatedHoursAgo = { "Updated $it h ago" },
            updatedOn = { "Updated on ${Dates.shortDate(it, Language.ENGLISH)}" },
            showingSavedForecast = "No connection. Showing the last saved forecast.",
            savedForecastTooOld = {
                "Couldn't load the forecast. The saved one from ${Dates.shortDate(it, Language.ENGLISH)} is too old to use."
            },
        )

        val swahili = Strings(
            tagline = "Tahadhari za mafuriko ya El Niño nchini Kenya",
            searchHint = "Tafuta kaunti",
            back = "← Kaunti zote",
            rainNext7 = "Mvua, siku 7 zijazo (mm)",
            whatToDo = "Unachopaswa kufanya",
            loading = "Inapakia utabiri…",
            failed = "Imeshindwa kupakia utabiri. Angalia muunganisho wako.",
            retry = "Jaribu tena",
            disclaimer = "Mwongozo huu unatokana na data ya utabiri ya Open-Meteo. Si tahadhari rasmi. " +
                "Fuata daima ushauri wa Idara ya Hali ya Hewa Kenya na serikali ya kaunti.",
            dashboardTitle = "Dashibodi ya hatari ya mafuriko Kenya",
            selectCounty = "Chagua kaunti kuona maelezo",
            preparing = "Inaandaa maelezo…",
            aiLabel = "✨ Maelezo ya AI",
            offlineLabel = "Maelezo ya nje ya mtandao",
            riverPeak = { r, q, d, x -> "$r: kilele cha m³/s $q kinatarajiwa $d (mara $x ya kiwango cha hivi karibuni)" },
            loaded = { d, t -> "Kaunti $d kati ya $t zimepakiwa" },
            risk = { when (it) {
                RiskLevel.LOW -> "Ndogo"; RiskLevel.MODERATE -> "Wastani"
                RiskLevel.HIGH -> "Kubwa"; RiskLevel.SEVERE -> "Kubwa sana"
            } },
            updatedJustNow = "Imesasishwa sasa hivi",
            updatedMinutesAgo = { if (it == 1) "Imesasishwa dakika 1 iliyopita" else "Imesasishwa dakika $it zilizopita" },
            updatedHoursAgo = { if (it == 1) "Imesasishwa saa 1 iliyopita" else "Imesasishwa saa $it zilizopita" },
            updatedOn = { "Imesasishwa ${Dates.shortDate(it, Language.SWAHILI)}" },
            showingSavedForecast = "Hakuna mtandao. Tunaonyesha utabiri wa mwisho uliohifadhiwa.",
            savedForecastTooOld = {
                "Imeshindwa kupakia utabiri. Utabiri uliohifadhiwa wa ${Dates.shortDate(it, Language.SWAHILI)} " +
                    "ni wa zamani mno kutumika."
            },
        )

        fun of(language: Language) = if (language == Language.SWAHILI) swahili else english
    }
}

/** "Updated 5 min ago" / "Updated 3 h ago" / "Updated on Tue 27 Oct" (Nairobi date). Pure: pass the clock in. */
fun Strings.updatedText(updatedAtMillis: Long, nowMillis: Long): String =
    when (val age = Freshness.ageOf(updatedAtMillis, nowMillis)) {
        DataAge.JustNow -> updatedJustNow
        is DataAge.Minutes -> updatedMinutesAgo(age.minutes)
        is DataAge.Hours -> updatedHoursAgo(age.hours)
        is DataAge.OnDate -> updatedOn(age.isoDate)
    }
