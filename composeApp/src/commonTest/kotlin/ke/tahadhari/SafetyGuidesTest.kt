package ke.tahadhari

import ke.tahadhari.data.guides.EmergencyContacts
import ke.tahadhari.data.guides.EmergencyService
import ke.tahadhari.data.guides.SafetyGuides
import ke.tahadhari.domain.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SafetyGuidesTest {
    @Test
    fun coversTheRequiredTopics() {
        assertEquals(listOf("floods", "landslides", "cholera", "livestock", "lightning"), SafetyGuides.all.map { it.id })
    }

    @Test
    fun idsAreUnique() {
        assertEquals(SafetyGuides.all.size, SafetyGuides.all.map { it.id }.toSet().size)
    }

    @Test
    fun everyGuideHasBothLanguagesWithMatchingStructure() {
        SafetyGuides.all.forEach { guide ->
            val en = guide.english
            val sw = guide.swahili
            assertTrue(en.sections.isNotEmpty(), "${guide.id}: no sections")
            assertEquals(en.sections.size, sw.sections.size, "${guide.id}: section count differs between languages")
            en.sections.zip(sw.sections).forEachIndexed { i, (e, s) ->
                assertEquals(e.steps.size, s.steps.size, "${guide.id} section $i: step count differs between languages")
                assertTrue(e.steps.isNotEmpty(), "${guide.id} section $i: no steps")
            }
            listOf(en, sw).forEach { c ->
                assertTrue(c.title.isNotBlank() && c.summary.isNotBlank(), "${guide.id}: blank title or summary")
                c.sections.forEach { sec ->
                    assertTrue(sec.title.isNotBlank(), "${guide.id}: blank section title")
                    sec.steps.forEach { assertTrue(it.isNotBlank(), "${guide.id}: blank step") }
                }
            }
            assertTrue(en.title != sw.title, "${guide.id}: Swahili title looks untranslated")
        }
    }

    @Test
    fun contentPicksTheLanguage() {
        val g = SafetyGuides.floods
        assertEquals(g.english, g.content(Language.ENGLISH))
        assertEquals(g.swahili, g.content(Language.SWAHILI))
    }

    @Test
    fun everyGuideCitesHttpsSources() {
        SafetyGuides.all.forEach { guide ->
            assertTrue(guide.sources.isNotEmpty(), "${guide.id}: no sources")
            guide.sources.forEach { assertTrue(it.url.startsWith("https://"), "${guide.id}: ${it.url}") }
        }
    }

    @Test
    fun keySafetyNumbersAreConsistent() {
        // Kenya Red Cross / US NWS: 15 cm knocks you down, 30 cm sweeps a car away.
        val flash = SafetyGuides.lightning.english.sections.first().steps.joinToString(" ")
        assertTrue("15 cm" in flash && "30 cm" in flash && "60 cm" in flash)
        val flashSw = SafetyGuides.lightning.swahili.sections.first().steps.joinToString(" ")
        assertTrue("Sentimita 15" in flashSw && "Sentimita 30" in flashSw && "Sentimita 60" in flashSw)
    }

    @Test
    fun emergencyNumbersAreTheVerifiedOnes() {
        assertEquals(listOf("999", "112", "1199"), EmergencyContacts.national.map { it.number })
        assertEquals(EmergencyService.RED_CROSS, EmergencyContacts.national.last().service)
        EmergencyContacts.national.forEach { assertTrue(it.sourceUrl.startsWith("https://")) }
    }
}
