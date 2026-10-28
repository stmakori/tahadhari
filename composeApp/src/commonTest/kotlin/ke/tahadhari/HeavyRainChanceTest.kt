package ke.tahadhari

import ke.tahadhari.data.remote.EnsembleResponseParser
import ke.tahadhari.domain.HeavyRainChance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class HeavyRainChanceTest {
    // Four fake members over 3 days. Wettest days: 55, 20, 49.9, 10.
    private val members = listOf(
        listOf(0.0, 55.0, 3.0),
        listOf(20.0, 1.0, 0.0),
        listOf(49.9, 0.0, 0.0),
        listOf(10.0, 2.0, null),
    )

    @Test fun countsMembersAtOrAboveEachThreshold() {
        val c = HeavyRainChance.fromMembers(members, moderateMm = 20.0, highMm = 50.0)!!
        assertEquals(4, c.members)
        assertEquals(1, c.overHigh) // only 55
        assertEquals(3, c.overModerate) // 55, 20 (boundary inclusive), 49.9
        assertEquals(25, c.highPercent)
        assertEquals(75, c.moderatePercent)
        assertEquals(50.0, c.highMm)
    }

    @Test fun membersWithNoDataAreNotCounted() {
        val c = HeavyRainChance.fromMembers(members + listOf(listOf(null, null, null), emptyList()), 20.0, 50.0)!!
        assertEquals(4, c.members)
    }

    @Test fun noUsableMembersIsNullNotZeroPercent() {
        assertNull(HeavyRainChance.fromMembers(emptyList(), 20.0, 50.0))
        assertNull(HeavyRainChance.fromMembers(listOf(listOf(null, null)), 20.0, 50.0))
    }

    @Test fun percentIsRounded() {
        val three = listOf(listOf(60.0), listOf(0.0), listOf(0.0))
        assertEquals(33, HeavyRainChance.fromMembers(three, 20.0, 50.0)!!.highPercent)
    }

    @Test fun parsesControlAndNumberedMembers() {
        val json = """{"daily":{"time":["2026-10-28","2026-10-29"],
            "precipitation_sum":[1.0,2.0],
            "precipitation_sum_member01":[3.0,null],
            "precipitation_sum_member02":[60.0,0.5],
            "other":[9,9]}}"""
        val parsed = EnsembleResponseParser.members(json)
        assertEquals(3, parsed.size)
        assertEquals(listOf(60.0, 0.5), parsed.last())
    }

    @Test fun errorResponseThrows() {
        assertFailsWith<Exception> { EnsembleResponseParser.members("""{"error":true,"reason":"x"}""") }
    }
}
