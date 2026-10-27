package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.data.GeoPoint
import ke.tahadhari.domain.RiverOutlook
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.dashboard.CsvExport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvExportTest {
    // 2026-10-27T11:05:00Z = 14:05 in Nairobi
    private val now = 1_793_099_100_000L

    private fun lines(csv: String) = csv.split("\r\n").dropLast(1)

    @Test
    fun escapesCommasQuotesAndNewlines() {
        assertEquals("plain", CsvExport.escape("plain"))
        assertEquals("\"a,b\"", CsvExport.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExport.escape("say \"hi\""))
        assertEquals("\"two\nlines\"", CsvExport.escape("two\nlines"))
        assertEquals("\" padded \"", CsvExport.escape(" padded "))
        assertEquals("Murang'a", CsvExport.escape("Murang'a"))
        assertEquals("'=1+1", CsvExport.escape("=1+1"))
        assertEquals("", CsvExport.escape(""))
    }

    @Test
    fun startsWithGuidanceNoteThenHeader() {
        val out = lines(CsvExport.build(emptyList(), emptyMap(), now))
        assertEquals(2, out.size)
        assertTrue(out[0].startsWith("\"# Tahadhari flood risk export, generated 2026-10-27T14:05+03:00."))
        assertTrue("not an official warning" in out[0])
        assertTrue("Open-Meteo.com (CC BY 4.0)" in out[0])
        assertEquals(CsvExport.columns.joinToString(","), out[1])
    }

    @Test
    fun readyRowHasLevelNumbersRiverAndIsoTime() {
        val county = Counties.byCode(4)!! // Tana River
        val state = readyState(county, Rain.high, RiverOutlook("Tana River", 100.0, 245.0, "2026-11-06"), now, fromCache = true)
        val row = lines(CsvExport.build(listOf(county), mapOf(4 to state), now))[2]
        assertEquals(
            "4,Tana River,Hola,high,2026-11-05,55.0,113.0,Tana River,2.45,2026-10-27T14:05+03:00,true,ok",
            row,
        )
    }

    @Test
    fun failedAndLoadingRowsNeverHaveALevel() {
        val a = Counties.byCode(42)!!
        val b = Counties.byCode(47)!!
        val out = lines(CsvExport.build(listOf(a, b), mapOf(42 to LoadState.Failed("offline")), now))
        assertEquals("42,Kisumu,Kisumu,,,,,Nyando River,,,,failed", out[2])
        assertEquals("47,Nairobi,Nairobi,,,,,,,,,loading", out[3])
    }

    @Test
    fun quotesNamesThatNeedIt() {
        val odd = County(99, "Name, with \"quotes\"", "Town", GeoPoint(0.0, 0.0))
        val row = lines(CsvExport.build(listOf(odd), emptyMap(), now))[2]
        assertTrue(row.startsWith("99,\"Name, with \"\"quotes\"\"\",Town,"))
    }

    @Test
    fun decimalsAndFileName() {
        assertEquals("2.35", CsvExport.decimal(2.345, 2))
        assertEquals("0.0", CsvExport.decimal(0.0, 1))
        assertEquals("-1.5", CsvExport.decimal(-1.5, 1))
        assertEquals("120.0", CsvExport.decimal(119.96, 1))
        assertEquals("tahadhari-risk-2026-10-27.csv", CsvExport.fileName(now))
        // 22:30 UTC on the 27th is already the 28th in Nairobi.
        assertEquals("tahadhari-risk-2026-10-28.csv", CsvExport.fileName(1_793_140_200_000L))
    }
}
