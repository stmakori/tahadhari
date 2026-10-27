package ke.tahadhari.ui

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
        )

        fun of(language: Language) = if (language == Language.SWAHILI) swahili else english
    }
}
