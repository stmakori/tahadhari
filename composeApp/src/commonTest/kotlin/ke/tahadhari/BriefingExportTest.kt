package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.data.GeoPoint
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.Trend
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.Strings
import ke.tahadhari.ui.dashboard.BriefingExport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BriefingExportTest {
    // 2026-10-27T11:05:00Z = 14:05 in Nairobi
    private val now = 1_793_099_100_000L
    private val tana = Counties.byCode(4)!!
    private val kisumu = Counties.byCode(42)!!
    private val nairobi = Counties.byCode(47)!!
    private val mombasa = Counties.byCode(1)!!

    private val states: Map<Int, LoadState> = mapOf(
        4 to readyState(tana, Rain.severe, updatedAtMillis = now),
        42 to readyState(kisumu, Rain.high, updatedAtMillis = now, fromCache = true),
        47 to readyState(nairobi, Rain.low, updatedAtMillis = now),
        1 to LoadState.Failed("offline"),
    )
    private val all = listOf(nairobi, mombasa, kisumu, tana)

    private fun build(language: Language = Language.ENGLISH, list: List<County> = all, st: Map<Int, LoadState> = states) =
        BriefingExport.build(list, st, language, now)

    @Test
    fun escapesHtml() {
        assertEquals("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt; &amp; Murang&#39;a", BriefingExport.escapeHtml("<script>alert(\"x\")</script> & Murang'a"))
        val evil = County(99, "<img src=x onerror=alert(1)>", "T<own>", GeoPoint(0.0, 0.0))
        val html = build(list = listOf(evil), st = mapOf(99 to readyState(evil, Rain.high, updatedAtMillis = now)))
        assertFalse("<img" in html)
        assertTrue("&lt;img src=x onerror=alert(1)&gt;" in html)
        assertFalse("<script" in html)
    }

    @Test
    fun isSelfContainedDocument() {
        val html = build()
        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue("<meta charset=\"utf-8\">" in html)
        assertTrue("@media print" in html)
        assertTrue("break-inside:avoid" in html)
        assertFalse("src=" in html || "href=" in html) // no external resources
        assertFalse("<link" in html || "<script" in html)
        assertTrue("2026-10-27 at 14:05" in html)
    }

    @Test
    fun tableIsSortedByRiskWithNoDataAfterData() {
        val html = build()
        val order = listOf("Tana River", "Kisumu", "Nairobi", "Mombasa").map { name ->
            html.indexOf("<td>$name</td>").also { assertTrue(it > 0, name) }
        }
        assertEquals(order.sorted(), order)
    }

    @Test
    fun failedCountyIsNeverLabelledLow() {
        val html = build()
        val row = html.substringAfter("<td>Mombasa</td>").substringBefore("</tr>")
        assertFalse("Low" in row)
        assertTrue("No data" in row)
        assertTrue("nodata" in html.substringBeforeLast("<td>Mombasa</td>").substringAfterLast("<tr "))
        // The summary counts Low only for counties that really are Low.
        assertTrue("<span class=\"n\">1</span> Low" in html)
        assertTrue("<span class=\"n\">1</span> No data" in html)
    }

    @Test
    fun allFailedMeansNoLowAtAll() {
        val failed = all.associate { it.code to (LoadState.Failed("x") as LoadState) }
        val html = build(st = failed)
        assertTrue("<span class=\"n\">0</span> Low" in html)
        assertFalse("badge lvl-low" in html)
        assertTrue("No county has a High or Severe level" in html)
    }

    @Test
    fun highAndSevereGetTemplateExplanationAndActions() {
        val html = build()
        val section = html.substringAfter("<h2>High and Severe")
        assertTrue("Severe flood risk in Tana River" in section)
        assertTrue("High flood risk in Kisumu" in section)
        assertFalse("Low flood risk" in section)
        assertTrue("move to higher ground early" in section)
        // Cached data is labelled with its date.
        assertTrue("Saved forecast (offline)" in section)
    }

    @Test
    fun hasDisclaimerAndAttribution() {
        val s = Strings.english
        val html = build()
        assertTrue(BriefingExport.escapeHtml(s.disclaimer) in html)
        assertTrue("Open-Meteo.com (CC BY 4.0)" in html)
        assertTrue("geoBoundaries (RCMRD), public domain" in html)
    }

    @Test
    fun swahiliBriefingUsesSwahiliTextAndLang() {
        val html = build(Language.SWAHILI)
        assertTrue("<html lang=\"sw\">" in html)
        assertTrue("Muhtasari wa hatari ya mafuriko" in html)
        assertTrue("Hatari kubwa sana ya mafuriko Tana River" in html)
        assertTrue("Hakuna data" in html)
    }

    @Test
    fun fileNameUsesNairobiDate() {
        assertEquals("tahadhari-briefing-2026-10-27.html", BriefingExport.fileName(now))
        assertEquals("tahadhari-briefing-2026-10-28.html", BriefingExport.fileName(1_793_140_200_000L))
    }

    @Test
    fun listsCountiesRisingSinceYesterdayOnlyWhenThereAreAny() {
        assertFalse("Rising since yesterday" in build())
        val trends = mapOf(4 to Trend.Rising(RiskLevel.HIGH), 42 to Trend.Rising(RiskLevel.MODERATE), 47 to Trend.Falling(RiskLevel.HIGH), 1 to Trend.Rising(RiskLevel.LOW))
        val html = BriefingExport.build(all, states, Language.ENGLISH, now, trends)
        // Tana River is first by risk; Mombasa has no data so it is not listed.
        assertTrue("<p class=\"rising\">Rising since yesterday: Tana River, Kisumu</p>" in html)
        val sw = BriefingExport.build(all, states, Language.SWAHILI, now, trends)
        assertTrue("Imepanda tangu jana: Tana River, Kisumu" in sw)
    }
}
