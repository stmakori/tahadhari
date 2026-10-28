package ke.tahadhari.domain

import ke.tahadhari.data.County
import kotlin.math.roundToInt

enum class Language { ENGLISH, SWAHILI }

enum class ExplanationSource { TEMPLATE, AI }

data class Explanation(
    val headline: String,
    val body: String,
    val actions: List<String>,
    val source: ExplanationSource,
)

/** Turns a risk assessment into plain-language advice. */
interface AlertExplainer {
    suspend fun explain(county: County, assessment: RiskAssessment, language: Language): Explanation
}

/**
 * Offline, deterministic explainer. Always available, so the app works without
 * internet or an AI key. The AI explainer (week 3) falls back to this on any error.
 * TODO: have a native Swahili speaker review all Swahili text before submission.
 */
class TemplateExplainer : AlertExplainer {
    private companion object {
        const val MAX_ACTIONS = 5
    }

    override suspend fun explain(county: County, assessment: RiskAssessment, language: Language): Explanation =
        build(county, assessment, language)

    fun build(county: County, a: RiskAssessment, language: Language): Explanation {
        val sw = language == Language.SWAHILI
        val mm = a.wettestDay?.precipitationMm?.roundToInt() ?: 0
        val day = a.wettestDay?.let { Dates.dayName(it.date, language) } ?: ""
        val total = a.maxThreeDayTotalMm.roundToInt()
        val riverClause = riverClause(a, sw)
        val name = county.name

        val (headline, body) = when (a.level) {
            RiskLevel.LOW -> if (sw)
                "Hatari ndogo ya mafuriko $name" to
                    "Hakuna mvua kubwa inayotarajiwa katika siku saba zijazo. Endelea kufuatilia utabiri rasmi wa hali ya hewa."
            else
                "Low flood risk in $name" to
                    "No heavy rain is expected over the next 7 days. Keep following official weather forecasts."

            RiskLevel.MODERATE -> if (sw)
                "Mvua ya wastani inatarajiwa $name" to
                    "Mvua ya hadi milimita $mm inatarajiwa siku ya $day$riverClause. Barabara zinaweza kuteleza na maji yanaweza kukusanyika maeneo ya chini."
            else
                "Moderate rain expected in $name" to
                    "Up to $mm mm of rain is expected on $day$riverClause. Roads may become slippery and water may collect in low-lying areas."

            RiskLevel.HIGH -> if (sw)
                "Hatari kubwa ya mafuriko $name" to
                    "Mvua kubwa ya takriban milimita $mm inatarajiwa siku ya $day, na jumla ya milimita $total kwa siku tatu$riverClause. Mafuriko yanawezekana katika maeneo ya chini na karibu na mito."
            else
                "High flood risk in $name" to
                    "Heavy rain of about $mm mm is expected on $day, with $total mm over three days$riverClause. Flooding is possible in low-lying areas and near rivers."

            RiskLevel.SEVERE -> if (sw)
                "Hatari kubwa sana ya mafuriko $name" to
                    "Mvua kubwa sana ya takriban milimita $mm inatarajiwa siku ya $day, na jumla ya milimita $total kwa siku tatu$riverClause. Mafuriko makubwa na maporomoko ya ardhi yanawezekana."
            else
                "Severe flood risk in $name" to
                    "Very heavy rain of about $mm mm is expected on $day, with $total mm over three days$riverClause. Serious flooding and landslides are possible."
        }

        val hotspots = if (county.hotspots.isNotEmpty() && a.level >= RiskLevel.HIGH) {
            val list = county.hotspots.joinToString(", ")
            if (sw) " Maeneo yaliyo hatarini zaidi: $list." else " Areas most at risk: $list."
        } else ""

        val landslide = a.reasons.any { it is RiskReason.LandslideRisk }
        val acts = if (landslide) landslideActions(sw) + actions(a.level, sw) else actions(a.level, sw)
        return Explanation(
            headline,
            body + wetGroundClause(a, sw) + landslideClause(county, landslide, sw) + upstreamClause(a, sw) + hotspots,
            acts.take(MAX_ACTIONS),
            ExplanationSource.TEMPLATE,
        )
    }

    /**
     * Heavy rain upstream, or (when upstream data is missing) an honest "not checked". Never claims upstream is dry.
     * TODO(review): Swahili wording needs a fluent speaker's check.
     */
    private fun upstreamClause(a: RiskAssessment, sw: Boolean): String {
        val up = a.reasons.filterIsInstance<RiskReason.UpstreamRain>().firstOrNull()
        if (up != null) {
            val mm = up.totalMm.roundToInt()
            val river = a.river?.riverName
            return if (sw) " Mvua kubwa (takriban milimita $mm) inanyesha juu ya mto huko ${up.countyName}; inaweza kuinua ${river?.let { "mto $it" } ?: "mto"} ndani ya siku chache."
            else " Heavy rain upstream in ${up.countyName} (about $mm mm) can raise ${river?.let { "the $it" } ?: "the river"} within days."
        }
        if (a.upstreamUnavailable) {
            return if (sw) " Mvua za juu ya mto hazikuweza kukaguliwa."
            else " Rain upstream could not be checked, so it is not included."
        }
        return ""
    }

    private fun wetGroundClause(a: RiskAssessment, sw: Boolean): String {
        if (a.reasons.none { it is RiskReason.SaturatedGround }) return ""
        // TODO(review): Swahili wording needs a fluent speaker's check.
        return if (sw) " Ardhi tayari imeloa sana, kwa hiyo maji ya mvua yatatiririka haraka badala ya kuingia ardhini."
        else " The ground is already very wet, so rain will run off quickly instead of soaking in."
    }

    private fun landslideClause(county: County, landslide: Boolean, sw: Boolean): String {
        if (!landslide) return ""
        val areas = county.landslideAreas.joinToString(", ")
        return if (sw) {
            " Maporomoko ya ardhi yanawezekana kwenye miteremko mikali" + (if (areas.isNotEmpty()) ", hasa $areas" else "") + "."
        } else {
            " Landslides are possible on steep slopes" + (if (areas.isNotEmpty()) ", especially around $areas" else "") + "."
        }
    }

    private fun landslideActions(sw: Boolean): List<String> = if (sw) listOf(
        "Angalia nyufa ardhini au kwenye kuta, miti au nguzo zinazoinama, na chemchemi mpya au maji machafu. Hizi ni dalili za hatari",
        "Ondoka kwenye miteremko mikali na kwenye mabonde yaliyo chini yake, hasa usiku. Usirudi hadi eneo likaguliwe",
    ) else listOf(
        "Watch for cracks in the ground or walls, tilting trees or poles, and new springs or muddy water. These are warning signs",
        "Move away from steep slopes and from valley bottoms below them, especially at night. Don't return until the area has been checked",
    )

    private fun riverClause(a: RiskAssessment, sw: Boolean): String {
        val river = a.river ?: return ""
        if (a.reasons.none { it is RiskReason.RiverRising }) return ""
        val ratio = ((river.riseRatio * 10).roundToInt() / 10.0).toString()
        return if (sw) {
            val name = river.riverName?.let { "mto $it" } ?: "mito iliyo karibu"
            ", na $name unatarajiwa kujaa hadi mara $ratio ya kiwango chake cha hivi karibuni"
        } else {
            val name = river.riverName ?: "nearby rivers"
            ", and the $name is forecast to rise to about $ratio times its recent level"
        }
    }

    private fun actions(level: RiskLevel, sw: Boolean): List<String> = when (level) {
        RiskLevel.LOW -> if (sw) listOf(
            "Safisha mitaro karibu na nyumba yako",
            "Hifadhi nambari za dharura kwenye simu yako",
        ) else listOf(
            "Clear drainage channels around your home",
            "Save emergency numbers on your phone",
        )
        RiskLevel.MODERATE -> if (sw) listOf(
            "Usitembee wala kuendesha gari kwenye maji yanayotiririka",
            "Safisha mitaro karibu na nyumba yako",
            "Hakikisha simu yako ina chaji",
        ) else listOf(
            "Don't walk or drive through flowing water",
            "Clear drainage around your home",
            "Keep your phone charged",
        )
        RiskLevel.HIGH -> if (sw) listOf(
            "Ikiwa unaishi karibu na mto au eneo la chini, panga mahali utakapokwenda maji yakipanda",
            "Weka stakabadhi muhimu na mali yako mahali pa juu na pakavu",
            "Usivuke barabara wala madaraja yaliyofurika",
            "Chemsha au tibu maji ya kunywa",
        ) else listOf(
            "If you live near a river or in a low-lying area, plan where you will go if water rises",
            "Move important documents and valuables to a high, dry place",
            "Don't cross flooded roads or bridges",
            "Boil or treat drinking water",
        )
        RiskLevel.SEVERE -> if (sw) listOf(
            "Ikiwa unaishi karibu na mto, kwenye mteremko mkali au eneo la chini, hamia mahali pa juu mapema. Usisubiri giza",
            "Usitembee, kuogelea wala kuendesha gari kwenye maji ya mafuriko",
            "Waweke watoto na mifugo mbali na mito na mitaro",
            "Wakati wa dharura piga 999 au 112, au 1199 kwa Shirika la Msalaba Mwekundu Kenya",
        ) else listOf(
            "If you live near a river, on a steep slope or in a low-lying area, move to higher ground early. Don't wait until dark",
            "Never walk, swim or drive through flood water",
            "Keep children and livestock away from rivers and drainage channels",
            "In an emergency call 999 or 112, or 1199 for the Kenya Red Cross",
        )
    }
}
