package ke.tahadhari

import ke.tahadhari.domain.Dates
import ke.tahadhari.domain.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class DatesTest {
    @Test fun knownWeekdays() {
        assertEquals("Tuesday", Dates.dayName("2026-10-27", Language.ENGLISH))
        assertEquals("Jumanne", Dates.dayName("2026-10-27", Language.SWAHILI))
        assertEquals("Thursday", Dates.dayName("2026-01-01", Language.ENGLISH))
        assertEquals("Jumapili", Dates.dayName("2026-11-01", Language.SWAHILI))
        assertEquals("Thursday", Dates.dayName("2024-02-29", Language.ENGLISH))
    }

    @Test fun dayOfMonthLabel() {
        assertEquals("7", Dates.dayOfMonth("2026-11-07"))
        assertEquals("27", Dates.dayOfMonth("2026-10-27"))
    }
}
