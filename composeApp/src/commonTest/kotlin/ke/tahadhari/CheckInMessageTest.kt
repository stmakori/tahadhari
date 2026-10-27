package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.platform.LocationResult
import ke.tahadhari.ui.Strings
import ke.tahadhari.ui.mobile.CheckInKind
import ke.tahadhari.ui.mobile.LocationUi
import ke.tahadhari.ui.mobile.checkInMessage
import ke.tahadhari.ui.mobile.checkInTimeText
import ke.tahadhari.ui.mobile.locationUiFor
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckInMessageTest {
    private val at1430Nairobi = 1_793_100_600_000L // 2026-10-27T11:30Z = 14:30 EAT
    private val at0005NextDay = 1_793_135_100_000L // 2026-10-27T21:05Z = 00:05 EAT on 28 Oct

    @Test
    fun swahiliSafeMessage() {
        assertEquals(
            "Niko salama. Kaunti ya Kisumu, 27 Oktoba 14:30. — kupitia Tahadhari",
            checkInMessage(CheckInKind.SAFE, "Kisumu", at1430Nairobi, Strings.swahili),
        )
    }

    @Test
    fun englishSafeMessage() {
        assertEquals(
            "I'm safe. Kisumu County, 27 Oct 14:30. — via Tahadhari",
            checkInMessage(CheckInKind.SAFE, "Kisumu", at1430Nairobi, Strings.english),
        )
    }

    @Test
    fun helpMessageAsksForACall() {
        assertEquals(
            "I need help. Tana River County, 27 Oct 14:30. Please call me. — via Tahadhari",
            checkInMessage(CheckInKind.NEED_HELP, "Tana River", at1430Nairobi, Strings.english),
        )
        assertEquals(
            "Nahitaji msaada. Kaunti ya Tana River, 27 Oktoba 14:30. Tafadhali nipigie simu. — kupitia Tahadhari",
            checkInMessage(CheckInKind.NEED_HELP, "Tana River", at1430Nairobi, Strings.swahili),
        )
    }

    @Test
    fun noCountyLeavesThePlaceOut() {
        assertEquals("I'm safe. 27 Oct 14:30. — via Tahadhari", checkInMessage(CheckInKind.SAFE, null, at1430Nairobi, Strings.english))
    }

    @Test
    fun timeUsesNairobiDateAcrossMidnight() {
        assertEquals("28 Oct 00:05", checkInTimeText(at0005NextDay, Strings.english))
        assertEquals("28 Oktoba 00:05", checkInTimeText(at0005NextDay, Strings.swahili))
    }

    @Test
    fun locationResultsMapToClearMessages() {
        val s = Strings.english
        assertEquals(LocationUi.Suggest(Counties.byCode(42)!!), locationUiFor(LocationResult.Found(-0.10, 34.76), s))
        assertEquals(LocationUi.Message(s.locationOutsideKenya), locationUiFor(LocationResult.Found(-6.79, 39.21), s))
        assertEquals(LocationUi.Message(s.locationDenied), locationUiFor(LocationResult.PermissionDenied, s))
        assertEquals(LocationUi.Message(s.locationUnavailable), locationUiFor(LocationResult.Unavailable, s))
    }
}
