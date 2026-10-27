package ke.tahadhari.ui

import ke.tahadhari.data.guides.EmergencyService
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
    // --- UI redesign: mobile shell, hero card, chart, river card, checklist ---
    val appName: String,
    val navAlerts: String,
    val navSafety: String,
    val navEmergency: String,
    val welcomeTitle: String,
    val welcomeBody: String,
    val countiesHeading: String,
    val noMatches: (query: String) -> String,
    val clearSearch: String,
    val hqLabel: (town: String) -> String,
    val riverDataLabel: String,
    val allCounties: String,
    val changeCounty: String,
    val languageLabel: String,
    val languageName: (Language) -> String,
    val floodRisk: String,
    val riskHeadline: (RiskLevel) -> String,
    val riskDescription: (RiskLevel) -> String,
    val countyLabel: (name: String) -> String,
    val reasonHeavyDay: (mm: Int, day: String) -> String,
    val reasonWetSpell: (mm: Int, startDay: String) -> String,
    val reasonRiver: (river: String, ratio: String) -> String,
    val reasonNone: String,
    val nearbyRiver: String,
    val today: String,
    val dayShort: (fullDayName: String) -> String,
    val chartDay: (day: String, mm: Int, probability: Int?) -> String,
    val chartLegend: (moderateMm: Int, highMm: Int) -> String,
    val chanceLegend: String,
    val riverTitle: String,
    val riverRatioLabel: String,
    val riverPeakLine: (m3s: Int, day: String) -> String,
    val riverBaselineLine: (m3s: Int) -> String,
    val riverGauge: (ratio: String) -> String,
    val errorTitle: String,
    val errorNoRisk: String,
    val checklistHint: String,
    val actionState: (done: Boolean) -> String,
    val safetyTitle: String,
    val safetyBody: String,
    val emergencyTitle: String,
    val emergencyBody: String,
    // --- Week 4: desktop dashboard (map, summary bar, ranked list, CSV export) ---
    val mapTab: String,
    val listTab: String,
    val refresh: String,
    val exportCsv: String,
    val exportSaved: (fileName: String) -> String,
    val exportFailed: String,
    val loadingShort: String,
    val noDataShort: String,
    val savedDataShort: String,
    val notUpdatedYet: String,
    val clearFilter: String,
    val filterCount: (label: String, count: Int) -> String,
    val countiesShown: (shown: Int, total: Int) -> String,
    val rankLabel: (rank: Int) -> String,
    val wettestShort: (mm: Int) -> String,
    val threeDayShort: (mm: Int) -> String,
    val riverShort: (river: String, ratio: String) -> String,
    val mapDescription: String,
    val mapHint: String,
    val mapAttribution: String,
    val selectCountyBody: String,
    val noDataLegend: String,
    // --- Week 5: safety guides, emergency screen, check-in, location ---
    val guidesHeading: String,
    val worksOffline: String,
    val guideSectionCount: (sections: Int) -> String,
    val guideProgress: (done: Int, total: Int) -> String,
    val showSteps: String,
    val hideSteps: String,
    val allGuides: String,
    val sourcesTitle: String,
    val guideOfficialNote: String,
    val emergencyNumbersTitle: String,
    val emergencyCallHint: String,
    val emergencyService: (EmergencyService) -> String,
    val emergencyServiceDetail: (EmergencyService) -> String,
    val callNumber: (number: String, service: String) -> String,
    val numberCopied: (number: String) -> String,
    val emergencyOfficialNote: String,
    val hotspotsTitle: (countyName: String) -> String,
    val hotspotsBody: String,
    val hotspotsNone: String,
    val hotspotsPickCounty: String,
    val chooseCounty: String,
    val checkInTitle: String,
    val checkInBody: String,
    val checkInShort: String,
    val imSafe: String,
    val needHelp: String,
    val shareTitle: String,
    val messageCopied: String,
    /** Message the user shares. [place] is e.g. "Kisumu County" or null; [time] from [checkInTime]. */
    val checkInSafeMessage: (place: String?, time: String) -> String,
    val checkInHelpMessage: (place: String?, time: String) -> String,
    /** [month] is 1–12, [hhmm] is 24-hour Nairobi time such as "14:30". */
    val checkInTime: (day: Int, month: Int, hhmm: String) -> String,
    val useMyLocation: String,
    val locationPrivacy: String,
    val locating: String,
    val confirmCounty: (countyName: String) -> String,
    val confirmCountyNote: String,
    val confirmYes: String,
    val confirmNo: String,
    val locationDenied: String,
    val locationUnavailable: String,
    val locationOutsideKenya: String,
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
            aiLabel = "AI explanation",
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
            appName = "Tahadhari",
            navAlerts = "Alerts",
            navSafety = "Safety",
            navEmergency = "Emergency",
            welcomeTitle = "Know your flood risk",
            welcomeBody = "Tahadhari turns open rainfall and river forecasts into plain flood alerts. " +
                "Choose your county to see the next 7 days and what to do.",
            countiesHeading = "Kenya's 47 counties",
            noMatches = { q -> "No county matches \"$q\"" },
            clearSearch = "Clear search",
            hqLabel = { t -> "HQ: $t" },
            riverDataLabel = "River forecast included",
            allCounties = "All counties",
            changeCounty = "Change county",
            languageLabel = "Language",
            languageName = { if (it == Language.SWAHILI) "Kiswahili" else "English" },
            floodRisk = "Flood risk",
            riskHeadline = { when (it) {
                RiskLevel.LOW -> "Low flood risk"; RiskLevel.MODERATE -> "Moderate flood risk"
                RiskLevel.HIGH -> "High flood risk"; RiskLevel.SEVERE -> "Severe flood risk"
            } },
            riskDescription = { l -> "Flood risk: " + when (l) {
                RiskLevel.LOW -> "Low"; RiskLevel.MODERATE -> "Moderate"
                RiskLevel.HIGH -> "High"; RiskLevel.SEVERE -> "Severe"
            } },
            countyLabel = { n -> "$n County" },
            reasonHeavyDay = { mm, d -> "Wettest day: $mm mm on $d" },
            reasonWetSpell = { mm, d -> "$mm mm over 3 days from $d" },
            reasonRiver = { r, x -> "$r: up to $x× its recent flow" },
            reasonNone = "No heavy rain expected in the next 7 days",
            nearbyRiver = "Nearby river",
            today = "Today",
            dayShort = { it.take(3) },
            chartDay = { d, mm, p -> "$d: $mm mm" + (p?.let { ", $it% chance of rain" } ?: "") },
            chartLegend = { m, h -> "Dashed lines: $m mm (moderate) and $h mm (high) in one day" },
            chanceLegend = "% = chance of rain",
            riverTitle = "River outlook",
            riverRatioLabel = "of its recent flow",
            riverPeakLine = { q, d -> "Forecast peak: $q m³/s on $d" },
            riverBaselineLine = { q -> "Recent average: $q m³/s" },
            riverGauge = { x -> "River forecast to reach $x times its recent flow. 2 times is high, 3 times is severe." },
            errorTitle = "Forecast unavailable",
            errorNoRisk = "No risk level is shown until the forecast loads.",
            checklistHint = "Tap each step when it's done",
            actionState = { if (it) "Done" else "Not done" },
            safetyTitle = "Stay safe",
            safetyBody = "How to prepare for floods, and how to stay safe during and after them.",
            emergencyTitle = "Emergency help",
            emergencyBody = "Who to call and where to get help when water rises.",
            mapTab = "Map",
            listTab = "Ranked list",
            refresh = "Refresh",
            exportCsv = "Export CSV",
            exportSaved = { "Saved $it" },
            exportFailed = "Couldn't save the file. Try another folder.",
            loadingShort = "Loading",
            noDataShort = "No data",
            savedDataShort = "Saved forecast (offline)",
            notUpdatedYet = "Loading forecasts…",
            clearFilter = "Show all",
            filterCount = { label, n -> "$label: $n" },
            countiesShown = { n, t -> "$n of $t counties" },
            rankLabel = { "Rank $it" },
            wettestShort = { "Wettest day $it mm" },
            threeDayShort = { "3 days $it mm" },
            riverShort = { r, x -> "$r ×$x" },
            mapDescription = "Map of Kenya's 47 counties coloured by flood risk",
            mapHint = "Hover for details, click to select",
            mapAttribution = "County boundaries: geoBoundaries (RCMRD), public domain. " +
                "Forecasts: Open-Meteo.com (CC BY 4.0).",
            selectCountyBody = "Click a county on the map or in the list to see its 7-day outlook, " +
                "the reasons for its level and what to do.",
            noDataLegend = "No data (not Low)",
            guidesHeading = "Safety guides",
            worksOffline = "Works without internet",
            guideSectionCount = { n -> if (n == 1) "1 part" else "$n parts" },
            guideProgress = { d, t -> "$d of $t steps done" },
            showSteps = "Show steps",
            hideSteps = "Hide steps",
            allGuides = "All guides",
            sourcesTitle = "Sources",
            guideOfficialNote = "This is general advice. Always follow instructions from county officials, " +
                "Kenya Red Cross and the Kenya Meteorological Department.",
            emergencyNumbersTitle = "Emergency numbers",
            emergencyCallHint = "Tap a number to open your phone's dialer.",
            emergencyService = { when (it) {
                EmergencyService.POLICE_FIRE_AMBULANCE -> "Police, fire, ambulance"
                EmergencyService.EMERGENCY_112 -> "Emergency line"
                EmergencyService.RED_CROSS -> "Kenya Red Cross"
            } },
            emergencyServiceDetail = { when (it) {
                EmergencyService.POLICE_FIRE_AMBULANCE -> "National emergency number"
                EmergencyService.EMERGENCY_112 -> "Use it if 999 does not go through"
                EmergencyService.RED_CROSS -> "Toll-free"
            } },
            callNumber = { n, s -> "Call $s, $n" },
            numberCopied = { n -> "$n copied. Dial it from your phone." },
            emergencyOfficialNote = "If you are in danger, call 999 or 112 now. Follow instructions from police, " +
                "county officials and Kenya Red Cross.",
            hotspotsTitle = { n -> "Flood-prone areas · $n County" },
            hotspotsBody = "Stay away from these places when heavy rain is forecast.",
            hotspotsNone = "No flood-prone areas are listed for this county yet. Follow county advisories.",
            hotspotsPickCounty = "Choose your county to see local flood-prone areas.",
            chooseCounty = "Choose county",
            checkInTitle = "Tell family you're OK",
            checkInBody = "Send a short message with your county and the time. You choose who gets it.",
            checkInShort = "Let family know",
            imSafe = "I'm safe",
            needHelp = "I need help",
            shareTitle = "Send message",
            messageCopied = "Message copied. Paste it into SMS or WhatsApp.",
            checkInSafeMessage = { p, t -> "I'm safe. " + (p?.let { "$it, " } ?: "") + "$t. — via Tahadhari" },
            checkInHelpMessage = { p, t ->
                "I need help. " + (p?.let { "$it, " } ?: "") + "$t. Please call me. — via Tahadhari"
            },
            checkInTime = { d, m, hhmm ->
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                "$d ${months[m - 1]} $hhmm"
            },
            useMyLocation = "Use my location",
            locationPrivacy = "Your location stays on this phone.",
            locating = "Finding your location…",
            confirmCounty = { n -> "Is this your county? $n County" },
            confirmCountyNote = "Based on the nearest county town, so it can be wrong near a border.",
            confirmYes = "Yes, show it",
            confirmNo = "No, I'll choose",
            locationDenied = "Location permission was not given. You can choose your county from the list.",
            locationUnavailable = "Couldn't find your location. Turn on location, or choose your county from the list.",
            locationOutsideKenya = "You seem to be outside Kenya. Choose a county from the list.",
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
            aiLabel = "Maelezo ya AI",
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
            // TODO(review): all Swahili below needs a fluent speaker's check before submission.
            appName = "Tahadhari",
            navAlerts = "Tahadhari",
            navSafety = "Usalama",
            navEmergency = "Dharura",
            welcomeTitle = "Jua hatari ya mafuriko kwako",
            welcomeBody = "Tahadhari hugeuza utabiri wa mvua na mito kuwa tahadhari rahisi za mafuriko. " +
                "Chagua kaunti yako uone siku 7 zijazo na unachopaswa kufanya.",
            countiesHeading = "Kaunti 47 za Kenya",
            noMatches = { q -> "Hakuna kaunti inayolingana na \"$q\"" },
            clearSearch = "Futa utafutaji",
            hqLabel = { t -> "Makao makuu: $t" },
            riverDataLabel = "Utabiri wa mto umejumuishwa",
            allCounties = "Kaunti zote",
            changeCounty = "Badilisha kaunti",
            languageLabel = "Lugha",
            languageName = { if (it == Language.SWAHILI) "Kiswahili" else "Kiingereza" },
            floodRisk = "Hatari ya mafuriko",
            riskHeadline = { when (it) {
                RiskLevel.LOW -> "Hatari ndogo ya mafuriko"; RiskLevel.MODERATE -> "Hatari ya wastani ya mafuriko"
                RiskLevel.HIGH -> "Hatari kubwa ya mafuriko"; RiskLevel.SEVERE -> "Hatari kubwa sana ya mafuriko"
            } },
            riskDescription = { l -> "Hatari ya mafuriko: " + when (l) {
                RiskLevel.LOW -> "Ndogo"; RiskLevel.MODERATE -> "Wastani"
                RiskLevel.HIGH -> "Kubwa"; RiskLevel.SEVERE -> "Kubwa sana"
            } },
            countyLabel = { n -> "Kaunti ya $n" },
            reasonHeavyDay = { mm, d -> "Siku yenye mvua nyingi: milimita $mm, $d" },
            reasonWetSpell = { mm, d -> "Milimita $mm kwa siku 3 kuanzia $d" },
            reasonRiver = { r, x -> "$r: hadi mara $x ya mtiririko wa hivi karibuni" },
            reasonNone = "Hakuna mvua kubwa inayotarajiwa siku 7 zijazo",
            nearbyRiver = "Mto ulio karibu",
            today = "Leo",
            // Full names don't fit a 7-column phone chart. TODO(review): confirm these short forms.
            dayShort = { when (it) {
                "Jumatatu" -> "Jtt"; "Jumanne" -> "Jnn"; "Jumatano" -> "Jtn"; "Alhamisi" -> "Alh"
                "Ijumaa" -> "Iju"; "Jumamosi" -> "Jms"; "Jumapili" -> "Jpl"; else -> it.take(3)
            } },
            chartDay = { d, mm, p -> "$d: milimita $mm" + (p?.let { ", uwezekano wa mvua $it%" } ?: "") },
            chartLegend = { m, h -> "Mistari ya vistari: milimita $m (wastani) na $h (kubwa) kwa siku moja" },
            chanceLegend = "% = uwezekano wa mvua",
            riverTitle = "Hali ya mto",
            riverRatioLabel = "ya mtiririko wa hivi karibuni",
            riverPeakLine = { q, d -> "Kilele kinachotarajiwa: m³/s $q, $d" },
            riverBaselineLine = { q -> "Wastani wa hivi karibuni: m³/s $q" },
            riverGauge = { x ->
                "Mto unatarajiwa kufikia mara $x ya mtiririko wake wa hivi karibuni. " +
                    "Mara 2 ni hatari kubwa, mara 3 ni hatari kubwa sana."
            },
            errorTitle = "Utabiri haupatikani",
            errorNoRisk = "Kiwango cha hatari hakitaonyeshwa hadi utabiri upakie.",
            checklistHint = "Gusa kila hatua ukiikamilisha",
            actionState = { if (it) "Imekamilika" else "Haijakamilika" },
            safetyTitle = "Kaa salama",
            safetyBody = "Jinsi ya kujiandaa kwa mafuriko, na jinsi ya kukaa salama wakati wa mafuriko na baada yake.",
            emergencyTitle = "Msaada wa dharura",
            emergencyBody = "Nani wa kupigia simu na wapi kupata msaada maji yakipanda.",
            mapTab = "Ramani",
            listTab = "Orodha kwa hatari",
            refresh = "Sasisha",
            exportCsv = "Hamisha CSV",
            exportSaved = { "Imehifadhiwa: $it" },
            exportFailed = "Imeshindwa kuhifadhi faili. Jaribu folda nyingine.",
            loadingShort = "Inapakia",
            noDataShort = "Hakuna data",
            savedDataShort = "Utabiri uliohifadhiwa (nje ya mtandao)",
            notUpdatedYet = "Inapakia utabiri…",
            clearFilter = "Onyesha zote",
            filterCount = { label, n -> "$label: $n" },
            countiesShown = { n, t -> "Kaunti $n kati ya $t" },
            rankLabel = { "Nafasi ya $it" },
            wettestShort = { "Siku ya mvua nyingi: mm $it" },
            threeDayShort = { "Siku 3: mm $it" },
            riverShort = { r, x -> "$r mara $x" },
            mapDescription = "Ramani ya kaunti 47 za Kenya zikiwa na rangi kulingana na hatari ya mafuriko",
            mapHint = "Elekeza kipanya kuona maelezo, bofya kuchagua",
            mapAttribution = "Mipaka ya kaunti: geoBoundaries (RCMRD), kikoa cha umma. " +
                "Utabiri: Open-Meteo.com (CC BY 4.0).",
            selectCountyBody = "Bofya kaunti kwenye ramani au orodha uone utabiri wa siku 7, " +
                "sababu za kiwango chake na unachopaswa kufanya.",
            noDataLegend = "Hakuna data (si Ndogo)",
            // TODO(review): Week 5 Swahili below needs a fluent speaker's check, especially the
            // emergency, check-in and location lines.
            guidesHeading = "Miongozo ya usalama",
            worksOffline = "Inafanya kazi bila mtandao",
            guideSectionCount = { n -> if (n == 1) "Sehemu 1" else "Sehemu $n" },
            guideProgress = { d, t -> "Hatua $d kati ya $t zimekamilika" },
            showSteps = "Onyesha hatua",
            hideSteps = "Ficha hatua",
            allGuides = "Miongozo yote",
            sourcesTitle = "Vyanzo",
            guideOfficialNote = "Huu ni ushauri wa jumla. Fuata daima maagizo ya maafisa wa kaunti, " +
                "Msalaba Mwekundu wa Kenya na Idara ya Hali ya Hewa Kenya.",
            emergencyNumbersTitle = "Nambari za dharura",
            emergencyCallHint = "Gusa nambari ili kuifungua kwenye simu yako.",
            emergencyService = { when (it) {
                EmergencyService.POLICE_FIRE_AMBULANCE -> "Polisi, zimamoto, ambulensi"
                EmergencyService.EMERGENCY_112 -> "Nambari ya dharura"
                EmergencyService.RED_CROSS -> "Msalaba Mwekundu wa Kenya"
            } },
            emergencyServiceDetail = { when (it) {
                EmergencyService.POLICE_FIRE_AMBULANCE -> "Nambari ya kitaifa ya dharura"
                EmergencyService.EMERGENCY_112 -> "Itumie 999 isipopatikana"
                EmergencyService.RED_CROSS -> "Bila malipo"
            } },
            callNumber = { n, s -> "Piga simu $s, $n" },
            numberCopied = { n -> "$n imenakiliwa. Ipige kutoka kwenye simu yako." },
            emergencyOfficialNote = "Ukiwa hatarini, piga 999 au 112 sasa. Fuata maagizo ya polisi, " +
                "maafisa wa kaunti na Msalaba Mwekundu wa Kenya.",
            hotspotsTitle = { n -> "Maeneo hatari ya mafuriko · Kaunti ya $n" },
            hotspotsBody = "Kaa mbali na maeneo haya mvua kubwa ikitabiriwa.",
            hotspotsNone = "Bado hakuna maeneo hatari ya mafuriko yaliyoorodheshwa kwa kaunti hii. Fuata ushauri wa kaunti.",
            hotspotsPickCounty = "Chagua kaunti yako uone maeneo hatari ya mafuriko karibu nawe.",
            chooseCounty = "Chagua kaunti",
            checkInTitle = "Waarifu familia kuwa uko salama",
            checkInBody = "Tuma ujumbe mfupi wenye kaunti yako na saa. Unachagua nani aupokee.",
            checkInShort = "Waarifu familia",
            imSafe = "Niko salama",
            needHelp = "Nahitaji msaada",
            shareTitle = "Tuma ujumbe",
            messageCopied = "Ujumbe umenakiliwa. Ubandike kwenye SMS au WhatsApp.",
            checkInSafeMessage = { p, t -> "Niko salama. " + (p?.let { "$it, " } ?: "") + "$t. — kupitia Tahadhari" },
            checkInHelpMessage = { p, t ->
                "Nahitaji msaada. " + (p?.let { "$it, " } ?: "") + "$t. Tafadhali nipigie simu. — kupitia Tahadhari"
            },
            checkInTime = { d, m, hhmm ->
                val months = listOf(
                    "Januari", "Februari", "Machi", "Aprili", "Mei", "Juni",
                    "Julai", "Agosti", "Septemba", "Oktoba", "Novemba", "Desemba",
                )
                "$d ${months[m - 1]} $hhmm"
            },
            useMyLocation = "Tumia mahali nilipo",
            locationPrivacy = "Mahali ulipo panabaki kwenye simu hii.",
            locating = "Inatafuta mahali ulipo…",
            confirmCounty = { n -> "Je, hii ndiyo kaunti yako? Kaunti ya $n" },
            confirmCountyNote = "Imetokana na mji wa kaunti ulio karibu zaidi, kwa hivyo huenda isiwe sahihi karibu na mpaka.",
            confirmYes = "Ndiyo, ionyeshe",
            confirmNo = "Hapana, nitachagua",
            locationDenied = "Ruhusa ya mahali haikutolewa. Unaweza kuchagua kaunti yako kwenye orodha.",
            locationUnavailable = "Imeshindwa kupata mahali ulipo. Washa huduma ya mahali, au chagua kaunti yako kwenye orodha.",
            locationOutsideKenya = "Inaonekana uko nje ya Kenya. Chagua kaunti kwenye orodha.",
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
