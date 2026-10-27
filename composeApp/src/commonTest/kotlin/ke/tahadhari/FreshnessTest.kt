package ke.tahadhari

import ke.tahadhari.domain.DataAge
import ke.tahadhari.domain.Dates
import ke.tahadhari.domain.Freshness
import ke.tahadhari.domain.Language
import ke.tahadhari.ui.Strings
import ke.tahadhari.ui.updatedText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreshnessTest {
    /** 2026-10-27 09:00 in Nairobi (06:00 UTC), a Tuesday. */
    private val now = 1_793_080_800_000L
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    @Test fun epochMillisToNairobiDate() {
        assertEquals("2026-10-27", Dates.isoDateFromEpochMillis(now))
        assertEquals("1970-01-01", Dates.isoDateFromEpochMillis(0L))
        // 22:30 UTC on the 26th is already 01:30 on the 27th in Nairobi.
        assertEquals("2026-10-27", Dates.isoDateFromEpochMillis(now - 7 * hour - 30 * minute))
        assertEquals("2026-10-26", Dates.isoDateFromEpochMillis(now - 9 * hour - 1))
        assertEquals("2024-02-29", Dates.isoDateFromEpochMillis(1_709_200_000_000L)) // 2024-02-29T09:46Z
        assertEquals("2027-01-01", Dates.isoDateFromEpochMillis(now + 66 * day))
    }

    @Test fun shortDates() {
        assertEquals("Tue 27 Oct", Dates.shortDate("2026-10-27", Language.ENGLISH))
        assertEquals("Jumanne, 27 Oktoba", Dates.shortDate("2026-10-27", Language.SWAHILI))
        assertEquals("Sun 1 Nov", Dates.shortDate("2026-11-01", Language.ENGLISH))
    }

    @Test fun ageBuckets() {
        assertEquals(DataAge.JustNow, Freshness.ageOf(now - 59_000L, now))
        assertEquals(DataAge.JustNow, Freshness.ageOf(now + 5 * minute, now)) // clock skew
        assertEquals(DataAge.Minutes(5), Freshness.ageOf(now - 5 * minute - 20_000L, now))
        assertEquals(DataAge.Minutes(59), Freshness.ageOf(now - hour + 1, now))
        assertEquals(DataAge.Hours(1), Freshness.ageOf(now - hour, now))
        assertEquals(DataAge.Hours(23), Freshness.ageOf(now - day + 1, now))
        assertEquals(DataAge.OnDate("2026-10-26"), Freshness.ageOf(now - day, now))
    }

    @Test fun tooOldPolicy() {
        assertFalse(Freshness.isTooOldToShow(now, now))
        assertFalse(Freshness.isTooOldToShow(now - 3 * day, now))
        assertTrue(Freshness.isTooOldToShow(now - 3 * day - 1, now))
        assertTrue(Freshness.isTooOldToShow(now - 10 * day, now))
        assertFalse(Freshness.isTooOldToShow(now + hour, now))
    }

    @Test fun updatedTextEnglish() {
        val s = Strings.english
        assertEquals("Updated just now", s.updatedText(now - 10_000L, now))
        assertEquals("Updated 5 min ago", s.updatedText(now - 5 * minute, now))
        assertEquals("Updated 3 h ago", s.updatedText(now - 3 * hour - 10 * minute, now))
        assertEquals("Updated on Sat 24 Oct", s.updatedText(now - 3 * day, now))
    }

    @Test fun updatedTextSwahili() {
        val s = Strings.swahili
        assertEquals("Imesasishwa sasa hivi", s.updatedText(now, now))
        assertEquals("Imesasishwa dakika 1 iliyopita", s.updatedText(now - minute, now))
        assertEquals("Imesasishwa dakika 5 zilizopita", s.updatedText(now - 5 * minute, now))
        assertEquals("Imesasishwa saa 3 zilizopita", s.updatedText(now - 3 * hour, now))
        assertEquals("Imesasishwa Jumamosi, 24 Oktoba", s.updatedText(now - 3 * day, now))
    }

    @Test fun tooOldMessageHasDate() {
        assertTrue("Sat 24 Oct" in Strings.english.savedForecastTooOld("2026-10-24"))
        assertTrue("Jumamosi, 24 Oktoba" in Strings.swahili.savedForecastTooOld("2026-10-24"))
    }
}
